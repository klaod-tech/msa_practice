# 02. 핵심 서비스: DB, 엔티티, 매퍼, 테스트

> 시작: 교수님 `Chapter05` 상태 / 결과: 교수님 `Chapter06`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

서비스마다 같은 모양으로 영속성 계층을 만든다(product, recommendation은 MongoDB, review는 MySQL).

| 파일 | 하는 일 |
|---|---|
| `build.gradle` | Spring Data(MongoDB 또는 JPA + MySQL 드라이버), MapStruct, Testcontainers 의존성 |
| `persistence/XxxEntity` (새 파일) | DB에 저장되는 모양. `id`, `version`(낙관적 잠금) 등 DB용 필드 |
| `persistence/XxxRepository` (새 파일) | Spring Data 리포지토리. `findByProductId` 같은 메서드는 이름만으로 쿼리가 만들어진다 |
| `services/XxxMapper` (새 파일) | API 객체 ↔ 엔티티 변환. MapStruct가 구현 코드를 빌드할 때 만든다 |
| `services/XxxServiceImpl` | 고정 데이터 대신 리포지토리로 저장·조회·삭제 |
| `XxxServiceApplication` | MongoDB 인덱스 만들기(product, recommendation) 등 시작 시 설정 |
| `application.yml` | DB 주소. 기본은 `localhost`, docker 프로필은 컨테이너 이름(`mongodb`, `mysql`) |
| 테스트 | `PersistenceTests`(리포지토리), `MapperTests`(매퍼), `MongoDbTestBase`/`MySqlTestBase`: **Testcontainers**가 테스트용 DB를 도커로 잠깐 띄운다 |

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

ext {  // ★ 추가
    mapstructVersion = '1.6.3'  // ★ 추가
}  // ★ 추가

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
    // ★ 추가 (5줄)
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-test'  // ★ 추가
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'  // ★ 추가
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // ★ 추가
    testImplementation 'org.testcontainers:testcontainers-mongodb'  // ★ 추가
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

import org.slf4j.Logger;  // ★ 추가
import org.slf4j.LoggerFactory;  // ★ 추가
import org.springframework.beans.factory.annotation.Autowired;  // ★ 추가
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;  // ★ 추가
import org.springframework.context.annotation.ComponentScan;
// ★ 추가 (10줄)
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import se.magnus.microservices.core.product.persistence.ProductEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);  // ★ 추가

  public static void main(String[] args) {
    // ★ 변경 (13줄, 전: 1줄)
    ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  MongoOperations mongoTemplate;

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    IndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
    resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e));
  }
}
```

## 2-3. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductEntity.java` (새 파일)
```bash
mkdir -p microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductEntity.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product.persistence;

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

## 2-4. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductRepository.java` (새 파일)
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductRepository.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product.persistence;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String>, CrudRepository<ProductEntity, String> {
  Optional<ProductEntity> findByProductId(int productId);
}
```

## 2-5. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductMapper.java` (새 파일)
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductMapper.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductEntity;

@Mapper(componentModel = "spring")
public interface ProductMapper {

  @Mappings({
    @Mapping(target = "serviceAddress", ignore = true)
  })
  Product entityToApi(ProductEntity entity);

  @Mappings({
    @Mapping(target = "id", ignore = true), @Mapping(target = "version", ignore = true)
  })
  ProductEntity apiToEntity(Product api);
}
```

## 2-6. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java`
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;  // ★ 추가
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.core.product.persistence.ProductEntity;  // ★ 추가
import se.magnus.microservices.core.product.persistence.ProductRepository;  // ★ 추가
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

  private final ServiceUtil serviceUtil;

  // ★ 변경 (5줄, 전: 1줄)
  private final ProductRepository repository;

  private final ProductMapper mapper;

  public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  // ★ 추가 (11줄)
  public Product createProduct(Product body) {
    try {
      ProductEntity entity = mapper.apiToEntity(body);
      ProductEntity newEntity = repository.save(entity);

      LOG.debug("createProduct: entity created for productId: {}", body.getProductId());
      return mapper.entityToApi(newEntity);

    } catch (DuplicateKeyException dke) {
      throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
    }
  }

  @Override
  public Product getProduct(int productId) {
// ★ 삭제: LOG.debug("/product return the found product for productId={}", pro...

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    ProductEntity entity = repository.findByProductId(productId)  // ★ 변경 (전: if (productId == 13) {)
      .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));  // ★ 변경 (전: throw new NotFoundException("No product found for productId: " + pr...)
// ★ 삭제: }

    // ★ 변경 (9줄, 전: 1줄)
    Product response = mapper.entityToApi(entity);
    response.setServiceAddress(serviceUtil.getServiceAddress());

    LOG.debug("getProduct: found productId: {}", response.getProductId());

    return response;
  }

  @Override
  public void deleteProduct(int productId) {
    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    repository.findByProductId(productId).ifPresent(e -> repository.delete(e));
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
# ★ 추가 (4줄)

spring.mongodb:
  host: localhost
  port: 27017
  database: product-db

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.MongoTemplate: DEBUG  # ★ 추가

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb  # ★ 추가
```

## 2-8. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/MapperTests.java` (새 파일)
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/MapperTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.services.ProductMapper;

class MapperTests {

  private ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

  @Test
  void mapperTests() {

    assertNotNull(mapper);

    Product api = new Product(1, "n", 1, "sa");

    ProductEntity entity = mapper.apiToEntity(api);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getName(), entity.getName());
    assertEquals(api.getWeight(), entity.getWeight());

    Product api2 = mapper.entityToApi(entity);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getName(),      api2.getName());
    assertEquals(api.getWeight(),    api2.getWeight());
    assertNull(api2.getServiceAddress());
  }
}
```

## 2-9. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/MongoDbTestBase.java` (새 파일)
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/MongoDbTestBase.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;

public abstract class MongoDbTestBase {

  @ServiceConnection
  private static MongoDBContainer database = new MongoDBContainer("mongo:8.0.5");

  static {
    database.start();
  }

}
```

## 2-10. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java` (새 파일)
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.ASC;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@DataMongoTest
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();

    ProductEntity entity = new ProductEntity(1, "n", 1);
    savedEntity = repository.save(entity);

    assertEqualsProduct(entity, savedEntity);
  }

  @Test
  void create() {

    ProductEntity newEntity = new ProductEntity(2, "n", 2);
    repository.save(newEntity);

    ProductEntity foundEntity = repository.findById(newEntity.getId()).get();
    assertEqualsProduct(newEntity, foundEntity);

    assertEquals(2, repository.count());
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    repository.save(savedEntity);

    ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("n2", foundEntity.getName());
  }

  @Test
  void delete() {
    repository.delete(savedEntity);
    assertFalse(repository.existsById(savedEntity.getId()));
  }

  @Test
  void getByProductId() {
    Optional<ProductEntity> entity = repository.findByProductId(savedEntity.getProductId());

    assertTrue(entity.isPresent());
    assertEqualsProduct(savedEntity, entity.get());
  }

  @Test
  void duplicateError() {
    assertThrows(DuplicateKeyException.class, () -> {
      ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
      repository.save(entity);
    });
  }

  @Test
  void optimisticLockError() {

    ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).get();

    entity1.setName("n1");
    repository.save(entity1);

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setName("n2");
      repository.save(entity2);
    });

    ProductEntity updatedEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (int)updatedEntity.getVersion());
    assertEquals("n1", updatedEntity.getName());
  }

  @Test
  void paging() {

    repository.deleteAll();

    List<ProductEntity> newProducts = rangeClosed(1001, 1010)
      .mapToObj(i -> new ProductEntity(i, "name " + i, i))
      .collect(Collectors.toList());
    repository.saveAll(newProducts);

    Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
    nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
    nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
    nextPage = testNextPage(nextPage, "[1009, 1010]", false);
  }

  private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
    Page<ProductEntity> productPage = repository.findAll(nextPage);
    assertEquals(expectedProductIds, productPage.getContent().stream().map(p -> p.getProductId()).collect(Collectors.toList()).toString());
    assertEquals(expectsNextPage, productPage.hasNext());
    return productPage.nextPageable();
  }

  private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
    assertEquals(expectedEntity.getId(),               actualEntity.getId());
    assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
    assertEquals(expectedEntity.getName(),           actualEntity.getName());
    assertEquals(expectedEntity.getWeight(),           actualEntity.getWeight());
  }
}
```

## 2-11. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java`
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import static org.junit.jupiter.api.Assertions.assertFalse;  // ★ 추가
import static org.junit.jupiter.api.Assertions.assertTrue;  // ★ 추가
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;  // ★ 변경 (전: import static org.springframework.http.HttpStatus.BAD_REQUEST;)
// ★ 삭제: import static org.springframework.http.HttpStatus.UNPROCESSABLE_CON...
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;  // ★ 추가

import org.junit.jupiter.api.BeforeEach;  // ★ 추가
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;  // ★ 추가
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;  // ★ 추가
import se.magnus.microservices.core.product.persistence.ProductRepository;  // ★ 추가

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductServiceApplicationTests extends MongoDbTestBase {  // ★ 변경 (전: class ProductServiceApplicationTests {)

  @Autowired private WebTestClient client;
  // ★ 추가 (5줄)

  @Autowired private ProductRepository repository;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  void getProductById() {

    int productId = 1;

    // ★ 변경 (21줄, 전: 8줄)
    postAndVerifyProduct(productId, OK);

    assertTrue(repository.findByProductId(productId).isPresent());

    getAndVerifyProduct(productId, OK).jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void duplicateError() {

    int productId = 1;

    postAndVerifyProduct(productId, OK);

    assertTrue(repository.findByProductId(productId).isPresent());

    postAndVerifyProduct(productId, UNPROCESSABLE_CONTENT)
      .jsonPath("$.path").isEqualTo("/product")
      .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
  }

  @Test
  void deleteProduct() {

    int productId = 1;

    postAndVerifyProduct(productId, OK);
    assertTrue(repository.findByProductId(productId).isPresent());

    deleteAndVerifyProduct(productId, OK);
    assertFalse(repository.findByProductId(productId).isPresent());

    deleteAndVerifyProduct(productId, OK);
  }

  @Test
  void getProductInvalidParameterString() {

    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyProduct("/no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/product/no-integer")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getProductNotFound() {

    int productIdNotFound = 13;
    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyProduct(productIdNotFound, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
      .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
  }

  @Test
  void getProductInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    // ★ 변경 (10줄, 전: 2줄)
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
      .expectStatus().isEqualTo(expectedStatus)  // ★ 변경 (전: .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT))
      .expectHeader().contentType(APPLICATION_JSON)
      // ★ 변경 (20줄, 전: 3줄)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    Product product = new Product(productId, "Name " + productId, productId, "SA");
    return client.post()
      .uri("/product")
      .body(just(product), Product.class)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return client.delete()
      .uri("/product/" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectBody();
  }
}
```

## 2-12. `microservices/product-service/src/test/resources/logback-test.xml` (새 파일)
```bash
mkdir -p microservices/product-service/src/test/resources
code microservices/product-service/src/test/resources/logback-test.xml
```
붙여넣기 (새 파일)
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

## 2-13. `microservices/recommendation-service/build.gradle`
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

ext {  // ★ 추가
    mapstructVersion = '1.6.3'  // ★ 추가
}  // ★ 추가

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
    // ★ 추가 (5줄)
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-test'  // ★ 추가
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'  // ★ 추가
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // ★ 추가
    testImplementation 'org.testcontainers:testcontainers-mongodb'  // ★ 추가
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-14. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import org.slf4j.Logger;  // ★ 추가
import org.slf4j.LoggerFactory;  // ★ 추가
import org.springframework.beans.factory.annotation.Autowired;  // ★ 추가
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;  // ★ 추가
import org.springframework.context.annotation.ComponentScan;
// ★ 추가 (10줄)
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class RecommendationServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceApplication.class);  // ★ 추가

  public static void main(String[] args) {
    // ★ 변경 (13줄, 전: 1줄)
    ConfigurableApplicationContext ctx = SpringApplication.run(RecommendationServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  MongoOperations mongoTemplate;

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    IndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);
    resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e));
  }
}
```

## 2-15. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationEntity.java` (새 파일)
```bash
mkdir -p microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationEntity.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

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

## 2-16. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java` (새 파일)
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface RecommendationRepository extends CrudRepository<RecommendationEntity, String> {
  List<RecommendationEntity> findByProductId(int productId);
}
```

## 2-17. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationMapper.java` (새 파일)
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationMapper.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation.services;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

  @Mappings({
    @Mapping(target = "rate", source = "entity.rating"),
    @Mapping(target = "serviceAddress", ignore = true)
  })
  Recommendation entityToApi(RecommendationEntity entity);

  @Mappings({
    @Mapping(target = "rating", source = "api.rate"),
    @Mapping(target = "id", ignore = true),
    @Mapping(target = "version", ignore = true)
  })
  RecommendationEntity apiToEntity(Recommendation api);

  List<Recommendation> entityListToApiList(List<RecommendationEntity> entity);

  List<RecommendationEntity> apiListToEntityList(List<Recommendation> api);
}
```

## 2-18. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.services;

// ★ 삭제: import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;  // ★ 추가
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;  // ★ 추가
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;  // ★ 추가
import se.magnus.util.http.ServiceUtil;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final RecommendationRepository repository;  // ★ 추가

  private final RecommendationMapper mapper;  // ★ 추가

  private final ServiceUtil serviceUtil;

  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper, ServiceUtil serviceUtil) {  // ★ 변경 (전: public RecommendationServiceImpl(ServiceUtil serviceUtil) {)
    this.repository = repository;  // ★ 추가
    this.mapper = mapper;  // ★ 추가
    this.serviceUtil = serviceUtil;
  // ★ 추가 (11줄)
  }

  @Override
  public Recommendation createRecommendation(Recommendation body) {
    try {
      RecommendationEntity entity = mapper.apiToEntity(body);
      RecommendationEntity newEntity = repository.save(entity);

      LOG.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(), body.getRecommendationId());
      return mapper.entityToApi(newEntity);

    } catch (DuplicateKeyException dke) {
      throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId());
    }
  }

  @Override
  public List<Recommendation> getRecommendations(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    // ★ 변경 (3줄, 전: 4줄)
    List<RecommendationEntity> entityList = repository.findByProductId(productId);
    List<Recommendation> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    // ★ 변경 (1줄, 전: 5줄)
    LOG.debug("getRecommendations: response size: {}", list.size());

    return list;
  }
  // ★ 추가 (5줄)

  @Override
  public void deleteRecommendations(int productId) {
    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    repository.deleteAll(repository.findByProductId(productId));
  }
}
```

## 2-19. `microservices/recommendation-service/src/main/resources/application.yml`
```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always
# ★ 추가 (4줄)

spring.mongodb:
  host: localhost
  port: 27017
  database: recommendation-db

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.MongoTemplate: DEBUG  # ★ 추가

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb  # ★ 추가
```

## 2-20. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/MapperTests.java` (새 파일)
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/MapperTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.services.RecommendationMapper;

class MapperTests {

  private RecommendationMapper mapper = Mappers.getMapper(RecommendationMapper.class);

  @Test
  void mapperTests() {

    assertNotNull(mapper);

    Recommendation api = new Recommendation(1, 2, "a", 4, "C", "adr");

    RecommendationEntity entity = mapper.apiToEntity(api);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getRecommendationId(), entity.getRecommendationId());
    assertEquals(api.getAuthor(), entity.getAuthor());
    assertEquals(api.getRate(), entity.getRating());
    assertEquals(api.getContent(), entity.getContent());

    Recommendation api2 = mapper.entityToApi(entity);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getRecommendationId(), api2.getRecommendationId());
    assertEquals(api.getAuthor(), api2.getAuthor());
    assertEquals(api.getRate(), api2.getRate());
    assertEquals(api.getContent(), api2.getContent());
    assertNull(api2.getServiceAddress());
  }

  @Test
  void mapperListTests() {

    assertNotNull(mapper);

    Recommendation api = new Recommendation(1, 2, "a", 4, "C", "adr");
    List<Recommendation> apiList = Collections.singletonList(api);

    List<RecommendationEntity> entityList = mapper.apiListToEntityList(apiList);
    assertEquals(apiList.size(), entityList.size());

    RecommendationEntity entity = entityList.get(0);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getRecommendationId(), entity.getRecommendationId());
    assertEquals(api.getAuthor(), entity.getAuthor());
    assertEquals(api.getRate(), entity.getRating());
    assertEquals(api.getContent(), entity.getContent());

    List<Recommendation> api2List = mapper.entityListToApiList(entityList);
    assertEquals(apiList.size(), api2List.size());

    Recommendation api2 = api2List.get(0);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getRecommendationId(), api2.getRecommendationId());
    assertEquals(api.getAuthor(), api2.getAuthor());
    assertEquals(api.getRate(), api2.getRate());
    assertEquals(api.getContent(), api2.getContent());
    assertNull(api2.getServiceAddress());
  }
}
```

## 2-21. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/MongoDbTestBase.java` (새 파일)
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/MongoDbTestBase.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;

public abstract class MongoDbTestBase {

  @ServiceConnection
  private static MongoDBContainer database = new MongoDBContainer("mongo:8.0.5");

  static {
    database.start();
  }

}
```

## 2-22. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java` (새 파일)
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java
```
붙여넣기 (새 파일)
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
    repository.deleteAll();

    RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
    savedEntity = repository.save(entity);

    assertEqualsRecommendation(entity, savedEntity);
  }

  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
    repository.save(newEntity);

    RecommendationEntity foundEntity = repository.findById(newEntity.getId()).get();
    assertEqualsRecommendation(newEntity, foundEntity);

    assertEquals(2, repository.count());
  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    repository.save(savedEntity);

    RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("a2", foundEntity.getAuthor());
  }

  @Test
  void delete() {
    repository.delete(savedEntity);
    assertFalse(repository.existsById(savedEntity.getId()));
  }

  @Test
  void getByProductId() {
    List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId());

    assertThat(entityList, hasSize(1));
    assertEqualsRecommendation(savedEntity, entityList.get(0));
  }

  @Test
  void duplicateError() {
    assertThrows(DuplicateKeyException.class, () -> {
      RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
      repository.save(entity);
    });
  }

  @Test
  void optimisticLockError() {

    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).get();
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).get();

    entity1.setAuthor("a1");
    repository.save(entity1);

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2);
    });

    RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).get();
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

## 2-23. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java`
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;  // ★ 추가
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;  // ★ 변경 (전: import static org.springframework.http.HttpStatus.BAD_REQUEST;)
// ★ 삭제: import static org.springframework.http.HttpStatus.UNPROCESSABLE_CON...
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;  // ★ 추가

import org.junit.jupiter.api.BeforeEach;  // ★ 추가
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;  // ★ 추가
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;  // ★ 추가
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;  // ★ 추가

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class RecommendationServiceApplicationTests extends MongoDbTestBase {  // ★ 변경 (전: class RecommendationServiceApplicationTests {)

  // ★ 변경 (8줄, 전: 1줄)
  @Autowired
  private WebTestClient client;

  @Autowired
  private RecommendationRepository repository;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  void getRecommendationsByProductId() {

    int productId = 1;

    // ★ 변경 (31줄, 전: 9줄)
    postAndVerifyRecommendation(productId, 1, OK);
    postAndVerifyRecommendation(productId, 2, OK);
    postAndVerifyRecommendation(productId, 3, OK);

    assertEquals(3, repository.findByProductId(productId).size());

    getAndVerifyRecommendationsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].recommendationId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int recommendationId = 1;

    postAndVerifyRecommendation(productId, recommendationId, OK)
      .jsonPath("$.productId").isEqualTo(productId)
      .jsonPath("$.recommendationId").isEqualTo(recommendationId);

    assertEquals(1, repository.count());

    postAndVerifyRecommendation(productId, recommendationId, UNPROCESSABLE_CONTENT)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Recommendation Id:1");

    assertEquals(1, repository.count());
  }

  @Test
  void deleteRecommendations() {

    int productId = 1;
    int recommendationId = 1;

    postAndVerifyRecommendation(productId, recommendationId, OK);
    assertEquals(1, repository.findByProductId(productId).size());

    deleteAndVerifyRecommendationsByProductId(productId, OK);
    assertEquals(0, repository.findByProductId(productId).size());

    deleteAndVerifyRecommendationsByProductId(productId, OK);
  }

  @Test
  void getRecommendationsMissingParameter() {

    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getRecommendationsInvalidParameter() {

    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getRecommendationsNotFound() {

    // ★ 변경 (2줄, 전: 9줄)
    getAndVerifyRecommendationsByProductId("?productId=113", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getRecommendationsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    // ★ 변경 (10줄, 전: 2줄)
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
      .expectStatus().isEqualTo(expectedStatus)  // ★ 변경 (전: .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT))
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();  // ★ 변경 (전: .expectBody())
  // ★ 삭제 (2줄, 첫 줄: .jsonPath("$.path").isEqualTo("/recommendation"))
  }
  // ★ 추가 (19줄)

  private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus expectedStatus) {
    Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "SA");
    return client.post()
      .uri("/recommendation")
      .body(just(recommendation), Recommendation.class)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
    return client.delete()
      .uri("/recommendation?productId=" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectBody();
  }

}
```

## 2-24. `microservices/recommendation-service/src/test/resources/logback-test.xml` (새 파일)
```bash
mkdir -p microservices/recommendation-service/src/test/resources
code microservices/recommendation-service/src/test/resources/logback-test.xml
```
붙여넣기 (새 파일)
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

## 2-25. `microservices/review-service/build.gradle`
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

ext {  // ★ 추가
    mapstructVersion = '1.6.3'  // ★ 추가
}  // ★ 추가

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
    // ★ 추가 (6줄)
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'com.mysql:mysql-connector-j'
    implementation "org.mapstruct:mapstruct:${mapstructVersion}"

    compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'  // ★ 추가
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

    testImplementation 'org.springframework.boot:spring-boot-testcontainers'  // ★ 추가
    testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // ★ 추가
    testImplementation 'org.testcontainers:testcontainers-mysql'  // ★ 추가
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-26. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java`
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import org.slf4j.Logger;  // ★ 추가
import org.slf4j.LoggerFactory;  // ★ 추가
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;  // ★ 추가
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ReviewServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);  // ★ 추가

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);  // ★ 변경 (전: SpringApplication.run(ReviewServiceApplication.class, args);)

    String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");  // ★ 추가
    LOG.info("Connected to MySQL: " + mysqlUri);  // ★ 추가
  }
}
```

## 2-27. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewEntity.java` (새 파일)
```bash
mkdir -p microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewEntity.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review.persistence;

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

## 2-28. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewRepository.java` (새 파일)
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewRepository.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ReviewRepository extends CrudRepository<ReviewEntity, Integer> {

  @Transactional(readOnly = true)
  List<ReviewEntity> findByProductId(int productId);
}
```

## 2-29. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewMapper.java` (새 파일)
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewMapper.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review.services;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistence.ReviewEntity;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

  @Mappings({
    @Mapping(target = "serviceAddress", ignore = true)
  })
  Review entityToApi(ReviewEntity entity);

  @Mappings({
    @Mapping(target = "id", ignore = true),
    @Mapping(target = "version", ignore = true)
  })
  ReviewEntity apiToEntity(Review api);

  List<Review> entityListToApiList(List<ReviewEntity> entity);

  List<ReviewEntity> apiListToEntityList(List<Review> api);
}
```

## 2-30. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java`
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review.services;

// ★ 삭제: import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;  // ★ 추가
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.review.persistence.ReviewEntity;  // ★ 추가
import se.magnus.microservices.core.review.persistence.ReviewRepository;  // ★ 추가
import se.magnus.util.http.ServiceUtil;

@RestController
public class ReviewServiceImpl implements ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

  private final ReviewRepository repository;  // ★ 추가

  private final ReviewMapper mapper;  // ★ 추가

  private final ServiceUtil serviceUtil;

  public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {  // ★ 변경 (전: public ReviewServiceImpl(ServiceUtil serviceUtil) {)
    this.repository = repository;  // ★ 추가
    this.mapper = mapper;  // ★ 추가
    this.serviceUtil = serviceUtil;
  // ★ 추가 (11줄)
  }

  @Override
  public Review createReview(Review body) {
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
  public List<Review> getReviews(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    // ★ 변경 (3줄, 전: 4줄)
    List<ReviewEntity> entityList = repository.findByProductId(productId);
    List<Review> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    // ★ 변경 (1줄, 전: 5줄)
    LOG.debug("getReviews: response size: {}", list.size());

    return list;
  }
  // ★ 추가 (5줄)

  @Override
  public void deleteReviews(int productId) {
    LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
    repository.deleteAll(repository.findByProductId(productId));
  }
}
```

## 2-31. `microservices/review-service/src/main/resources/application.yml`
```bash
code microservices/review-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always
# ★ 추가 (6줄)

spring.jpa.hibernate.ddl-auto: update

spring.datasource:
  url: jdbc:mysql://localhost/review-db
  username: user
  password: pwd

spring.datasource.hikari.initializationFailTimeout: 60000

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.hibernate.SQL: DEBUG  # ★ 추가
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # ★ 추가

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.datasource:  # ★ 추가
  url: jdbc:mysql://mysql/review-db  # ★ 추가
```

## 2-32. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/MapperTests.java` (새 파일)
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/MapperTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.services.ReviewMapper;

class MapperTests {

  private ReviewMapper mapper = Mappers.getMapper(ReviewMapper.class);

  @Test
  void mapperTests() {

    assertNotNull(mapper);

    Review api = new Review(1, 2, "a", "s", "C", "adr");

    ReviewEntity entity = mapper.apiToEntity(api);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getReviewId(), entity.getReviewId());
    assertEquals(api.getAuthor(), entity.getAuthor());
    assertEquals(api.getSubject(), entity.getSubject());
    assertEquals(api.getContent(), entity.getContent());

    Review api2 = mapper.entityToApi(entity);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getReviewId(), api2.getReviewId());
    assertEquals(api.getAuthor(), api2.getAuthor());
    assertEquals(api.getSubject(), api2.getSubject());
    assertEquals(api.getContent(), api2.getContent());
    assertNull(api2.getServiceAddress());
  }

  @Test
  void mapperListTests() {

    assertNotNull(mapper);

    Review api = new Review(1, 2, "a", "s", "C", "adr");
    List<Review> apiList = Collections.singletonList(api);

    List<ReviewEntity> entityList = mapper.apiListToEntityList(apiList);
    assertEquals(apiList.size(), entityList.size());

    ReviewEntity entity = entityList.get(0);

    assertEquals(api.getProductId(), entity.getProductId());
    assertEquals(api.getReviewId(), entity.getReviewId());
    assertEquals(api.getAuthor(), entity.getAuthor());
    assertEquals(api.getSubject(), entity.getSubject());
    assertEquals(api.getContent(), entity.getContent());

    List<Review> api2List = mapper.entityListToApiList(entityList);
    assertEquals(apiList.size(), api2List.size());

    Review api2 = api2List.get(0);

    assertEquals(api.getProductId(), api2.getProductId());
    assertEquals(api.getReviewId(), api2.getReviewId());
    assertEquals(api.getAuthor(), api2.getAuthor());
    assertEquals(api.getSubject(), api2.getSubject());
    assertEquals(api.getContent(), api2.getContent());
    assertNull(api2.getServiceAddress());
  }
}
```

## 2-33. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/MySqlTestBase.java` (새 파일)
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/MySqlTestBase.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mysql.MySQLContainer;

public abstract class MySqlTestBase {

  @ServiceConnection
  static final JdbcDatabaseContainer database = new MySQLContainer("mysql:9.2.0").withStartupTimeoutSeconds(300);

  static {
    database.start();
  }

}
```

## 2-34. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/PersistenceTests.java` (새 파일)
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/PersistenceTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@DataJpaTest
@Transactional(propagation = NOT_SUPPORTED)
class PersistenceTests extends MySqlTestBase {

  @Autowired
  private ReviewRepository repository;

  private ReviewEntity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();

    ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
    savedEntity = repository.save(entity);

    assertEqualsReview(entity, savedEntity);
  }

  @Test
  void create() {

    ReviewEntity newEntity = new ReviewEntity(1, 3, "a", "s", "c");
    repository.save(newEntity);

    ReviewEntity foundEntity = repository.findById(newEntity.getId()).get();
    assertEqualsReview(newEntity, foundEntity);

    assertEquals(2, repository.count());
  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    repository.save(savedEntity);

    ReviewEntity foundEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("a2", foundEntity.getAuthor());
  }

  @Test
  void delete() {
    repository.delete(savedEntity);
    assertFalse(repository.existsById(savedEntity.getId()));
  }

  @Test
  void getByProductId() {
    List<ReviewEntity> entityList = repository.findByProductId(savedEntity.getProductId());

    assertThat(entityList, hasSize(1));
    assertEqualsReview(savedEntity, entityList.get(0));
  }

  @Test
  void duplicateError() {
    assertThrows(DataIntegrityViolationException.class, () -> {
      ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
      repository.save(entity);
    });

  }

  @Test
  void optimisticLockError() {

    ReviewEntity entity1 = repository.findById(savedEntity.getId()).get();
    ReviewEntity entity2 = repository.findById(savedEntity.getId()).get();

    entity1.setAuthor("a1");
    repository.save(entity1);

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2);
    });

    ReviewEntity updatedEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (int)updatedEntity.getVersion());
    assertEquals("a1", updatedEntity.getAuthor());
  }

  private void assertEqualsReview(ReviewEntity expectedEntity, ReviewEntity actualEntity) {
    assertEquals(expectedEntity.getId(),        actualEntity.getId());
    assertEquals(expectedEntity.getVersion(),   actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
    assertEquals(expectedEntity.getReviewId(),  actualEntity.getReviewId());
    assertEquals(expectedEntity.getAuthor(),    actualEntity.getAuthor());
    assertEquals(expectedEntity.getSubject(),   actualEntity.getSubject());
    assertEquals(expectedEntity.getContent(),   actualEntity.getContent());
  }
}
```

## 2-35. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java`
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.assertEquals;  // ★ 추가
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;  // ★ 변경 (전: import static org.springframework.http.HttpStatus.BAD_REQUEST;)
// ★ 삭제: import static org.springframework.http.HttpStatus.UNPROCESSABLE_CON...
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;  // ★ 추가

import org.junit.jupiter.api.BeforeEach;  // ★ 추가
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;  // ★ 추가
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;  // ★ 추가
import se.magnus.microservices.core.review.persistence.ReviewRepository;  // ★ 추가

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ReviewServiceApplicationTests extends MySqlTestBase {  // ★ 변경 (전: class ReviewServiceApplicationTests {)

  // ★ 변경 (8줄, 전: 1줄)
  @Autowired
  private WebTestClient client;

  @Autowired
  private ReviewRepository repository;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  void getReviewsByProductId() {

    int productId = 1;

    // ★ 변경 (33줄, 전: 9줄)
    assertEquals(0, repository.findByProductId(productId).size());

    postAndVerifyReview(productId, 1, OK);
    postAndVerifyReview(productId, 2, OK);
    postAndVerifyReview(productId, 3, OK);

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

    postAndVerifyReview(productId, reviewId, OK)
      .jsonPath("$.productId").isEqualTo(productId)
      .jsonPath("$.reviewId").isEqualTo(reviewId);

    assertEquals(1, repository.count());

    postAndVerifyReview(productId, reviewId, UNPROCESSABLE_CONTENT)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Review Id:1");

    assertEquals(1, repository.count());
  }

  @Test
  void deleteReviews() {

    int productId = 1;
    int reviewId = 1;

    postAndVerifyReview(productId, reviewId, OK);
    assertEquals(1, repository.findByProductId(productId).size());

    deleteAndVerifyReviewsByProductId(productId, OK);
    assertEquals(0, repository.findByProductId(productId).size());

    deleteAndVerifyReviewsByProductId(productId, OK);
  }

  @Test
  void getReviewsMissingParameter() {

    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyReviewsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getReviewsInvalidParameter() {

    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getReviewsNotFound() {

    // ★ 변경 (2줄, 전: 9줄)
    getAndVerifyReviewsByProductId("?productId=213", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getReviewsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    // ★ 변경 (10줄, 전: 2줄)
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
      .expectStatus().isEqualTo(expectedStatus)  // ★ 변경 (전: .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT))
      .expectHeader().contentType(APPLICATION_JSON)
      // ★ 변경 (20줄, 전: 3줄)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec postAndVerifyReview(int productId, int reviewId, HttpStatus expectedStatus) {
    Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
    return client.post()
      .uri("/review")
      .body(just(review), Review.class)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
    return client.delete()
      .uri("/review?productId=" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectBody();
  }
}
```

## 2-36. `microservices/review-service/src/test/resources/logback-test.xml` (새 파일)
```bash
mkdir -p microservices/review-service/src/test/resources
code microservices/review-service/src/test/resources/logback-test.xml
```
붙여넣기 (새 파일)
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

다음: [03-composite.md](03-composite.md)
