# 01. api: delay, faultPercent 파라미터

> 시작: 교수님 `Chapter12` 상태 / 결과: 교수님 `Chapter13`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`GET /product/{productId}`와 `GET /product-composite/{productId}`에 선택 파라미터 `delay`(초), `faultPercent`(%)를 추가한다. 기본값 0.

## 1-1. `api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java`
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
  // ★ 변경 (5줄, 전: 1줄)
  Mono<ProductAggregate> getProduct(
    @PathVariable int productId,
    @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
    @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent
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

## 1-2. `api/src/main/java/se/magnus/api/core/product/ProductService.java`
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
  // ★ 변경 (5줄, 전: 1줄)
  Mono<Product> getProduct(
    @PathVariable int productId,
    @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
    @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent
  );

  Mono<Void> deleteProduct(int productId);
}
```

다음: [02-core-services.md](02-core-services.md)
