# 01. api: 리액티브 반환형, 이벤트 / util: 리액터 테스트

> 시작: 교수님 `Chapter06` 상태 / 결과: 교수님 `Chapter07`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- 서비스 인터페이스의 반환형이 `Mono<...>`(0~1개), `Flux<...>`(여러 개)로 바뀐다. 핵심 서비스의 생성·삭제는 HTTP 매핑이 빠진다(이벤트로만 부른다).
- `event/Event` (새 파일): 이벤트 종류(CREATE/DELETE), 키(productId), 데이터, 만든 시각.
- `util`의 `ReactorTests` (새 파일): `Flux`, `Mono` 동작을 보여 주는 예제 테스트.

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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;  // ★ 추가
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;  // ★ 추가

@Tag(name = "ProductComposite", description = "REST API for composite product information.")
public interface ProductCompositeService {

  @Operation(
    summary = "${api.product-composite.create-composite-product.description}",
    description = "${api.product-composite.create-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @ResponseStatus(HttpStatus.ACCEPTED)  // ★ 추가
  @PostMapping(
    value    = "/product-composite",
    consumes = "application/json")
  Mono<Void> createProduct(@RequestBody ProductAggregate body);  // ★ 변경 (전: void createProduct(@RequestBody ProductAggregate body);)

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
  Mono<ProductAggregate> getProduct(@PathVariable int productId);  // ★ 변경 (전: ProductAggregate getProduct(@PathVariable int productId);)

  @Operation(
    summary = "${api.product-composite.delete-composite-product.description}",
    description = "${api.product-composite.delete-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @ResponseStatus(HttpStatus.ACCEPTED)  // ★ 추가
  @DeleteMapping(value = "/product-composite/{productId}")
  Mono<Void> deleteProduct(@PathVariable int productId);  // ★ 변경 (전: void deleteProduct(@PathVariable int productId);)
}
```

## 1-2. `api/src/main/java/se/magnus/api/core/product/ProductService.java`
```bash
code api/src/main/java/se/magnus/api/core/product/ProductService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;  // ★ 변경 (전: import org.springframework.web.bind.annotation.*;)
import org.springframework.web.bind.annotation.PathVariable;  // ★ 추가
import reactor.core.publisher.Mono;  // ★ 추가

public interface ProductService {

  // ★ 변경 (1줄, 전: 5줄)
  Mono<Product> createProduct(Product body);

  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Mono<Product> getProduct(@PathVariable int productId);  // ★ 변경 (전: Product getProduct(@PathVariable int productId);)

  Mono<Void> deleteProduct(int productId);  // ★ 변경 (전: @DeleteMapping(value = "/product/{productId}"))
// ★ 삭제: void deleteProduct(@PathVariable int productId);
}
```

## 1-3. `api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java`
```bash
code api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.recommendation;

// ★ 삭제: import java.util.List;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;  // ★ 추가
import reactor.core.publisher.Mono;  // ★ 추가

public interface RecommendationService {

  // ★ 변경 (1줄, 전: 5줄)
  Mono<Recommendation> createRecommendation(Recommendation body);

  @GetMapping(
    value = "/recommendation",
    produces = "application/json")
  Flux<Recommendation> getRecommendations(  // ★ 변경 (전: List<Recommendation> getRecommendations()
    @RequestParam(value = "productId", required = true) int productId);

  Mono<Void> deleteRecommendations(int productId);  // ★ 변경 (전: @DeleteMapping(value = "/recommendation"))
// ★ 삭제: void deleteRecommendations(@RequestParam(value = "productId", requi...
}
```

## 1-4. `api/src/main/java/se/magnus/api/core/review/ReviewService.java`
```bash
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

// ★ 삭제: import java.util.List;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;  // ★ 추가
import reactor.core.publisher.Mono;  // ★ 추가

public interface ReviewService {

  // ★ 변경 (1줄, 전: 5줄)
  Mono<Review> createReview(Review body);

  @GetMapping(
    value = "/review",
    produces = "application/json")
  Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);  // ★ 변경 (전: List<Review> getReviews(@RequestParam(value = "productId", required...)

  Mono<Void> deleteReviews(int productId);  // ★ 변경 (전: @DeleteMapping(value = "/review"))
// ★ 삭제: void deleteReviews(@RequestParam(value = "productId", required = tr...
}
```

## 1-5. `api/src/main/java/se/magnus/api/event/Event.java` (새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/event
code api/src/main/java/se/magnus/api/event/Event.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.event;

import static java.time.ZonedDateTime.now;

import java.time.ZonedDateTime;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;

public class Event<K, T> {

  public enum Type {
    CREATE,
    DELETE
  }

  private Type eventType;
  private K key;
  private T data;
  private ZonedDateTime eventCreatedAt;

  public Event() {
    this.eventType = null;
    this.key = null;
    this.data = null;
    this.eventCreatedAt = null;
  }

  public Event(Type eventType, K key, T data) {
    this.eventType = eventType;
    this.key = key;
    this.data = data;
    this.eventCreatedAt = now();
  }

  public Type getEventType() {
    return eventType;
  }

  public K getKey() {
    return key;
  }

  public T getData() {
    return data;
  }

  @JsonSerialize(using = ZonedDateTimeSerializer.class)
  public ZonedDateTime getEventCreatedAt() {
    return eventCreatedAt;
  }
}
```

## 1-6. `api/src/main/java/se/magnus/api/exceptions/EventProcessingException.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/exceptions/EventProcessingException.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.exceptions;

public class EventProcessingException extends RuntimeException {
  public EventProcessingException() {
  }

  public EventProcessingException(String message) {
    super(message);
  }

  public EventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  public EventProcessingException(Throwable cause) {
    super(cause);
  }
}
```

## 1-7. `util/src/test/java/se/magnus/util/reactor/ReactorTests.java` (새 파일)
```bash
mkdir -p util/src/test/java/se/magnus/util/reactor
code util/src/test/java/se/magnus/util/reactor/ReactorTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.util.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class ReactorTests {

  @Test
  void testFlux() {

    List<Integer> list = new ArrayList<>();

    Flux.just(1, 2, 3, 4)
      .filter(n -> n % 2 == 0)
      .map(n -> n * 2)
      .log()
      .subscribe(n -> list.add(n));

    assertThat(list).containsExactly(4, 8);
  }

  @Test
  void testFluxBlocking() {

    List<Integer> list = Flux.just(1, 2, 3, 4)
      .filter(n -> n % 2 == 0)
      .map(n -> n * 2)
      .log()
      .collectList().block();

    assertThat(list).containsExactly(4, 8);
  }
}
```

다음: [02-core-services.md](02-core-services.md)
