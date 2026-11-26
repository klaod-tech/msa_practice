# 03. 리액티브 영속성 (MongoDB 2개 서비스 + 엔티티 3개)

> 표기: 한 줄만 바뀌면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`. 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 지우고 바꿀 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(`★ 지움:` 줄을 지운다). `★ 참고:` = 교재와 다른 이유. 표시 없는 줄 = 이미 있는 줄. ★ 주석은 입력하지 않아도 된다.
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

// ★ 변경 시작 (전 3줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import java.util.Optional;
// ★ 전: import org.springframework.data.repository.CrudRepository;
// ★ 전: import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
// ★ 변경 끝

// ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String>, CrudRepository<ProductEntity, String> {
// ★ 전: Optional<ProductEntity> findByProductId(int productId);
public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, String> {
  Mono<ProductEntity> findByProductId(int productId);
// ★ 변경 끝
}
```

```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation.persistence;

// ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import java.util.List;
// ★ 전: import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
// ★ 변경 끝

// ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: public interface RecommendationRepository extends CrudRepository<RecommendationEntity, String> {
// ★ 전: List<RecommendationEntity> findByProductId(int productId);
public interface RecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {
  Flux<RecommendationEntity> findByProductId(int productId);
// ★ 변경 끝
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

  // ★ 추가 시작 (3줄) — 로그에 엔티티를 짧게 찍기 위해
  @Override
  public String toString() {
    return format("ProductEntity: %s", productId);
  // ★ 추가 끝
  }
}  // ★ 추가
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

  // ★ 추가 시작 (3줄) — 로그에 엔티티를 짧게 찍기 위해
  @Override
  public String toString() {
    return format("RecommendationEntity: %s/%d", productId, recommendationId);
  // ★ 추가 끝
  }
}  // ★ 추가
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

  // ★ 추가 시작 (3줄) — 로그에 엔티티를 짧게 찍기 위해
  @Override
  public String toString() {
    return format("ReviewEntity: %s/%d", productId, reviewId);
  // ★ 추가 끝
  }
}  // ★ 추가
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
// ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import org.springframework.data.mongodb.core.MongoOperations;
// ★ 전: import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
// ★ 변경 끝
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

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    // ★ 전: String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    // ★ 변경 끝
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;  // ★ 변경 (전: MongoOperations mongoTemplate;)

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: IndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
    // ★ 전: resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e));
    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
    resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e).block());
    // ★ 변경 끝
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
// ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import org.springframework.data.mongodb.core.MongoOperations;
// ★ 전: import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
// ★ 변경 끝
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

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");
    // ★ 전: String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");
    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    // ★ 변경 끝
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;  // ★ 변경 (전: MongoOperations mongoTemplate;)

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: IndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);
    // ★ 전: resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e));
    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);
    resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e).block());
    // ★ 변경 끝
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

// ★ 삭제 시작 (6줄) — 아래 줄들을 지운다
// ★ 지움: import static java.util.stream.IntStream.rangeClosed;
// ★ 지움: import static org.junit.jupiter.api.Assertions.*;
// ★ 지움: import static org.springframework.data.domain.Sort.Direction.ASC;
// ★ 지움: import java.util.List;
// ★ 지움: import java.util.Optional;
// ★ 지움: import java.util.stream.Collectors;
// ★ 삭제 끝
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // 교재와 다름 (6장부터)  // ★ 변경 (전: import org.springframework.boot.data.mongodb.test.autoconfigure.Dat...)
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
// ★ 변경 시작 (전 3줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import org.springframework.data.domain.Page;
// ★ 전: import org.springframework.data.domain.PageRequest;
// ★ 전: import org.springframework.data.domain.Pageable;
import reactor.test.StepVerifier;
// ★ 변경 끝
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
    // ★ 변경 시작 (전 2줄 → 6줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: savedEntity = repository.save(entity);
    // ★ 전: assertEqualsProduct(entity, savedEntity);
    StepVerifier.create(repository.save(entity))
      .expectNextMatches(createdEntity -> {
        savedEntity = createdEntity;
        return areProductEqual(entity, savedEntity);
      })
      .verifyComplete();
    // ★ 변경 끝
  }

  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);  // ★ 추가

    // ★ 변경 시작 (전 2줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity newEntity = new ProductEntity(2, "n", 2);
    // ★ 전: repository.save(newEntity);
    StepVerifier.create(repository.save(newEntity))
      .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
      .verifyComplete();
    // ★ 변경 끝

    // ★ 변경 시작 (전 2줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity foundEntity = repository.findById(newEntity.getId()).get();
    // ★ 전: assertEqualsProduct(newEntity, foundEntity);
    StepVerifier.create(repository.findById(newEntity.getId()))
      .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
      .verifyComplete();
    // ★ 변경 끝

    StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();  // ★ 변경 (전: assertEquals(2, repository.count());)
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    // ★ 변경 시작 (전 1줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: repository.save(savedEntity);
    StepVerifier.create(repository.save(savedEntity))
      .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
      .verifyComplete();
    // ★ 변경 끝

    // ★ 변경 시작 (전 3줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
    // ★ 전: assertEquals(1, (long)foundEntity.getVersion());
    // ★ 전: assertEquals("n2", foundEntity.getName());
    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n2"))
      .verifyComplete();
    // ★ 변경 끝
  }

  @Test
  void delete() {
    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: repository.delete(savedEntity);
    // ★ 전: assertFalse(repository.existsById(savedEntity.getId()));
    StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
    StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
    // ★ 변경 끝
  }

  @Test
  void getByProductId() {
// ★ 삭제: Optional<ProductEntity> entity = repository.findByProductId(savedEntity.getProductId());

    // ★ 변경 시작 (전 2줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: assertTrue(entity.isPresent());
    // ★ 전: assertEqualsProduct(savedEntity, entity.get());
    StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
      .expectNextMatches(foundEntity -> areProductEqual(savedEntity, foundEntity))
      .verifyComplete();
    // ★ 변경 끝
  }

  @Test
  void duplicateError() {
    // ★ 삭제: assertThrows(DuplicateKeyException.class, () -> {
    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
    // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: repository.save(entity);
    // ★ 전: });
    StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
    // ★ 변경 끝
  }

  @Test
  void optimisticLockError() {

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
    // ★ 전: ProductEntity entity2 = repository.findById(savedEntity.getId()).get();
    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();
    // ★ 변경 끝

    entity1.setName("n1");
    repository.save(entity1).block();  // ★ 변경 (전: repository.save(entity1);)

    // ★ 변경 시작 (전 4줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: assertThrows(OptimisticLockingFailureException.class, () -> {
    // ★ 전: entity2.setName("n2");
    // ★ 전: repository.save(entity2);
    // ★ 전: });
    StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();
    // ★ 변경 끝

    // ★ 변경 시작 (전 3줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: ProductEntity updatedEntity = repository.findById(savedEntity.getId()).get();
    // ★ 전: assertEquals(1, (int)updatedEntity.getVersion());
    // ★ 전: assertEquals("n1", updatedEntity.getName());
    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n1"))
      .verifyComplete();
    // ★ 변경 끝
  }

  // ★ 변경 시작 (전 24줄 → 7줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: @Test
  // ★ 전: void paging() {
  // ★ 전: repository.deleteAll();
  // ★ 전: List<ProductEntity> newProducts = rangeClosed(1001, 1010)
  // ★ 전: .mapToObj(i -> new ProductEntity(i, "name " + i, i))
  // ★ 전: .collect(Collectors.toList());
  // ★ 전: repository.saveAll(newProducts);
  // ★ 전: Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
  // ★ 전: nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
  // ★ 전: nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
  // ★ 전: nextPage = testNextPage(nextPage, "[1009, 1010]", false);
  // ★ 전: }
  // ★ 전: ... (12줄 더)
  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return
      (expectedEntity.getId().equals(actualEntity.getId()))
      && (expectedEntity.getVersion() == actualEntity.getVersion())
      && (expectedEntity.getProductId() == actualEntity.getProductId())
      && (expectedEntity.getName().equals(actualEntity.getName()))
      && (expectedEntity.getWeight() == actualEntity.getWeight());
  // ★ 변경 끝
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
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // 교재와 다름 (6장부터)  // ★ 변경 (전: import org.springframework.boot.data.mongodb.test.autoconfigure.Dat...)
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
    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: repository.delete(savedEntity);
    // ★ 전: assertFalse(repository.existsById(savedEntity.getId()));
    repository.delete(savedEntity).block();
    assertFalse(repository.existsById(savedEntity.getId()).block());
    // ★ 변경 끝
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

    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: RecommendationEntity entity1 = repository.findById(savedEntity.getId()).get();
    // ★ 전: RecommendationEntity entity2 = repository.findById(savedEntity.getId()).get();
    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();
    // ★ 변경 끝

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

review의 PersistenceTests는 그대로 둔다(JPA, 블로킹).

## 확인
04 끝에서 컴파일, 05 끝에서 테스트까지 한 번에 확인한다.

다음: [04-core-services-nonblocking.md](04-core-services-nonblocking.md)
