# 02. 핵심 서비스: build.gradle, 테스트, Dockerfile 맞추기

> 시작: 14장까지 따라 한 상태 / 결과: 15장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

- `build.gradle`: 스타터 정리(`spring-boot-starter-restclient` 등). **Lombok 3줄(`compileOnly`/`annotationProcessor`/`lombok-mapstruct-binding`)은 맨 아래에 그대로 둔다.**
- 메인 클래스와 테스트 정리, 테스트용 `application.yml`.
- `Dockerfile`: 레이어를 푸는 폴더 이름이 바뀐다(`/extracted/layers/...` → `/builder/extracted/...`). 결과는 같다.

## 2-1. `microservices/product-service/build.gradle`
```bash
code microservices/product-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    // ★ 변경 시작 (전 3줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: }
    // ★ 전: jar {
    // ★ 전: enabled = false
    id 'java'
    // ★ 변경 끝
}

group = 'se.magnus.microservices.core.product'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    mapstructVersion = '1.6.3'
    springCloudVersion = '2025.1.4'
}

repositories {
    mavenCentral()
}

// ★ 추가 시작 (3줄)
jar {
    enabled = false
}
// ★ 추가 끝

dependencies {
    // ★ 추가 시작 (2줄)
    implementation project(':api')
    implementation project(':util')
    // ★ 추가 끝
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 전: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 전: implementation project(":api")
    // ★ 전: implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-restclient'
    // ★ 변경 끝
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    // ★ 변경 시작 (전 3줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
    // ★ 전: annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
    // ★ 전: annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)
    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'
    // ★ 변경 끝

    // ★ 추가 시작 (2줄)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 추가 끝
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'
    // ★ 추가 시작 (2줄)
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 추가 끝

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    // ★ 변경 시작 (전 2줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
    // ★ 전: testImplementation 'org.testcontainers:testcontainers-mongodb'  // 교재와 다름 (교재: org.testcontainers:mongodb, 6장부터)
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    testImplementation 'org.testcontainers:testcontainers-mongodb'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
    // ★ 변경 끝
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-2. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/ProductServiceApplication.java`
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/ProductServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import reactor.core.publisher.Hooks;
import se.magnus.microservices.core.product.persistence.ProductEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    // ★ 전: String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    // ★ 변경 끝
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
    resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e).block());
  }
}
```

## 2-3. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java`
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import java.util.Objects;  // ★ 추가
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // ★ 변경 (전: import org.springframework.boot.data.mongodb.test.autoconfigure.Dat...)
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();

    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(repository.save(entity))
      .expectNextMatches(createdEntity -> {
        savedEntity = createdEntity;
        return areProductEqual(entity, savedEntity);
      })
      .verifyComplete();
  }

  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);

    StepVerifier.create(repository.save(newEntity))
      .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
      .verifyComplete();

    StepVerifier.create(repository.findById(newEntity.getId()))
      .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
      .verifyComplete();

    StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    StepVerifier.create(repository.save(savedEntity))
      .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
      .verifyComplete();

    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n2"))
      .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
    StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
  }

  @Test
  void getByProductId() {

    StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
      .expectNextMatches(foundEntity -> areProductEqual(savedEntity, foundEntity))
      .verifyComplete();
  }

  @Test
  void duplicateError() {
    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
    StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {

    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

    entity1.setName("n1");
    repository.save(entity1).block();

    StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n1"))
      .verifyComplete();
  }

  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return
      (expectedEntity.getId().equals(actualEntity.getId()))
      && (Objects.equals(expectedEntity.getVersion(), actualEntity.getVersion()))  // ★ 변경 (전: && (expectedEntity.getVersion() == actualEntity.getVersion()))
      && (expectedEntity.getProductId() == actualEntity.getProductId())
      && (expectedEntity.getName().equals(actualEntity.getName()))
      && (expectedEntity.getWeight() == actualEntity.getWeight());
  }
}
```

## 2-4. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java`
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 변경 (전: import org.springframework.boot.webtestclient.autoconfigure.AutoCon...)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@AutoConfigureWebTestClient  // ★ 변경 (전: @AutoConfigureWebTestClient  // 교재와 다름 (Boot 4))
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "eureka.client.enabled=false",
  "spring.cloud.config.enabled=false"})
class ProductServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ProductRepository repository;

  @Autowired
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

    getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_CONTENT)  // ★ 변경 (전: getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재...)
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

## 2-5. `microservices/product-service/src/test/resources/application.yml`
```bash
code microservices/product-service/src/test/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message)
# ★ 전: spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)
spring.web.error.include-message: always
# ★ 변경 끝

spring.cloud.stream.defaultBinder: rabbit
```

## 2-6. `microservices/product-service/Dockerfile`
```bash
code microservices/product-service/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 2-7. `microservices/recommendation-service/build.gradle`
```bash
code microservices/recommendation-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'  // ★ 추가
}

// ★ 변경 시작 (전 3줄 → 0줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: jar {
// ★ 전: enabled = false
// ★ 전: }
// ★ 변경 끝
group = 'se.magnus.microservices.core.recommendation'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    mapstructVersion = '1.6.3'
    springCloudVersion = '2025.1.4'
}

repositories {
    mavenCentral()
}

// ★ 추가 시작 (3줄)
jar {
    enabled = false
}
// ★ 추가 끝

dependencies {
    // ★ 추가 시작 (2줄)
    implementation project(':api')
    implementation project(':util')
    // ★ 추가 끝
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 전: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 전: implementation project(':api')
    // ★ 전: implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-restclient'
    // ★ 변경 끝
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    // ★ 변경 시작 (전 3줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
    // ★ 전: annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
    // ★ 전: annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)
    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'
    // ★ 변경 끝

    // ★ 추가 시작 (2줄)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 추가 끝
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'
    // ★ 추가 시작 (2줄)
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 추가 끝

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    // ★ 변경 시작 (전 2줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
    // ★ 전: testImplementation 'org.testcontainers:testcontainers-mongodb'  // 교재와 다름 (교재: org.testcontainers:mongodb, 6장부터)
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    testImplementation 'org.testcontainers:testcontainers-mongodb'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
    // ★ 변경 끝
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-8. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import reactor.core.publisher.Hooks;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class RecommendationServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceApplication.class);

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    ConfigurableApplicationContext ctx = SpringApplication.run(RecommendationServiceApplication.class, args);

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    // ★ 전: String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    // ★ 변경 끝
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);
    resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e).block());
  }
}
```

## 2-9. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java`
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // ★ 변경 (전: import org.springframework.boot.data.mongodb.test.autoconfigure.Dat...)
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private RecommendationRepository repository;

  private RecommendationEntity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();

    RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
    savedEntity = repository.save(entity).block();

    assertEqualsRecommendation(entity, savedEntity);
  }

  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
    repository.save(newEntity).block();

    RecommendationEntity foundEntity = repository.findById(newEntity.getId()).block();
    assertEqualsRecommendation(newEntity, foundEntity);

    assertEquals(2, (long)repository.count().block());
  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    repository.save(savedEntity).block();

    RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).block();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("a2", foundEntity.getAuthor());
  }

  @Test
  void delete() {
    repository.delete(savedEntity).block();
    assertFalse(repository.existsById(savedEntity.getId()).block());
  }

  @Test
  void getByProductId() {
    List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId()).collectList().block();

    assertThat(entityList, hasSize(1));
    assertEqualsRecommendation(savedEntity, entityList.get(0));
  }

  @Test
  void duplicateError() {
    assertThrows(DuplicateKeyException.class, () -> {
      RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
      repository.save(entity).block();
    });
  }

  @Test
  void optimisticLockError() {

    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();

    entity1.setAuthor("a1");
    repository.save(entity1).block();

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2).block();
    });

    RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).block();
    assertEquals(1, (int)updatedEntity.getVersion());
    assertEquals("a1", updatedEntity.getAuthor());
  }

  private void assertEqualsRecommendation(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
    assertEquals(expectedEntity.getId(),               actualEntity.getId());
    assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
    assertEquals(expectedEntity.getRecommendationId(), actualEntity.getRecommendationId());
    assertEquals(expectedEntity.getAuthor(),           actualEntity.getAuthor());
    assertEquals(expectedEntity.getRating(),           actualEntity.getRating());
    assertEquals(expectedEntity.getContent(),          actualEntity.getContent());
  }
}
```

## 2-10. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java`
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 변경 (전: import org.springframework.boot.webtestclient.autoconfigure.AutoCon...)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@AutoConfigureWebTestClient  // ★ 변경 (전: @AutoConfigureWebTestClient  // 교재와 다름 (Boot 4))
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "eureka.client.enabled=false",
  "spring.cloud.config.enabled=false"})
class RecommendationServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private RecommendationRepository repository;

  @Autowired
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

    getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)  // ★ 변경 (전: getAndVerifyRecommendationsByProductId("?productId=" + productIdInv...)
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

## 2-11. `microservices/recommendation-service/src/test/resources/application.yml`
```bash
code microservices/recommendation-service/src/test/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message)
# ★ 전: spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)
spring.web.error.include-message: always
# ★ 변경 끝

spring.cloud.stream.defaultBinder: rabbit
```

## 2-12. `microservices/recommendation-service/Dockerfile`
```bash
code microservices/recommendation-service/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 2-13. `microservices/review-service/build.gradle`
```bash
code microservices/review-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'  // ★ 추가
}

// ★ 변경 시작 (전 3줄 → 0줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: jar {
// ★ 전: enabled = false
// ★ 전: }
// ★ 변경 끝
group = 'se.magnus.microservices.core.review'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    mapstructVersion = '1.6.3'
    springCloudVersion = '2025.1.4'
}

repositories {
    mavenCentral()
}

// ★ 추가 시작 (3줄)
jar {
    enabled = false
}
// ★ 추가 끝

dependencies {
    // ★ 추가 시작 (2줄)
    implementation project(':api')
    implementation project(':util')
    // ★ 추가 끝
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    // ★ 변경 시작 (전 5줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 전: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 전: implementation project(':api')
    // ★ 전: implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-restclient'
    // ★ 변경 끝
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    // ★ 삭제: implementation 'com.mysql:mysql-connector-j'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation 'net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1'
    implementation 'com.mysql:mysql-connector-j'  // ★ 추가
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    // ★ 변경 시작 (전 3줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
    // ★ 전: annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
    // ★ 전: annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)
    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'
    // ★ 변경 끝

    // ★ 변경 시작 (전 1줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'  // 교재에는 없음 (6장부터)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 변경 끝

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    // ★ 변경 시작 (전 2줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
    // ★ 전: testImplementation 'org.testcontainers:testcontainers-mysql'  // 교재와 다름 (교재: org.testcontainers:mysql, 6장부터)
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    testImplementation 'org.testcontainers:testcontainers-mysql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
    // ★ 변경 끝
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-14. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java`
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 변경 (전: import org.springframework.boot.webtestclient.autoconfigure.AutoCon...)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@AutoConfigureWebTestClient  // ★ 변경 (전: @AutoConfigureWebTestClient  // 교재와 다름 (Boot 4))
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "spring.cloud.stream.defaultBinder=rabbit",
  "logging.level.se.magnus=DEBUG",
  "eureka.client.enabled=false",
  "spring.jpa.hibernate.ddl-auto=update",
  "spring.cloud.config.enabled=false"})
class ReviewServiceApplicationTests extends MySqlTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ReviewRepository repository;

  @Autowired
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

    getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)  // ★ 변경 (전: getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UN...)
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

## 2-15. `microservices/review-service/src/test/resources/application.yml`
```bash
code microservices/review-service/src/test/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message)
# ★ 전: spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)
spring.web.error.include-message: always
# ★ 변경 끝

spring.cloud.stream.defaultBinder: rabbit

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## 2-16. `microservices/review-service/Dockerfile`
```bash
code microservices/review-service/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 2-17. 지울 파일
```bash
cd ~/msa
rm microservices/product-service/.gitattributes
rm microservices/product-service/HELP.md
rm microservices/recommendation-service/.gitattributes
rm microservices/recommendation-service/HELP.md
rm microservices/review-service/.gitattributes
rm microservices/review-service/HELP.md
```

다음: [03-composite.md](03-composite.md)
