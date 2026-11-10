# 03. 리액티브 영속성 (MongoDB 2개 서비스 + 엔티티 3개)

> 표기: `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.
> 교재 "스프링 데이터 MongoDB를 활용한 논블로킹 영속성" 절.

바꾸는 것은 두 가지다(교재 본문 그대로).
1. 리포지토리의 기본 클래스를 **`ReactiveCrudRepository`** 로
2. 직접 만든 `find` 메서드가 **`Mono` 또는 `Flux`** 를 돌려주게

review(MySQL)는 리포지토리를 **안 바꾼다**. JPA는 블로킹이라 04에서 서비스 쪽에서 감싼다.

> ⚠ 이 문서가 끝나면 product/recommendation의 **서비스 구현이 컴파일되지 않는다**(아직 `Optional`, `List`를 기대함). 04까지 하고 확인한다.

## 1. 리포지토리 (2개)
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductRepository.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

// ★ 변경 (전: PagingAndSortingRepository + CrudRepository)
public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, String> {
  Mono<ProductEntity> findByProductId(int productId);  // ★ 변경 (전: Optional<ProductEntity>)
}
```

```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

// ★ 변경 (전: CrudRepository)
public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {
  Flux<RecommendationEntity> findByProductId(int productId);  // ★ 변경 (전: List<RecommendationEntity>)
}
```

`PagingAndSortingRepository`도 뺐으니 6장 PersistenceTests의 `paging` 테스트는 아래 2절에서 없어진다(교재도 삭제).

## 2. 엔티티에 toString 추가 (3개)
교재 7장은 로그에 엔티티를 짧게 찍으려고 `toString()`을 넣는다. Lombok 엔티티(6장)에 메서드만 추가한다.
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductEntity.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product.persistence;

import static java.lang.String.format;  // ★ 추가

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

  @Id private String id;

  @Version private Integer version;

  @Indexed(unique = true)
  private int productId;

  private String name;
  private int weight;

  public ProductEntity(int productId, String name, int weight) {
    this.productId = productId;
    this.name = name;
    this.weight = weight;
  }

  // ★ 추가 (로그에 엔티티를 짧게 찍기 위해)
  @Override
  public String toString() {
    return format("ProductEntity: %s", productId);
  }
}
```

```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationEntity.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

import static java.lang.String.format;  // ★ 추가

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recommendations")
@CompoundIndex(name = "prod-rec-id", unique = true, def = "{'productId': 1, 'recommendationId' : 1}")
@Getter
@Setter
@NoArgsConstructor
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

  public RecommendationEntity(int productId, int recommendationId, String author, int rating, String content) {
    this.productId = productId;
    this.recommendationId = recommendationId;
    this.author = author;
    this.rating = rating;
    this.content = content;
  }

  // ★ 추가 (로그에 엔티티를 짧게 찍기 위해)
  @Override
  public String toString() {
    return format("RecommendationEntity: %s/%d", productId, recommendationId);
  }
}
```

```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewEntity.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review.persistence;

import static java.lang.String.format;  // ★ 추가

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reviews", indexes = { @Index(name = "reviews_unique_idx", unique = true, columnList = "productId,reviewId") })
@Getter
@Setter
@NoArgsConstructor
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

  public ReviewEntity(int productId, int reviewId, String author, String subject, String content) {
    this.productId = productId;
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;
  }

  // ★ 추가 (로그에 엔티티를 짧게 찍기 위해)
  @Override
  public String toString() {
    return format("ReviewEntity: %s/%d", productId, reviewId);
  }
}
```

## 3. Application: 인덱스 만들기를 리액티브로 (2개)
6장에서 시작할 때 인덱스(유일 키)를 만들던 코드가 `MongoOperations`를 썼다. 리액티브 스타터에는 블로킹 템플릿이 없으니 **`ReactiveMongoOperations`** 로 바꾸고, `createIndex(e)`가 이제 `Mono`라서 **`.block()`** 으로 끝날 때까지 기다린다(시작할 때 한 번이라 블로킹해도 된다).
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
import se.magnus.microservices.core.product.persistence.ProductEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;  // ★ 변경 (전: MongoOperations)

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    // ★ 변경 (전: IndexOperations, 아래 줄 끝에 .block() 추가)
    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
    resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e).block());
  }
}
```

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
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class RecommendationServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(RecommendationServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;  // ★ 변경 (전: MongoOperations)

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    // ★ 변경 (전: IndexOperations, 아래 줄 끝에 .block() 추가)
    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);
    resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e).block());
  }
}
```

## 4. PersistenceTests (2개, 전체 교체)
리포지토리가 `Mono`/`Flux`를 돌려주니 결과 검증을 **`StepVerifier`** 로 한다.

| 6장 | 7장 |
|---|---|
| `repository.save(e)` 후 값 비교 | `StepVerifier.create(repository.save(e)).expectNextMatches(...).verifyComplete()` |
| `assertThrows(DuplicateKeyException.class, ...)` | `StepVerifier.create(...).expectError(DuplicateKeyException.class).verify()` |
| 값이 바로 필요할 때 | `.block()` (예: optimisticLockError에서 엔티티 두 개 꺼내기) |

```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // 교재와 다름 (6장부터)
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;  // ★ 추가 (Mono/Flux 결과 검증)
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@DataMongoTest
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

    // Store the saved entity in two separate entity objects
    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

    // Update the entity using the first entity object
    entity1.setName("n1");
    repository.save(entity1).block();

    //  Update the entity using the second entity object.
    // This should fail since the second entity now holds a old version number, i.e. a Optimistic Lock Error
    StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    // Get the updated entity from the database and verify its new sate
    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n1"))
      .verifyComplete();
  }

  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return
      (expectedEntity.getId().equals(actualEntity.getId()))
      && (expectedEntity.getVersion() == actualEntity.getVersion())
      && (expectedEntity.getProductId() == actualEntity.getProductId())
      && (expectedEntity.getName().equals(actualEntity.getName()))
      && (expectedEntity.getWeight() == actualEntity.getWeight());
  }
}
```

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
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // 교재와 다름 (6장부터)
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

    // Store the saved entity in two separate entity objects
    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();

    // Update the entity using the first entity object
    entity1.setAuthor("a1");
    repository.save(entity1).block();

    //  Update the entity using the second entity object.
    // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2).block();
    });

    // Get the updated entity from the database and verify its new sate
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

review의 PersistenceTests는 그대로 둔다(JPA, 블로킹).

## 확인
04 끝에서 컴파일, 05 끝에서 테스트까지 한 번에 확인한다.
