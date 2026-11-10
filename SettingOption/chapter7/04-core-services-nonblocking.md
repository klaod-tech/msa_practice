# 04. 핵심 서비스를 논블로킹으로 (API 인터페이스 + 서비스 구현)

> 표기: `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.
> 교재 "핵심 서비스의 논블로킹 REST API" 절, "블로킹 코드 처리" 절.

## 1. 핵심 API 인터페이스 (3개, 전체 교체)
- 반환형이 `Mono`/`Flux`로 바뀐다.
- **create/delete의 `@PostMapping`, `@DeleteMapping`이 없어진다.** 생성/삭제는 이제 HTTP가 아니라 이벤트(05)로 들어온다. 메서드는 남아 있어서 composite(06)와 메시지 처리기(05)가 부른다.

```bash
code api/src/main/java/se/magnus/api/core/product/ProductService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

public interface ProductService {

  Mono<Product> createProduct(Product body);  // ★ 변경 (전: @PostMapping + Product 반환 → 이벤트로 호출, HTTP 매핑 삭제)

  /**
   * Sample usage: "curl $HOST:$PORT/product/1".
   *
   * @param productId Id of the product
   * @return the product, if found, else null
   */
  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Mono<Product> getProduct(@PathVariable int productId);  // ★ 변경 (전: Product)

  Mono<Void> deleteProduct(int productId);  // ★ 변경 (전: @DeleteMapping + void → 이벤트로 호출)
}
```

```bash
code api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {

  Mono<Recommendation> createRecommendation(Recommendation body);  // ★ 변경 (전: @PostMapping + Recommendation 반환)

  /**
   * Sample usage: "curl $HOST:$PORT/recommendation?productId=1".
   *
   * @param productId Id of the product
   * @return the recommendations of the product
   */
  @GetMapping(
    value = "/recommendation",
    produces = "application/json")
  Flux<Recommendation> getRecommendations(  // ★ 변경 (전: List<Recommendation>)
    @RequestParam(value = "productId", required = true) int productId);

  Mono<Void> deleteRecommendations(int productId);  // ★ 변경 (전: @DeleteMapping + void)
}
```

```bash
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {

  Mono<Review> createReview(Review body);  // ★ 변경 (전: @PostMapping + Review 반환)

  /**
   * Sample usage: "curl $HOST:$PORT/review?productId=1".
   *
   * @param productId Id of the product
   * @return the reviews of the product
   */
  @GetMapping(
    value = "/review",
    produces = "application/json")
  Flux<Review> getReviews(  // ★ 변경 (전: List<Review>)
   @RequestParam(value = "productId", required = true) int productId);

  Mono<Void> deleteReviews(int productId);  // ★ 변경 (전: @DeleteMapping + void)
}
```

## 2. product, recommendation 서비스 구현 (전체 교체)
리포지토리가 이미 `Mono`/`Flux`를 주니 **연산자로 이어 붙이기만** 한다.

| 6장 (블로킹) | 7장 (논블로킹) |
|---|---|
| `try { repository.save(e) } catch (DuplicateKeyException)` | `repository.save(e).onErrorMap(DuplicateKeyException.class, ...)` |
| `findByProductId(id).orElseThrow(...)` | `findByProductId(id).switchIfEmpty(Mono.error(new NotFoundException(...)))` |
| 결과에 serviceAddress 넣기 | `.map(e -> mapper.entityToApi(e)).map(e -> setServiceAddress(e))` |
| `repository.deleteAll(list)` | `repository.deleteAll(repository.findByProductId(id))` (Flux를 그대로 넘김) |

`.log(LOG.getName(), FINE)`은 02의 `log()`처럼 신호를 찍는다. 레벨이 FINE(=DEBUG)이라 평소엔 안 보인다.

```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.services;

import static java.util.logging.Level.FINE;

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
  public Mono<Product> createProduct(Product body) {  // ★ 변경: 블로킹 반환 → Mono/Flux

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
  public Mono<Product> getProduct(int productId) {  // ★ 변경: 블로킹 반환 → Mono/Flux

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get product info for id={}", productId);

    return repository.findByProductId(productId)
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
      .log(LOG.getName(), FINE)
      .map(e -> mapper.entityToApi(e))
      .map(e -> setServiceAddress(e));
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {  // ★ 변경: 블로킹 반환 → Mono/Flux

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
}
```

```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.services;

import static java.util.logging.Level.FINE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;
import se.magnus.util.http.ServiceUtil;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final RecommendationRepository repository;

  private final RecommendationMapper mapper;

  private final ServiceUtil serviceUtil;

  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {  // ★ 변경: 블로킹 반환 → Mono/Flux

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }

    RecommendationEntity entity = mapper.apiToEntity(body);
    Mono<Recommendation> newEntity = repository.save(entity)
      .log(LOG.getName(), FINE)
      .onErrorMap(
        DuplicateKeyException.class,
        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
      .map(e -> mapper.entityToApi(e));

    return newEntity;
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {  // ★ 변경: 블로킹 반환 → Mono/Flux

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get recommendations for product with id={}", productId);

    return repository.findByProductId(productId)
      .log(LOG.getName(), FINE)
      .map(e -> mapper.entityToApi(e))
      .map(e -> setServiceAddress(e));
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {  // ★ 변경: 블로킹 반환 → Mono/Flux

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    return repository.deleteAll(repository.findByProductId(productId));
  }

  private Recommendation setServiceAddress(Recommendation e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
  }
}
```

## 3. review: 블로킹 JPA를 별도 스레드 풀에서 (전체 교체)
JPA(MySQL)는 리액티브가 아니라 DB 응답이 올 때까지 스레드를 막는다. 웹플럭스는 **적은 수의 스레드**로 많은 요청을 처리하므로, 그 스레드를 막으면 서비스 전체가 느려진다. 그래서:

1. 블로킹 코드를 `internalCreateReview` 같은 private 메서드로 옮기고
2. `Mono.fromCallable(() -> internal...())`로 감싼 뒤
3. **`.subscribeOn(jdbcScheduler)`** 로 전용 스레드 풀(`jdbc-pool`)에서 실행한다.

```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ReviewServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

  // ★ 추가 (생성자와 jdbcScheduler 빈: 블로킹 JPA 호출을 별도 스레드 풀에서 실행)
  private final Integer threadPoolSize;
  private final Integer taskQueueSize;

  public ReviewServiceApplication(
    @Value("${app.threadPoolSize:10}") Integer threadPoolSize,
    @Value("${app.taskQueueSize:100}") Integer taskQueueSize
  ) {
    this.threadPoolSize = threadPoolSize;
    this.taskQueueSize = taskQueueSize;
  }

  @Bean
  public Scheduler jdbcScheduler() {
    LOG.info("Creates a jdbcScheduler with thread pool size = {}", threadPoolSize);
    return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
  }

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);

    String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
    LOG.info("Connected to MySQL: " + mysqlUri);
  }
}
```

| 값 | 뜻 |
|---|---|
| `app.threadPoolSize:10` | 스레드 최대 10개 (yml에 없으면 기본값 10) |
| `app.taskQueueSize:100` | 스레드가 다 바쁠 때 기다릴 수 있는 작업 100개 |

```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review.services;

import static java.util.logging.Level.FINE;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ReviewServiceImpl implements ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

  private final ReviewRepository repository;

  private final ReviewMapper mapper;

  private final ServiceUtil serviceUtil;

  private final Scheduler jdbcScheduler;  // ★ 추가

  public ReviewServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
    this.jdbcScheduler = jdbcScheduler;
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Review> createReview(Review body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
    // ★ 변경: 블로킹 JPA 코드를 internalXxx로 옮기고 jdbcScheduler에서 실행
    return Mono.fromCallable(() -> internalCreateReview(body))
      .subscribeOn(jdbcScheduler);
  }

  private Review internalCreateReview(Review body) {
    try {
      ReviewEntity entity = mapper.apiToEntity(body);
      ReviewEntity newEntity = repository.save(entity);

      LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
      return mapper.entityToApi(newEntity);

    } catch (DataIntegrityViolationException dive) {
      throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
    }
  }

  @Override
  public Flux<Review> getReviews(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get reviews for product with id={}", productId);

    return Mono.fromCallable(() -> internalGetReviews(productId))
      .flatMapMany(Flux::fromIterable)
      .log(LOG.getName(), FINE)
      .subscribeOn(jdbcScheduler);
  }

  private List<Review> internalGetReviews(int productId) {

    List<ReviewEntity> entityList = repository.findByProductId(productId);
    List<Review> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    LOG.debug("Response size: {}", list.size());

    return list;
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    return Mono.fromRunnable(() -> internalDeleteReviews(productId)).subscribeOn(jdbcScheduler).then();
  }

  private void internalDeleteReviews(int productId) {

    LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);

    repository.deleteAll(repository.findByProductId(productId));
  }
}
```

## 확인 (도커 필요 없음)
```bash
./gradlew :microservices:product-service:compileJava :microservices:recommendation-service:compileJava :microservices:review-service:compileJava
```
`BUILD SUCCESSFUL`이면 된다. 테스트 코드는 05에서 바꾸므로 `build`는 아직 안 한다(지금 하면 옛 테스트가 컴파일 실패).
