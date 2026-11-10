# 05. 핵심 서비스가 이벤트를 받게 (Event, MessageProcessorConfig, yml, 테스트)

> 표기: `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.
> 교재 "토픽과 이벤트 정의", "핵심 서비스에서 이벤트 소비" 절.

토픽 3개와 거기 실리는 이벤트:

| 토픽 | 이벤트 | 받는 서비스 (consumer group) |
|---|---|---|
| `products` | `Event<Integer, Product>` | product (`productsGroup`) |
| `recommendations` | `Event<Integer, Recommendation>` | recommendation (`recommendationsGroup`) |
| `reviews` | `Event<Integer, Review>` | review (`reviewsGroup`) |

이벤트 종류는 `CREATE`(data에 객체), `DELETE`(key에 productId만).

## 1. Event 클래스 (api, 새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/event
code api/src/main/java/se/magnus/api/event/Event.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.event;

import static java.time.ZonedDateTime.now;

import java.time.ZonedDateTime;

public class Event<K, T> {

  public enum Type {
    CREATE,
    DELETE
  }

  private final Type eventType;
  private final K key;
  private final T data;
  private final ZonedDateTime eventCreatedAt;

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

  public ZonedDateTime getEventCreatedAt() {
    return eventCreatedAt;
  }
}
```

**교재와 다름:** 교재는 `getEventCreatedAt()` 위에 `@JsonSerialize(using = ZonedDateTimeSerializer.class)`가 있다. 이건 **Jackson 2** 어노테이션이고, Spring Cloud Stream 5는 **Jackson 3**으로 JSON을 만들어서 무시된다. Jackson 3은 기본으로 `"2026-11-10T16:05:37.44+09:00"` 같은 ISO 문자열로 쓰므로 어노테이션과 import 두 줄을 뺐다.

## 2. EventProcessingException (api, 새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/exceptions
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

## 3. MessageProcessorConfig (서비스마다 새 파일 3개)
`Consumer<Event<...>>` 빈 하나가 **토픽 구독자**가 된다. 메시지가 오면 이벤트 종류를 보고 04의 서비스 메서드를 부른다.
- `.block()`: 메시지 처리는 이 메서드가 끝나야 "처리 완료"로 본다. 그래서 저장/삭제가 끝날 때까지 기다린다.
- 처리 중 예외가 나면 yml 설정대로 3번 재시도하고, 그래도 실패하면 DLQ(dead-letter queue)로 보낸다.

```bash
mkdir -p microservices/product-service/src/main/java/se/magnus/microservices/core/product/services
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/MessageProcessorConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product.services;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.EventProcessingException;

@Configuration
public class MessageProcessorConfig {

  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

  private final ProductService productService;

  public MessageProcessorConfig(ProductService productService) {
    this.productService = productService;
  }

  @Bean
  public Consumer<Event<Integer, Product>> messageProcessor() {
    return event -> {
      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch (event.getEventType()) {

        case CREATE:
          Product product = event.getData();
          LOG.info("Create product with ID: {}", product.getProductId());
          productService.createProduct(product).block();
          break;

        case DELETE:
          int productId = event.getKey();
          LOG.info("Delete product with ProductID: {}", productId);
          productService.deleteProduct(productId).block();
          break;

        default:
          String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
          LOG.warn(errorMessage);
          throw new EventProcessingException(errorMessage);
      }

      LOG.info("Message processing done!");

    };
  }
}
```

```bash
mkdir -p microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/MessageProcessorConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation.services;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.EventProcessingException;

@Configuration
public class MessageProcessorConfig {

  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

  private final RecommendationService recommendationService;

  public MessageProcessorConfig(RecommendationService recommendationService) {
    this.recommendationService = recommendationService;
  }

  @Bean
  public Consumer<Event<Integer, Recommendation>> messageProcessor() {
    return event -> {

      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch (event.getEventType()) {

        case CREATE:
          Recommendation recommendation = event.getData();
          LOG.info("Create recommendation with ID: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
          recommendationService.createRecommendation(recommendation).block();
          break;

        case DELETE:
          int productId = event.getKey();
          LOG.info("Delete recommendations with ProductID: {}", productId);
          recommendationService.deleteRecommendations(productId).block();
          break;

        default:
          String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
          LOG.warn(errorMessage);
          throw new EventProcessingException(errorMessage);
      }

      LOG.info("Message processing done!");
    };
  }
}
```

```bash
mkdir -p microservices/review-service/src/main/java/se/magnus/microservices/core/review/services
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/MessageProcessorConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review.services;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.EventProcessingException;

@Configuration
public class MessageProcessorConfig {

  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

  private final ReviewService reviewService;

  public MessageProcessorConfig(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @Bean
  public Consumer<Event<Integer, Review>> messageProcessor() {
    return event -> {
      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch (event.getEventType()) {

        case CREATE:
          Review review = event.getData();
          LOG.info("Create review with ID: {}/{}", review.getProductId(), review.getReviewId());
          reviewService.createReview(review).block();
          break;

        case DELETE:
          int productId = event.getKey();
          LOG.info("Delete reviews with ProductID: {}", productId);
          reviewService.deleteReviews(productId).block();
          break;

        default:
          String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
          LOG.warn(errorMessage);
          throw new EventProcessingException(errorMessage);
      }

      LOG.info("Message processing done!");
    };
  }
}
```

## 4. application.yml (3개, 전체 교체)
```bash
code microservices/product-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: product-service

server.port: 7002
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)

spring.mongodb:  # 교재와 다름 (교재: spring.data.mongodb, 6장부터 유지)
  host: localhost
  port: 27017
  database: product-db

spring.jackson.mapper.allow-final-fields-as-mutators: true  # ★ 추가 (교재에는 없음, Jackson 3: Event의 final 필드 역직렬화)

spring.cloud.function.definition: messageProcessor  # ★ 추가 (여기부터 spring.rabbitmq까지)

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: products
    group: productsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG  # ★ 변경 (전: MongoTemplate)

management.endpoint.health.show-details: "ALWAYS"  # ★ 추가
management.endpoints.web.exposure.include: "*"  # ★ 추가

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb  # 교재와 다름 (교재: spring.data.mongodb.host, 6장부터 유지)

spring.rabbitmq.host: rabbitmq  # ★ 추가

spring.cloud.stream.kafka.binder.brokers: kafka  # ★ 추가

# ★ 추가 (여기부터 파일 끝까지)
---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: recommendation-service

server.port: 7003
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)

spring.mongodb:  # 교재와 다름 (교재: spring.data.mongodb, 6장부터 유지)
  host: localhost
  port: 27017
  database: recommendation-db

spring.jackson.mapper.allow-final-fields-as-mutators: true  # ★ 추가 (교재에는 없음, Jackson 3: Event의 final 필드 역직렬화)

spring.cloud.function.definition: messageProcessor  # ★ 추가 (여기부터 spring.rabbitmq까지)

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: recommendations
    group: recommendationsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG  # ★ 변경 (전: MongoTemplate)

management.endpoint.health.show-details: "ALWAYS"  # ★ 추가
management.endpoints.web.exposure.include: "*"  # ★ 추가

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb  # 교재와 다름 (교재: spring.data.mongodb.host, 6장부터 유지)

spring.rabbitmq.host: rabbitmq  # ★ 추가

spring.cloud.stream.kafka.binder.brokers: kafka  # ★ 추가

# ★ 추가 (여기부터 파일 끝까지)
---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

```bash
code microservices/review-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: review-service

server.port: 7004
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)

# Strongly recommend to set this property to "none" in a production environment!
spring.jpa.hibernate.ddl-auto: update

spring.datasource:
  url: jdbc:mysql://localhost/review-db
  username: user
  password: pwd

spring.datasource.hikari.initializationFailTimeout: 60000

spring.jackson.mapper.allow-final-fields-as-mutators: true  # ★ 추가 (교재에는 없음, Jackson 3: Event의 final 필드 역직렬화)

spring.cloud.function.definition: messageProcessor  # ★ 추가 (여기부터 spring.rabbitmq까지)

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: reviews
    group: reviewsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

management.endpoint.health.show-details: "ALWAYS"  # ★ 추가
management.endpoints.web.exposure.include: "*"  # ★ 추가

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.datasource.url: jdbc:mysql://mysql/review-db  # ★ 변경 (전: spring.datasource: 아래 url 두 줄 → 한 줄로)

spring.rabbitmq.host: rabbitmq  # ★ 추가

spring.cloud.stream.kafka.binder.brokers: kafka  # ★ 추가

# ★ 추가 (여기부터 파일 끝까지)
---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

| 설정 | 뜻 |
|---|---|
| `spring.cloud.function.definition: messageProcessor` | 3절의 빈 이름. 이 함수가 메시지를 받는다 |
| `bindings.messageProcessor-in-0` | 그 함수의 첫 번째 입력(`-in-0`)을 `destination` 토픽에 연결 |
| `group` | 소비자 그룹. 같은 그룹의 인스턴스가 여러 개면 메시지를 **나눠서** 받는다 |
| `maxAttempts`, `backOff...` | 실패 시 3번까지, 0.5초→1초 간격으로 재시도 |
| `autoBindDlq`, `republishToDlq` (rabbit) / `enableDlq` (kafka) | 재시도해도 실패한 메시지를 DLQ로 |
| `spring.rabbitmq.*` | 로컬 실행 시 브로커 주소. `docker` 프로필에서는 `rabbitmq` 컨테이너 |
| `management.*` | `/actuator/health`에 세부 정보까지 보이게 (06의 composite 헬스 체크가 사용) |
| `streaming_partitioned`, `streaming_instance_0/1` 프로필 | 07의 파티션 실습용 |
| `kafka` 프로필 | 07의 Kafka 실습용 (바인더를 kafka로) |

### 교재와 다른 점: `spring.jackson.mapper.allow-final-fields-as-mutators: true`
`Event`의 필드는 전부 `final`이고 setter가 없다. Jackson 3은 기본으로 **final 필드에 값을 넣지 않아서**, 이 줄이 없으면 받은 이벤트의 `eventType`, `key`, `data`가 **전부 null**이 된다(그러면 `switch (event.getEventType())`에서 NullPointerException). composite yml에 3장부터 있는 그 설정과 같은 것이다. Spring Cloud Stream이 스프링 부트의 Jackson 설정을 그대로 가져다 쓰므로 yml 한 줄로 해결된다.

## 5. 서비스 테스트 (3개, 전체 교체)
HTTP POST/DELETE가 없어졌으니, 테스트는 **`messageProcessor` 빈을 주입받아 이벤트를 직접 넣는다**(`messageProcessor.accept(event)`). 조회(GET)는 그대로 WebTestClient로 확인한다.
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ProductRepository repository;

  @Autowired
  // ★ 추가 (HTTP POST/DELETE 대신 이벤트를 직접 넣어 테스트)
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Product>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();
  }

  @Test
  void getProductById() {

    int productId = 1;

    assertNull(repository.findByProductId(productId).block());
    assertEquals(0, (long)repository.count().block());

    sendCreateProductEvent(productId);

    assertNotNull(repository.findByProductId(productId).block());
    assertEquals(1, (long)repository.count().block());

    getAndVerifyProduct(productId, OK)
      .jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void duplicateError() {

    int productId = 1;

    assertNull(repository.findByProductId(productId).block());

    sendCreateProductEvent(productId);

    assertNotNull(repository.findByProductId(productId).block());

    InvalidInputException thrown = assertThrows(
      InvalidInputException.class,
      () -> sendCreateProductEvent(productId),
      "Expected a InvalidInputException here!");
    assertEquals("Duplicate key, Product Id: " + productId, thrown.getMessage());
  }

  @Test
  void deleteProduct() {

    int productId = 1;

    sendCreateProductEvent(productId);
    assertNotNull(repository.findByProductId(productId).block());

    sendDeleteProductEvent(productId);
    assertNull(repository.findByProductId(productId).block());

    sendDeleteProductEvent(productId);
  }

  @Test
  void getProductInvalidParameterString() {

    getAndVerifyProduct("/no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/product/no-integer")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getProductNotFound() {

    int productIdNotFound = 13;
    getAndVerifyProduct(productIdNotFound, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
      .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
  }

  @Test
  void getProductInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return getAndVerifyProduct("/" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
    return client.get()
      .uri("/product" + productIdPath)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void sendCreateProductEvent(int productId) {
    Product product = new Product(productId, "Name " + productId, productId, "SA");
    Event<Integer, Product> event = new Event<>(CREATE, productId, product);
    messageProcessor.accept(event);
  }

  private void sendDeleteProductEvent(int productId) {
    Event<Integer, Product> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RecommendationServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private RecommendationRepository repository;

  @Autowired
  // ★ 추가 (HTTP POST/DELETE 대신 이벤트를 직접 넣어 테스트)
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Recommendation>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();
  }

  @Test
  void getRecommendationsByProductId() {

    int productId = 1;

    sendCreateRecommendationEvent(productId, 1);
    sendCreateRecommendationEvent(productId, 2);
    sendCreateRecommendationEvent(productId, 3);

    assertEquals(3, (long)repository.findByProductId(productId).count().block());

    getAndVerifyRecommendationsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].recommendationId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int recommendationId = 1;

    sendCreateRecommendationEvent(productId, recommendationId);

    assertEquals(1, (long)repository.count().block());

    InvalidInputException thrown = assertThrows(
      InvalidInputException.class,
      () -> sendCreateRecommendationEvent(productId, recommendationId),
      "Expected a InvalidInputException here!");
    assertEquals("Duplicate key, Product Id: 1, Recommendation Id:1", thrown.getMessage());

    assertEquals(1, (long)repository.count().block());
  }

  @Test
  void deleteRecommendations() {

    int productId = 1;
    int recommendationId = 1;

    sendCreateRecommendationEvent(productId, recommendationId);
    assertEquals(1, (long)repository.findByProductId(productId).count().block());

    sendDeleteRecommendationEvent(productId);
    assertEquals(0, (long)repository.findByProductId(productId).count().block());

    sendDeleteRecommendationEvent(productId);
  }

  @Test
  void getRecommendationsMissingParameter() {

    getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getRecommendationsInvalidParameter() {

    getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getRecommendationsNotFound() {

    getAndVerifyRecommendationsByProductId("?productId=113", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getRecommendationsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
    return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus expectedStatus) {
    return client.get()
      .uri("/recommendation" + productIdQuery)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void sendCreateRecommendationEvent(int productId, int recommendationId) {
    Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "SA");
    Event<Integer, Recommendation> event = new Event<>(CREATE, productId, recommendation);
    messageProcessor.accept(event);
  }

  private void sendDeleteRecommendationEvent(int productId) {
    Event<Integer, Recommendation> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "spring.cloud.stream.defaultBinder=rabbit",
  "logging.level.se.magnus=DEBUG"})
class ReviewServiceApplicationTests extends MySqlTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ReviewRepository repository;

  @Autowired
  // ★ 추가 (HTTP POST/DELETE 대신 이벤트를 직접 넣어 테스트)
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Review>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  void getReviewsByProductId() {

    int productId = 1;

    assertEquals(0, repository.findByProductId(productId).size());

    sendCreateReviewEvent(productId, 1);
    sendCreateReviewEvent(productId, 2);
    sendCreateReviewEvent(productId, 3);

    assertEquals(3, repository.findByProductId(productId).size());

    getAndVerifyReviewsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].reviewId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int reviewId = 1;

    assertEquals(0, repository.count());

    sendCreateReviewEvent(productId, reviewId);

    assertEquals(1, repository.count());

    InvalidInputException thrown = assertThrows(
      InvalidInputException.class,
      () -> sendCreateReviewEvent(productId, reviewId),
      "Expected a InvalidInputException here!");
    assertEquals("Duplicate key, Product Id: 1, Review Id:1", thrown.getMessage());

    assertEquals(1, repository.count());
  }

  @Test
  void deleteReviews() {

    int productId = 1;
    int reviewId = 1;

    sendCreateReviewEvent(productId, reviewId);
    assertEquals(1, repository.findByProductId(productId).size());

    sendDeleteReviewEvent(productId);
    assertEquals(0, repository.findByProductId(productId).size());

    sendDeleteReviewEvent(productId);
  }

  @Test
  void getReviewsMissingParameter() {

    getAndVerifyReviewsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getReviewsInvalidParameter() {

    getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getReviewsNotFound() {

    getAndVerifyReviewsByProductId("?productId=213", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getReviewsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
    return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
    return client.get()
      .uri("/review" + productIdQuery)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void sendCreateReviewEvent(int productId, int reviewId) {
    Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
    Event<Integer, Review> event = new Event<>(CREATE, productId, review);
    messageProcessor.accept(event);
  }

  private void sendDeleteReviewEvent(int productId) {
    Event<Integer, Review> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

## 확인 (도커 필요: Testcontainers)
```bash
./gradlew :microservices:product-service:build
./gradlew :microservices:recommendation-service:build
./gradlew :microservices:review-service:build
```
`BUILD SUCCESSFUL`이면 된다. 03의 PersistenceTests도 여기서 같이 돈다.
