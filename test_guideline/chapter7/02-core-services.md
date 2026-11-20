# 02. 핵심 서비스: 리액티브 저장소, 이벤트 받기

> 시작: 교수님 `Chapter06` 상태 / 결과: 교수님 `Chapter07`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `build.gradle`: 리액티브 MongoDB(product, recommendation), Spring Cloud Stream(RabbitMQ·Kafka 바인더).
- 리포지토리: `ReactiveCrudRepository`(product, recommendation). review는 JPA가 블로킹이라 그대로 두고, 서비스에서 **`jdbcScheduler`**(별도 스레드 풀)로 실행해 리액티브로 감싼다.
- `MessageProcessorConfig` (새 파일): `messageProcessor` 빈이 큐에서 이벤트를 받아 CREATE면 저장, DELETE면 삭제.
- `application.yml`: 바인딩(어느 토픽, 어느 그룹), 재시도와 DLQ, RabbitMQ/Kafka 주소, 파티션 프로필.

## 2-1. `microservices/product-service/build.gradle`
```bash
code microservices/product-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
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
    springCloudVersion = '2025.1.4'  // ★ 추가
}

repositories {
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'  // ★ 변경 (전: implementation 'org.springframework.boot:spring-boot-starter-data-m...)
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가

    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'  // ★ 변경 (전: testImplementation 'org.springframework.boot:spring-boot-starter-da...)
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    testImplementation 'org.testcontainers:testcontainers-mongodb'
}

// ★ 추가 (5줄)
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
import org.springframework.data.mongodb.core.ReactiveMongoOperations;  // ★ 변경 (전: import org.springframework.data.mongodb.core.MongoOperations;)
// ★ 삭제: import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;  // ★ 추가
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import se.magnus.microservices.core.product.persistence.ProductEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;  // ★ 변경 (전: MongoOperations mongoTemplate;)

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);  // ★ 변경 (전: IndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.cla...)
    resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e).block());  // ★ 변경 (전: resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps...)
  }
}
```

## 2-3. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductEntity.java`
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductEntity.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.persistence;

import static java.lang.String.format;  // ★ 추가

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class ProductEntity {

  @Id private String id;

  @Version private Integer version;

  @Indexed(unique = true)
  private int productId;

  private String name;
  private int weight;

  public ProductEntity() {}

  public ProductEntity(int productId, String name, int weight) {
    this.productId = productId;
    this.name = name;
    this.weight = weight;
  // ★ 추가 (4줄)
  }

  @Override
  public String toString() {
    return format("ProductEntity: %s", productId);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }
}
```

## 2-4. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductRepository.java`
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductRepository.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;  // ★ 변경 (전: import java.util.Optional;)
import reactor.core.publisher.Mono;  // ★ 변경 (전: import org.springframework.data.repository.CrudRepository;)
// ★ 삭제: import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, String> {  // ★ 변경 (전: public interface ProductRepository extends PagingAndSortingReposito...)
  Mono<ProductEntity> findByProductId(int productId);  // ★ 변경 (전: Optional<ProductEntity> findByProductId(int productId);)
}
```

## 2-5. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/MessageProcessorConfig.java` (새 파일)
```bash
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

## 2-6. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java`
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
  // ★ 변경 (1줄, 전: 4줄)
  public Mono<Product> createProduct(Product body) {

    if (body.getProductId() < 1) {  // ★ 변경 (전: LOG.debug("createProduct: entity created for productId: {}", body.g...)
      throw new InvalidInputException("Invalid productId: " + body.getProductId());  // ★ 변경 (전: return mapper.entityToApi(newEntity);)
    }  // ★ 추가

    // ★ 변경 (8줄, 전: 3줄)
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
  public Mono<Product> getProduct(int productId) {  // ★ 변경 (전: public Product getProduct(int productId) {)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get product info for id={}", productId);  // ★ 변경 (전: ProductEntity entity = repository.findByProductId(productId))
// ★ 삭제: .orElseThrow(() -> new NotFoundException("No product found for prod...

    // ★ 변경 (5줄, 전: 4줄)
    return repository.findByProductId(productId)
      .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
      .log(LOG.getName(), FINE)
      .map(e -> mapper.entityToApi(e))
      .map(e -> setServiceAddress(e));
  }

  @Override
  // ★ 변경 (4줄, 전: 1줄)
  public Mono<Void> deleteProduct(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    // ★ 변경 (5줄, 전: 1줄)
    return repository.findByProductId(productId).log(LOG.getName(), FINE).map(e -> repository.delete(e)).flatMap(e -> e);
  }

  private Product setServiceAddress(Product e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
  }
}
```

## 2-7. `microservices/product-service/src/main/resources/application.yml`
```bash
code microservices/product-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always

spring.mongodb:
  host: localhost
  port: 27017
  database: product-db

# ★ 추가 (25줄)
spring.cloud.function.definition: messageProcessor

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
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG  # ★ 변경 (전: org.springframework.data.mongodb.core.MongoTemplate: DEBUG)

management.endpoint.health.show-details: "ALWAYS"  # ★ 추가
management.endpoints.web.exposure.include: "*"  # ★ 추가
---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb
# ★ 추가 (19줄)

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

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

## 2-8. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java`
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

// ★ 변경 (1줄, 전: 6줄)
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;  // ★ 변경 (전: import org.springframework.data.domain.Page;)
// ★ 삭제 (2줄, 첫 줄: import org.springframework.data.domain.PageRequ...)
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@DataMongoTest
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();  // ★ 변경 (전: repository.deleteAll();)

    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(repository.save(entity))  // ★ 변경 (전: savedEntity = repository.save(entity);)
      .expectNextMatches(createdEntity -> {  // ★ 추가
        savedEntity = createdEntity;  // ★ 변경 (전: assertEqualsProduct(entity, savedEntity);)
        return areProductEqual(entity, savedEntity);  // ★ 추가
      })  // ★ 추가
      .verifyComplete();  // ★ 추가
  }

  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);  // ★ 추가

    StepVerifier.create(repository.save(newEntity))  // ★ 변경 (전: ProductEntity newEntity = new ProductEntity(2, "n", 2);)
      .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())  // ★ 변경 (전: repository.save(newEntity);)
      .verifyComplete();  // ★ 추가

    StepVerifier.create(repository.findById(newEntity.getId()))  // ★ 변경 (전: ProductEntity foundEntity = repository.findById(newEntity.getId())....)
      .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))  // ★ 변경 (전: assertEqualsProduct(newEntity, foundEntity);)
      .verifyComplete();  // ★ 추가

    StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();  // ★ 변경 (전: assertEquals(2, repository.count());)
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    StepVerifier.create(repository.save(savedEntity))  // ★ 변경 (전: repository.save(savedEntity);)
      .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))  // ★ 추가
      .verifyComplete();  // ★ 추가

    StepVerifier.create(repository.findById(savedEntity.getId()))  // ★ 변경 (전: ProductEntity foundEntity = repository.findById(savedEntity.getId()...)
      .expectNextMatches(foundEntity ->  // ★ 변경 (전: assertEquals(1, (long)foundEntity.getVersion());)
        foundEntity.getVersion() == 1  // ★ 변경 (전: assertEquals("n2", foundEntity.getName());)
        && foundEntity.getName().equals("n2"))  // ★ 추가
      .verifyComplete();  // ★ 추가
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity)).verifyComplete();  // ★ 변경 (전: repository.delete(savedEntity);)
    StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();  // ★ 변경 (전: assertFalse(repository.existsById(savedEntity.getId()));)
  }

  @Test
  void getByProductId() {
// ★ 삭제: Optional<ProductEntity> entity = repository.findByProductId(savedEn...

    StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))  // ★ 변경 (전: assertTrue(entity.isPresent());)
      .expectNextMatches(foundEntity -> areProductEqual(savedEntity, foundEntity))  // ★ 변경 (전: assertEqualsProduct(savedEntity, entity.get());)
      .verifyComplete();  // ★ 추가
  }

  @Test
  void duplicateError() {
    // ★ 변경 (2줄, 전: 4줄)
    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
    StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {

    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();  // ★ 변경 (전: ProductEntity entity1 = repository.findById(savedEntity.getId()).ge...)
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();  // ★ 변경 (전: ProductEntity entity2 = repository.findById(savedEntity.getId()).ge...)

    entity1.setName("n1");
    repository.save(entity1).block();  // ★ 변경 (전: repository.save(entity1);)

    // ★ 변경 (1줄, 전: 4줄)
    StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    StepVerifier.create(repository.findById(savedEntity.getId()))  // ★ 변경 (전: ProductEntity updatedEntity = repository.findById(savedEntity.getId...)
      .expectNextMatches(foundEntity ->  // ★ 변경 (전: assertEquals(1, (int)updatedEntity.getVersion());)
        foundEntity.getVersion() == 1  // ★ 변경 (전: assertEquals("n1", updatedEntity.getName());)
        && foundEntity.getName().equals("n1"))  // ★ 추가
      .verifyComplete();  // ★ 추가
  }

  // ★ 변경 (7줄, 전: 24줄)
  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return
      (expectedEntity.getId().equals(actualEntity.getId()))
      && (Objects.equals(expectedEntity.getVersion(), actualEntity.getVersion()))
      && (expectedEntity.getProductId() == actualEntity.getProductId())
      && (expectedEntity.getName().equals(actualEntity.getName()))
      && (expectedEntity.getWeight() == actualEntity.getWeight());
  }
}
```

## 2-9. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java`
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import static org.junit.jupiter.api.Assertions.*;  // ★ 변경 (전: import static org.junit.jupiter.api.Assertions.assertFalse;)
// ★ 삭제: import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;  // ★ 변경 (전: import static reactor.core.publisher.Mono.just;)
import static se.magnus.api.event.Event.Type.DELETE;  // ★ 추가

import java.util.function.Consumer;  // ★ 추가
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;  // ★ 추가
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;  // ★ 추가
import se.magnus.api.exceptions.InvalidInputException;  // ★ 추가
import se.magnus.microservices.core.product.persistence.ProductRepository;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductServiceApplicationTests extends MongoDbTestBase {

  @Autowired  // ★ 변경 (전: @Autowired private WebTestClient client;)
  private WebTestClient client;  // ★ 추가

  // ★ 변경 (5줄, 전: 1줄)
  @Autowired
  private ProductRepository repository;

  @Autowired
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Product>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();  // ★ 변경 (전: repository.deleteAll();)
  }

  @Test
  void getProductById() {

    int productId = 1;

    assertNull(repository.findByProductId(productId).block());  // ★ 변경 (전: postAndVerifyProduct(productId, OK);)
    assertEquals(0, (long)repository.count().block());  // ★ 추가

    sendCreateProductEvent(productId);  // ★ 변경 (전: assertTrue(repository.findByProductId(productId).isPresent());)

    // ★ 변경 (4줄, 전: 1줄)
    assertNotNull(repository.findByProductId(productId).block());
    assertEquals(1, (long)repository.count().block());

    getAndVerifyProduct(productId, OK)
      .jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void duplicateError() {

    int productId = 1;

    assertNull(repository.findByProductId(productId).block());  // ★ 변경 (전: postAndVerifyProduct(productId, OK);)

    sendCreateProductEvent(productId);  // ★ 변경 (전: assertTrue(repository.findByProductId(productId).isPresent());)

    // ★ 변경 (6줄, 전: 3줄)
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

    sendCreateProductEvent(productId);  // ★ 변경 (전: postAndVerifyProduct(productId, OK);)
    assertNotNull(repository.findByProductId(productId).block());  // ★ 변경 (전: assertTrue(repository.findByProductId(productId).isPresent());)

    sendDeleteProductEvent(productId);  // ★ 변경 (전: deleteAndVerifyProduct(productId, OK);)
    assertNull(repository.findByProductId(productId).block());  // ★ 변경 (전: assertFalse(repository.findByProductId(productId).isPresent());)

    sendDeleteProductEvent(productId);  // ★ 변경 (전: deleteAndVerifyProduct(productId, OK);)
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

    getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_CONTENT)
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

  private void sendCreateProductEvent(int productId) {  // ★ 변경 (전: private WebTestClient.BodyContentSpec postAndVerifyProduct(int prod...)
    Product product = new Product(productId, "Name " + productId, productId, "SA");
    // ★ 변경 (2줄, 전: 8줄)
    Event<Integer, Product> event = new Event<>(CREATE, productId, product);
    messageProcessor.accept(event);
  }

  // ★ 변경 (3줄, 전: 7줄)
  private void sendDeleteProductEvent(int productId) {
    Event<Integer, Product> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

## 2-10. `microservices/recommendation-service/build.gradle`
```bash
code microservices/recommendation-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.core.recommendation'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    mapstructVersion = '1.6.3'
    springCloudVersion = '2025.1.4'  // ★ 추가
}

repositories {
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'  // ★ 변경 (전: implementation 'org.springframework.boot:spring-boot-starter-data-m...)
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'  // ★ 변경 (전: testImplementation 'org.springframework.boot:spring-boot-starter-da...)
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    testImplementation 'org.testcontainers:testcontainers-mongodb'
}

// ★ 추가 (5줄)
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-11. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java`
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
import org.springframework.data.mongodb.core.ReactiveMongoOperations;  // ★ 변경 (전: import org.springframework.data.mongodb.core.MongoOperations;)
// ★ 삭제: import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;  // ★ 추가
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class RecommendationServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(RecommendationServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;  // ★ 변경 (전: MongoOperations mongoTemplate;)

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);  // ★ 변경 (전: IndexOperations indexOps = mongoTemplate.indexOps(RecommendationEnt...)
    resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e).block());  // ★ 변경 (전: resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> i...)
  }
}
```

## 2-12. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationEntity.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationEntity.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

import static java.lang.String.format;  // ★ 추가

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recommendations")
@CompoundIndex(name = "prod-rec-id", unique = true, def = "{'productId': 1, 'recommendationId' : 1}")
public class RecommendationEntity {

  @Id
  private String id;

  @Version
  private Integer version;

  private int productId;
  private int recommendationId;
  private String author;
  private int rating;
  private String content;

  public RecommendationEntity() {
  }

  public RecommendationEntity(int productId, int recommendationId, String author, int rating, String content) {
    this.productId = productId;
    this.recommendationId = recommendationId;
    this.author = author;
    this.rating = rating;
    this.content = content;
  // ★ 추가 (4줄)
  }

  @Override
  public String toString() {
    return format("RecommendationEntity: %s/%d", productId, recommendationId);
  }

  public String getId() {
    return id;
  }

  public Integer getVersion() {
    return version;
  }

  public int getProductId() {
    return productId;
  }

  public int getRecommendationId() {
    return recommendationId;
  }

  public String getAuthor() {
    return author;
  }

  public int getRating() {
    return rating;
  }

  public String getContent() {
    return content;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public void setRecommendationId(int recommendationId) {
    this.recommendationId = recommendationId;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
```

## 2-13. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;  // ★ 변경 (전: import java.util.List;)
import reactor.core.publisher.Flux;  // ★ 변경 (전: import org.springframework.data.repository.CrudRepository;)

public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {  // ★ 변경 (전: public interface RecommendationRepository extends CrudRepository<Re...)
  Flux<RecommendationEntity> findByProductId(int productId);  // ★ 변경 (전: List<RecommendationEntity> findByProductId(int productId);)
}
```

## 2-14. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/MessageProcessorConfig.java` (새 파일)
```bash
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

## 2-15. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java`
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
import reactor.core.publisher.Flux;  // ★ 추가
import reactor.core.publisher.Mono;  // ★ 추가
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
  // ★ 변경 (1줄, 전: 4줄)
  public Mono<Recommendation> createRecommendation(Recommendation body) {

    if (body.getProductId() < 1) {  // ★ 변경 (전: LOG.debug("createRecommendation: created a recommendation entity: {...)
      throw new InvalidInputException("Invalid productId: " + body.getProductId());  // ★ 변경 (전: return mapper.entityToApi(newEntity);)
    }  // ★ 추가

    // ★ 변경 (8줄, 전: 3줄)
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
  public Flux<Recommendation> getRecommendations(int productId) {  // ★ 변경 (전: public List<Recommendation> getRecommendations(int productId) {)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.info("Will get recommendations for product with id={}", productId);  // ★ 변경 (전: List<RecommendationEntity> entityList = repository.findByProductId(...)
// ★ 삭제 (2줄, 첫 줄: List<Recommendation> list = mapper.entityListTo...)

    return repository.findByProductId(productId)  // ★ 변경 (전: LOG.debug("getRecommendations: response size: {}", list.size());)
      .log(LOG.getName(), FINE)  // ★ 추가
      .map(e -> mapper.entityToApi(e))  // ★ 변경 (전: return list;)
      .map(e -> setServiceAddress(e));  // ★ 추가
  }

  @Override
  // ★ 변경 (4줄, 전: 1줄)
  public Mono<Void> deleteRecommendations(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    // ★ 변경 (5줄, 전: 1줄)
    return repository.deleteAll(repository.findByProductId(productId));
  }

  private Recommendation setServiceAddress(Recommendation e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
  }
}
```

## 2-16. `microservices/recommendation-service/src/main/resources/application.yml`
```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always

spring.mongodb:
  host: localhost
  port: 27017
  database: recommendation-db

# ★ 추가 (25줄)
spring.cloud.function.definition: messageProcessor

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
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG  # ★ 변경 (전: org.springframework.data.mongodb.core.MongoTemplate: DEBUG)

management.endpoint.health.show-details: "ALWAYS"  # ★ 추가
management.endpoints.web.exposure.include: "*"  # ★ 추가
---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb
# ★ 추가 (19줄)

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

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

## 2-17. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java`
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
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@DataMongoTest
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private RecommendationRepository repository;

  private RecommendationEntity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();  // ★ 변경 (전: repository.deleteAll();)

    RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
    savedEntity = repository.save(entity).block();  // ★ 변경 (전: savedEntity = repository.save(entity);)

    assertEqualsRecommendation(entity, savedEntity);
  }

  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
    repository.save(newEntity).block();  // ★ 변경 (전: repository.save(newEntity);)

    RecommendationEntity foundEntity = repository.findById(newEntity.getId()).block();  // ★ 변경 (전: RecommendationEntity foundEntity = repository.findById(newEntity.ge...)
    assertEqualsRecommendation(newEntity, foundEntity);

    assertEquals(2, (long)repository.count().block());  // ★ 변경 (전: assertEquals(2, repository.count());)
  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    repository.save(savedEntity).block();  // ★ 변경 (전: repository.save(savedEntity);)

    RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).block();  // ★ 변경 (전: RecommendationEntity foundEntity = repository.findById(savedEntity....)
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("a2", foundEntity.getAuthor());
  }

  @Test
  void delete() {
    repository.delete(savedEntity).block();  // ★ 변경 (전: repository.delete(savedEntity);)
    assertFalse(repository.existsById(savedEntity.getId()).block());  // ★ 변경 (전: assertFalse(repository.existsById(savedEntity.getId()));)
  }

  @Test
  void getByProductId() {
    List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId()).collectList().block();  // ★ 변경 (전: List<RecommendationEntity> entityList = repository.findByProductId(...)

    assertThat(entityList, hasSize(1));
    assertEqualsRecommendation(savedEntity, entityList.get(0));
  }

  @Test
  void duplicateError() {
    assertThrows(DuplicateKeyException.class, () -> {
      RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
      repository.save(entity).block();  // ★ 변경 (전: repository.save(entity);)
    });
  }

  @Test
  void optimisticLockError() {

    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();  // ★ 변경 (전: RecommendationEntity entity1 = repository.findById(savedEntity.getI...)
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();  // ★ 변경 (전: RecommendationEntity entity2 = repository.findById(savedEntity.getI...)

    entity1.setAuthor("a1");
    repository.save(entity1).block();  // ★ 변경 (전: repository.save(entity1);)

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2).block();  // ★ 변경 (전: repository.save(entity2);)
    });

    RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).block();  // ★ 변경 (전: RecommendationEntity updatedEntity = repository.findById(savedEntit...)
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

## 2-18. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java`
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.junit.jupiter.api.Assertions.*;  // ★ 변경 (전: import static org.junit.jupiter.api.Assertions.assertEquals;)
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;  // ★ 변경 (전: import static reactor.core.publisher.Mono.just;)
import static se.magnus.api.event.Event.Type.DELETE;  // ★ 추가

import java.util.function.Consumer;  // ★ 추가
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;  // ★ 추가
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;  // ★ 추가
import se.magnus.api.exceptions.InvalidInputException;  // ★ 추가
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RecommendationServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private RecommendationRepository repository;

  @Autowired  // ★ 추가
  @Qualifier("messageProcessor")  // ★ 추가
  private Consumer<Event<Integer, Recommendation>> messageProcessor;  // ★ 추가

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();  // ★ 변경 (전: repository.deleteAll();)
  }

  @Test
  void getRecommendationsByProductId() {

    int productId = 1;

    sendCreateRecommendationEvent(productId, 1);  // ★ 변경 (전: postAndVerifyRecommendation(productId, 1, OK);)
    sendCreateRecommendationEvent(productId, 2);  // ★ 변경 (전: postAndVerifyRecommendation(productId, 2, OK);)
    sendCreateRecommendationEvent(productId, 3);  // ★ 변경 (전: postAndVerifyRecommendation(productId, 3, OK);)

    assertEquals(3, (long)repository.findByProductId(productId).count().block());  // ★ 변경 (전: assertEquals(3, repository.findByProductId(productId).size());)

    getAndVerifyRecommendationsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].recommendationId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int recommendationId = 1;

    sendCreateRecommendationEvent(productId, recommendationId);  // ★ 변경 (전: postAndVerifyRecommendation(productId, recommendationId, OK))
// ★ 삭제 (2줄, 첫 줄: .jsonPath("$.productId").isEqualTo(productId))

    assertEquals(1, (long)repository.count().block());  // ★ 변경 (전: assertEquals(1, repository.count());)

    InvalidInputException thrown = assertThrows(  // ★ 변경 (전: postAndVerifyRecommendation(productId, recommendationId, UNPROCESSA...)
      InvalidInputException.class,  // ★ 변경 (전: .jsonPath("$.path").isEqualTo("/recommendation"))
      () -> sendCreateRecommendationEvent(productId, recommendationId),  // ★ 변경 (전: .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Rec...)
      "Expected a InvalidInputException here!");  // ★ 추가
    assertEquals("Duplicate key, Product Id: 1, Recommendation Id:1", thrown.getMessage());  // ★ 추가

    assertEquals(1, (long)repository.count().block());  // ★ 변경 (전: assertEquals(1, repository.count());)
  }

  @Test
  void deleteRecommendations() {

    int productId = 1;
    int recommendationId = 1;

    sendCreateRecommendationEvent(productId, recommendationId);  // ★ 변경 (전: postAndVerifyRecommendation(productId, recommendationId, OK);)
    assertEquals(1, (long)repository.findByProductId(productId).count().block());  // ★ 변경 (전: assertEquals(1, repository.findByProductId(productId).size());)

    sendDeleteRecommendationEvent(productId);  // ★ 변경 (전: deleteAndVerifyRecommendationsByProductId(productId, OK);)
    assertEquals(0, (long)repository.findByProductId(productId).count().block());  // ★ 변경 (전: assertEquals(0, repository.findByProductId(productId).size());)

    sendDeleteRecommendationEvent(productId);  // ★ 변경 (전: deleteAndVerifyRecommendationsByProductId(productId, OK);)
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

    getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)
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

  private void sendCreateRecommendationEvent(int productId, int recommendationId) {  // ★ 변경 (전: private WebTestClient.BodyContentSpec postAndVerifyRecommendation(i...)
    Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "SA");
    // ★ 변경 (2줄, 전: 8줄)
    Event<Integer, Recommendation> event = new Event<>(CREATE, productId, recommendation);
    messageProcessor.accept(event);
  }

  // ★ 변경 (3줄, 전: 7줄)
  private void sendDeleteRecommendationEvent(int productId) {
    Event<Integer, Recommendation> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

## 2-19. `microservices/review-service/build.gradle`
```bash
code microservices/review-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.core.review'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    mapstructVersion = '1.6.3'
    springCloudVersion = '2025.1.4'  // ★ 추가
}

repositories {
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
    implementation 'com.mysql:mysql-connector-j'
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
    testImplementation 'org.testcontainers:testcontainers-mysql'
}

// ★ 추가 (5줄)
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-20. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java`
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
import reactor.core.scheduler.Scheduler;  // ★ 추가
import reactor.core.scheduler.Schedulers;  // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")
public class ReviewServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);
  // ★ 추가 (14줄)

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

## 2-21. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewEntity.java`
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewEntity.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review.persistence;

import static java.lang.String.format;  // ★ 추가

import jakarta.persistence.*;

@Entity
@Table(name = "reviews", indexes = { @Index(name = "reviews_unique_idx", unique = true, columnList = "productId,reviewId") })
public class ReviewEntity {

  @Id @GeneratedValue
  private int id;

  @Version
  private int version;

  private int productId;
  private int reviewId;
  private String author;
  private String subject;
  private String content;

  public ReviewEntity() {
  }

  public ReviewEntity(int productId, int reviewId, String author, String subject, String content) {
    this.productId = productId;
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;
  // ★ 추가 (4줄)
  }

  @Override
  public String toString() {
    return format("ReviewEntity: %s/%d", productId, reviewId);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public int getReviewId() {
    return reviewId;
  }

  public void setReviewId(int reviewId) {
    this.reviewId = reviewId;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
```

## 2-22. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/MessageProcessorConfig.java` (새 파일)
```bash
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

## 2-23. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java`
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
import reactor.core.publisher.Flux;  // ★ 추가
import reactor.core.publisher.Mono;  // ★ 추가
import reactor.core.scheduler.Scheduler;  // ★ 추가
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

  private final Scheduler jdbcScheduler;  // ★ 변경 (전: public ReviewServiceImpl(ReviewRepository repository, ReviewMapper ...)

  public ReviewServiceImpl(@Qualifier("jdbcScheduler") Scheduler jdbcScheduler, ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {  // ★ 추가
    this.jdbcScheduler = jdbcScheduler;  // ★ 추가
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  // ★ 변경 (8줄, 전: 1줄)
  public Mono<Review> createReview(Review body) {

    if (body.getProductId() < 1) {
      throw new InvalidInputException("Invalid productId: " + body.getProductId());
    }
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
  public Flux<Review> getReviews(int productId) {  // ★ 변경 (전: public List<Review> getReviews(int productId) {)

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    // ★ 추가 (7줄)
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

    LOG.debug("Response size: {}", list.size());  // ★ 변경 (전: LOG.debug("getReviews: response size: {}", list.size());)

    return list;
  }

  @Override
  // ★ 변경 (7줄, 전: 1줄)
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

## 2-24. `microservices/review-service/src/main/resources/application.yml`
```bash
code microservices/review-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always

spring.jpa.hibernate.ddl-auto: update

spring.datasource:
  url: jdbc:mysql://localhost/review-db
  username: user
  password: pwd

spring.datasource.hikari.initializationFailTimeout: 60000

# ★ 추가 (25줄)
spring.cloud.function.definition: messageProcessor

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

# ★ 변경 (20줄, 전: 2줄)
spring.datasource.url: jdbc:mysql://mysql/review-db

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

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

## 2-25. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java`
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.*;  // ★ 변경 (전: import static org.junit.jupiter.api.Assertions.assertEquals;)
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;  // ★ 변경 (전: import static reactor.core.publisher.Mono.just;)
import static se.magnus.api.event.Event.Type.DELETE;  // ★ 추가

import java.util.function.Consumer;  // ★ 추가
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;  // ★ 추가
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;  // ★ 추가
import se.magnus.api.exceptions.InvalidInputException;  // ★ 추가
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {  // ★ 변경 (전: @SpringBootTest(webEnvironment = RANDOM_PORT))
  "spring.cloud.stream.defaultBinder=rabbit",  // ★ 추가
  "logging.level.se.magnus=DEBUG"})  // ★ 추가
class ReviewServiceApplicationTests extends MySqlTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ReviewRepository repository;

  @Autowired  // ★ 추가
  @Qualifier("messageProcessor")  // ★ 추가
  private Consumer<Event<Integer, Review>> messageProcessor;  // ★ 추가

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  void getReviewsByProductId() {

    int productId = 1;

    assertEquals(0, repository.findByProductId(productId).size());

    sendCreateReviewEvent(productId, 1);  // ★ 변경 (전: postAndVerifyReview(productId, 1, OK);)
    sendCreateReviewEvent(productId, 2);  // ★ 변경 (전: postAndVerifyReview(productId, 2, OK);)
    sendCreateReviewEvent(productId, 3);  // ★ 변경 (전: postAndVerifyReview(productId, 3, OK);)

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

    sendCreateReviewEvent(productId, reviewId);  // ★ 변경 (전: postAndVerifyReview(productId, reviewId, OK))
// ★ 삭제 (2줄, 첫 줄: .jsonPath("$.productId").isEqualTo(productId))

    assertEquals(1, repository.count());

    InvalidInputException thrown = assertThrows(  // ★ 변경 (전: postAndVerifyReview(productId, reviewId, UNPROCESSABLE_CONTENT))
      InvalidInputException.class,  // ★ 변경 (전: .jsonPath("$.path").isEqualTo("/review"))
      () -> sendCreateReviewEvent(productId, reviewId),  // ★ 변경 (전: .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Rev...)
      "Expected a InvalidInputException here!");  // ★ 추가
    assertEquals("Duplicate key, Product Id: 1, Review Id:1", thrown.getMessage());  // ★ 추가

    assertEquals(1, repository.count());
  }

  @Test
  void deleteReviews() {

    int productId = 1;
    int reviewId = 1;

    sendCreateReviewEvent(productId, reviewId);  // ★ 변경 (전: postAndVerifyReview(productId, reviewId, OK);)
    assertEquals(1, repository.findByProductId(productId).size());

    sendDeleteReviewEvent(productId);  // ★ 변경 (전: deleteAndVerifyReviewsByProductId(productId, OK);)
    assertEquals(0, repository.findByProductId(productId).size());

    sendDeleteReviewEvent(productId);  // ★ 변경 (전: deleteAndVerifyReviewsByProductId(productId, OK);)
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

    getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)
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

  private void sendCreateReviewEvent(int productId, int reviewId) {  // ★ 변경 (전: private WebTestClient.BodyContentSpec postAndVerifyReview(int produ...)
    Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
    // ★ 변경 (2줄, 전: 8줄)
    Event<Integer, Review> event = new Event<>(CREATE, productId, review);
    messageProcessor.accept(event);
  }

  // ★ 변경 (3줄, 전: 7줄)
  private void sendDeleteReviewEvent(int productId) {
    Event<Integer, Review> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

다음: [03-composite.md](03-composite.md)
