# 04. 핵심 서비스를 논블로킹으로 (API 인터페이스 + 서비스 구현)

> 표기: 한 줄만 바뀌면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`. 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 지우고 바꿀 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(`★ 지움:` 줄을 지운다). `★ 참고:` = 교재와 다른 이유. 표시 없는 줄 = 이미 있는 줄. ★ 주석은 입력하지 않아도 된다.
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

// ★ 변경 시작 (전 1줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;
// ★ 변경 끝

public interface ProductService {

  // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @PostMapping(
  // ★ 전: value    = "/product",
  // ★ 전: consumes = "application/json",
  // ★ 전: produces = "application/json")
  // ★ 전: Product createProduct(@RequestBody Product body);
  Mono<Product> createProduct(Product body);
  // ★ 변경 끝

  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Mono<Product> getProduct(@PathVariable int productId);  // ★ 변경 (전: Product getProduct(@PathVariable int productId);)

  // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @DeleteMapping(value = "/product/{productId}")
  // ★ 전: void deleteProduct(@PathVariable int productId);
  Mono<Void> deleteProduct(int productId);
  // ★ 변경 끝
}
```

```bash
code api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.recommendation;

// ★ 삭제: import java.util.List;
import org.springframework.web.bind.annotation.*;
// ★ 추가 시작 (2줄)
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// ★ 추가 끝

public interface RecommendationService {

  // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @PostMapping(
  // ★ 전: value    = "/recommendation",
  // ★ 전: consumes = "application/json",
  // ★ 전: produces = "application/json")
  // ★ 전: Recommendation createRecommendation(@RequestBody Recommendation body);
  Mono<Recommendation> createRecommendation(Recommendation body);
  // ★ 변경 끝

  @GetMapping(
    value = "/recommendation",
    produces = "application/json")
  Flux<Recommendation> getRecommendations(  // ★ 변경 (전: List<Recommendation> getRecommendations()
    @RequestParam(value = "productId", required = true) int productId);

  // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @DeleteMapping(value = "/recommendation")
  // ★ 전: void deleteRecommendations(@RequestParam(value = "productId", required = true)  int productId);
  Mono<Void> deleteRecommendations(int productId);
  // ★ 변경 끝
}
```

```bash
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

// ★ 삭제: import java.util.List;
import org.springframework.web.bind.annotation.*;
// ★ 추가 시작 (2줄)
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// ★ 추가 끝

public interface ReviewService {

  // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @PostMapping(
  // ★ 전: value    = "/review",
  // ★ 전: consumes = "application/json",
  // ★ 전: produces = "application/json")
  // ★ 전: Review createReview(@RequestBody Review body);
  Mono<Review> createReview(Review body);
  // ★ 변경 끝

  @GetMapping(
    value = "/review",
    produces = "application/json")
  // ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
  Flux<Review> getReviews(
   @RequestParam(value = "productId", required = true) int productId);
  // ★ 변경 끝

  // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @DeleteMapping(value = "/review")
  // ★ 전: void deleteReviews(@RequestParam(value = "productId", required = true)  int productId);
  Mono<Void> deleteReviews(int productId);
  // ★ 변경 끝
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

import static java.util.logging.Level.FINE;  // ★ 추가

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;  // ★ 추가
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
  // ★ 변경 시작 (전 2줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public Product createProduct(Product body) {
  // ★ 전: try {
  public Mono<Product> createProduct(Product body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
  // ★ 변경 끝

    ProductEntity entity = mapper.apiToEntity(body);
    // ★ 변경 시작 (전 1줄 → 6줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity newEntity = repository.save(entity);
    Mono<Product> newEntity = repository.save(entity)
      .log(LOG.getName(), FINE)
      .onErrorMap(
        DuplicateKeyException.class,
        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
      .map(e -> mapper.entityToApi(e));
    // ★ 변경 끝

    // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: LOG.debug("createProduct: entity created for productId: {}", body.getProductId());
    // ★ 전: return mapper.entityToApi(newEntity);
    // ★ 전: } catch (DuplicateKeyException dke) {
    // ★ 전: throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
    // ★ 전: }
    return newEntity;
    // ★ 변경 끝
  }

  @Override
  public Mono<Product> getProduct(int productId) {  // ★ 변경 (전: public Product getProduct(int productId) {)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity entity = repository.findByProductId(productId)
    // ★ 전: .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));
    LOG.info("Will get product info for id={}", productId);
    // ★ 변경 끝

    // ★ 변경 시작 (전 4줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: Product response = mapper.entityToApi(entity);
    // ★ 전: response.setServiceAddress(serviceUtil.getServiceAddress());
    // ★ 전: LOG.debug("getProduct: found productId: {}", response.getProductId());
    // ★ 전: return response;
    return repository.findByProductId(productId)
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
      .log(LOG.getName(), FINE)
      .map(e -> mapper.entityToApi(e))
      .map(e -> setServiceAddress(e));
    // ★ 변경 끝
  }

  @Override
  // ★ 변경 시작 (전 1줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public void deleteProduct(int productId) {
  public Mono<Void> deleteProduct(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  // ★ 변경 끝

    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    // ★ 변경 시작 (전 1줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: repository.findByProductId(productId).ifPresent(e -> repository.delete(e));
    return repository.findByProductId(productId).log(LOG.getName(), FINE).map(e -> repository.delete(e)).flatMap(e -> e);
  }

  private Product setServiceAddress(Product e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
    // ★ 변경 끝
  }
}
```

```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.services;

import static java.util.logging.Level.FINE;  // ★ 변경 (전: import java.util.List;)

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
// ★ 추가 시작 (2줄)
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// ★ 추가 끝
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
  // ★ 변경 시작 (전 2줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public Recommendation createRecommendation(Recommendation body) {
  // ★ 전: try {
  public Mono<Recommendation> createRecommendation(Recommendation body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
  // ★ 변경 끝

    RecommendationEntity entity = mapper.apiToEntity(body);
    // ★ 변경 시작 (전 1줄 → 6줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: RecommendationEntity newEntity = repository.save(entity);
    Mono<Recommendation> newEntity = repository.save(entity)
      .log(LOG.getName(), FINE)
      .onErrorMap(
        DuplicateKeyException.class,
        ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
      .map(e -> mapper.entityToApi(e));
    // ★ 변경 끝

    // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: LOG.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(), body.getRecommendationId());
    // ★ 전: return mapper.entityToApi(newEntity);
    // ★ 전: } catch (DuplicateKeyException dke) {
    // ★ 전: throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId());
    // ★ 전: }
    return newEntity;
    // ★ 변경 끝
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {  // ★ 변경 (전: public List<Recommendation> getRecommendations(int productId) {)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    // ★ 변경 시작 (전 3줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: List<RecommendationEntity> entityList = repository.findByProductId(productId);
    // ★ 전: List<Recommendation> list = mapper.entityListToApiList(entityList);
    // ★ 전: list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));
    LOG.info("Will get recommendations for product with id={}", productId);
    // ★ 변경 끝

    // ★ 변경 시작 (전 2줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: LOG.debug("getRecommendations: response size: {}", list.size());
    // ★ 전: return list;
    return repository.findByProductId(productId)
      .log(LOG.getName(), FINE)
      .map(e -> mapper.entityToApi(e))
      .map(e -> setServiceAddress(e));
    // ★ 변경 끝
  }

  @Override
  // ★ 변경 시작 (전 1줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public void deleteRecommendations(int productId) {
  public Mono<Void> deleteRecommendations(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  // ★ 변경 끝

    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    // ★ 변경 시작 (전 1줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: repository.deleteAll(repository.findByProductId(productId));
    return repository.deleteAll(repository.findByProductId(productId));
  }

  private Recommendation setServiceAddress(Recommendation e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
    // ★ 변경 끝
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
import org.springframework.beans.factory.annotation.Value;  // ★ 추가
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;  // ★ 추가
import org.springframework.context.annotation.ComponentScan;
// ★ 추가 시작 (2줄)
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
// ★ 추가 끝

@SpringBootApplication
@ComponentScan("se.magnus")
public class ReviewServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

  // ★ 추가 시작 (14줄) — 생성자와 jdbcScheduler 빈: 블로킹 JPA 호출을 별도 스레드 풀에서 실행
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
  // ★ 추가 끝

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

import static java.util.logging.Level.FINE;  // ★ 추가

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;  // ★ 추가
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
// ★ 추가 시작 (3줄)
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
// ★ 추가 끝
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

  // ★ 변경 시작 (전 1줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
  private final Scheduler jdbcScheduler;

  public ReviewServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
    this.jdbcScheduler = jdbcScheduler;
  // ★ 변경 끝
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  // ★ 변경 시작 (전 1줄 → 8줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public Review createReview(Review body) {
  public Mono<Review> createReview(Review body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
    return Mono.fromCallable(() -> internalCreateReview(body))  // ★ 참고: 블로킹 JPA 코드를 internalXxx로 옮기고 jdbcScheduler에서 실행
      .subscribeOn(jdbcScheduler);
  }

  private Review internalCreateReview(Review body) {
  // ★ 변경 끝
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
  public Flux<Review> getReviews(int productId) {  // ★ 변경 (전: public List<Review> getReviews(int productId) {)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    // ★ 추가 시작 (7줄)
    LOG.info("Will get reviews for product with id={}", productId);

    return Mono.fromCallable(() -> internalGetReviews(productId))
      .flatMapMany(Flux::fromIterable)
      .log(LOG.getName(), FINE)
      .subscribeOn(jdbcScheduler);
  }

  private List<Review> internalGetReviews(int productId) {
    // ★ 추가 끝

    List<ReviewEntity> entityList = repository.findByProductId(productId);
    List<Review> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    LOG.debug("Response size: {}", list.size());  // ★ 변경 (전: LOG.debug("getReviews: response size: {}", list.size());)

    return list;
  }

  @Override
  // ★ 변경 시작 (전 1줄 → 7줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public void deleteReviews(int productId) {
  public Mono<Void> deleteReviews(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    return Mono.fromRunnable(() -> internalDeleteReviews(productId)).subscribeOn(jdbcScheduler).then();
  }

  private void internalDeleteReviews(int productId) {
  // ★ 변경 끝

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

다음: [05-core-event-consumers.md](05-core-event-consumers.md)
