# 01. API와 product 서비스: 일부러 느리게 / 실패하게

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 12장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 13장과 다르게 쓰는 부분.
> 교재 "product 서비스에 지연과 무작위 오류 추가" 부분. 회복성(서킷 브레이커 등)을 시험하려면 **느린 응답과 오류를 마음대로 만들 수** 있어야 한다.

## 무엇을 하나
`GET /product/{productId}`에 쿼리 파라미터 두 개를 추가한다(둘 다 선택, 기본 0).

| 파라미터 | 뜻 | 예 |
|---|---|---|
| `delay` | 응답을 몇 **초** 늦출지 | `?delay=3` → 3초 뒤 응답 |
| `faultPercent` | 몇 **%** 확률로 오류(500)를 낼지 | `?faultPercent=25` → 4번 중 1번꼴로 500 |

composite의 `GET /product-composite/{productId}`도 같은 두 파라미터를 받아 product로 그대로 넘긴다([02](02-composite-resilience4j.md)).

## 1. api: ProductService
```bash
code api/src/main/java/se/magnus/api/core/product/ProductService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;  // ★ 추가
import reactor.core.publisher.Mono;

public interface ProductService {

  Mono<Product> createProduct(Product body);

  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Mono<Product> getProduct(  // ★ 변경 (전: getProduct(@PathVariable int productId))
    @PathVariable int productId,
    @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,  // ★ 추가
    @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent  // ★ 추가
  );

  Mono<Void> deleteProduct(int productId);
}
```

## 2. api: ProductCompositeService
```bash
code api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.composite.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@SecurityRequirement(name = "security_auth")
@Tag(name = "ProductComposite", description = "REST API for composite product information.")
public interface ProductCompositeService {

  @Operation(
    summary = "${api.product-composite.create-composite-product.description}",
    description = "${api.product-composite.create-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping(
    value    = "/product-composite",
    consumes = "application/json")
  Mono<Void> createProduct(@RequestBody ProductAggregate body);

  @Operation(
    summary = "${api.product-composite.get-composite-product.description}",
    description = "${api.product-composite.get-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}"),
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @GetMapping(
    value = "/product-composite/{productId}",
    produces = "application/json")
  Mono<ProductAggregate> getProduct(  // ★ 변경 (전: getProduct(@PathVariable int productId))
    @PathVariable int productId,
    @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,  // ★ 추가
    @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent  // ★ 추가
  );

  @Operation(
    summary = "${api.product-composite.delete-composite-product.description}",
    description = "${api.product-composite.delete-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @ResponseStatus(HttpStatus.ACCEPTED)
  @DeleteMapping(value = "/product-composite/{productId}")
  Mono<Void> deleteProduct(@PathVariable int productId);
}
```

`@RequestParam`은 이 파일이 이미 `org.springframework.web.bind.annotation.*`로 import하고 있어서 import 추가가 없다.

## 3. product: ProductServiceImpl
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.services;

import static java.util.logging.Level.FINE;

import java.time.Duration;  // ★ 추가
import java.util.Random;  // ★ 추가
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

  private final ServiceUtil serviceUtil;

  private final ProductRepository repository;

  private final ProductMapper mapper;

  public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Product> createProduct(Product body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }

    ProductEntity entity = mapper.apiToEntity(body);
    Mono<Product> newEntity = repository.save(entity)
      .log(LOG.getName(), FINE)
      .onErrorMap(
        DuplicateKeyException.class,
        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
      .map(e -> mapper.entityToApi(e));

    return newEntity;
  }

  @Override
  public Mono<Product> getProduct(int productId, int delay, int faultPercent) {  // ★ 변경 (delay, faultPercent 추가)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get product info for id={}", productId);

    return repository.findByProductId(productId)
      .map(e -> throwErrorIfBadLuck(e, faultPercent))  // ★ 추가
      .delayElement(Duration.ofSeconds(delay))  // ★ 추가
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
      .log(LOG.getName(), FINE)
      .map(e -> mapper.entityToApi(e))
      .map(e -> setServiceAddress(e));
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    return repository.findByProductId(productId).log(LOG.getName(), FINE).map(e -> repository.delete(e)).flatMap(e -> e);
  }

  private Product setServiceAddress(Product e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
  }

  // ★ 추가 (아래 두 메서드와 필드): faultPercent 확률로 일부러 오류
  private ProductEntity throwErrorIfBadLuck(ProductEntity entity, int faultPercent) {

    if (faultPercent == 0) {
      return entity;
    }

    int randomThreshold = getRandomNumber(1, 100);

    if (faultPercent < randomThreshold) {
      LOG.debug("We got lucky, no error occurred, {} < {}", faultPercent, randomThreshold);
    } else {
      LOG.info("Bad luck, an error occurred, {} >= {}", faultPercent, randomThreshold);
      throw new RuntimeException("Something went wrong...");
    }

    return entity;
  }

  private final Random randomNumberGenerator = new Random();

  private int getRandomNumber(int min, int max) {

    if (max < min) {
      throw new IllegalArgumentException("Max must be greater than min");
    }

    return randomNumberGenerator.nextInt((max - min) + 1) + min;
  }
}
```

- `throwErrorIfBadLuck`: 1~100 사이 난수를 뽑아 `faultPercent` 이상이면(= 그 확률로) `RuntimeException`을 던진다 → product가 500으로 응답.
- `delayElement(Duration.ofSeconds(delay))`: 찾은 결과를 그만큼 늦게 내보낸다. 리액티브라 스레드를 막지 않고 기다린다.

## 확인
이 단계만으로는 composite가 아직 컴파일되지 않는다(`getProduct` 시그니처가 바뀌었으니). 빌드 확인은 [02](02-composite-resilience4j.md) 끝에서 한 번에 한다.

다음: [02-composite-resilience4j.md](02-composite-resilience4j.md)
