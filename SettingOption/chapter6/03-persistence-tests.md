# 03. 영속성 테스트 (Testcontainers)

> 새 파일들. 교재 6장 내용 그대로이고, `★ 변경 (교재: ...)` 표시한 **import 줄만** Boot 4 / Testcontainers 2에 맞게 다르다.

엔티티와 리포지토리가 진짜 DB에서 제대로 동작하는지 테스트한다. **Testcontainers**가 테스트 시작 때 도커로 MongoDB/MySQL 컨테이너를 띄우고, 테스트가 끝나면 지운다. 그래서 **Docker Desktop이 켜져 있어야** 한다.

## 1. 테스트 로그 설정 (3개 서비스 공통)
테스트 출력이 너무 많지 않게 로그 수준을 INFO로 맞춘다.
```bash
mkdir -p microservices/product-service/src/test/resources
```
`code microservices/product-service/src/test/resources/logback-test.xml` 들어가서 붙여넣기
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

나머지 두 서비스는 같은 파일이라 복사:
```bash
mkdir -p microservices/recommendation-service/src/test/resources microservices/review-service/src/test/resources
cp microservices/product-service/src/test/resources/logback-test.xml microservices/recommendation-service/src/test/resources/
cp microservices/product-service/src/test/resources/logback-test.xml microservices/review-service/src/test/resources/
```

## 2. product-service
`code microservices/product-service/src/test/java/se/magnus/microservices/core/product/MongoDbTestBase.java` 들어가서 붙여넣기
```java
package se.magnus.microservices.core.product;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;  // ★ 변경 (교재: org.testcontainers.containers.MongoDBContainer)

public abstract class MongoDbTestBase {

  @ServiceConnection
  private static MongoDBContainer database = new MongoDBContainer("mongo:8.0.5");

  static {
    database.start();
  }

}
```

`code microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java` 들어가서 붙여넣기
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
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // ★ 변경 (교재: org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest)
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

    // Store the saved entity in two separate entity objects
    ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).get();

    // Update the entity using the first entity object
    entity1.setName("n1");
    repository.save(entity1);

    // Update the entity using the second entity object.
    // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setName("n2");
      repository.save(entity2);
    }); 

    // Get the updated entity from the database and verify its new sate
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

## 3. recommendation-service
`code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/MongoDbTestBase.java` 들어가서 붙여넣기 (product와 `package` 줄만 다르다)
```java
package se.magnus.microservices.core.recommendation;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;  // ★ 변경 (교재: org.testcontainers.containers.MongoDBContainer)

public abstract class MongoDbTestBase {

  @ServiceConnection
  private static MongoDBContainer database = new MongoDBContainer("mongo:8.0.5");

  static {
    database.start();
  }

}
```

`code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java` 들어가서 붙여넣기
```java
package se.magnus.microservices.core.recommendation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // ★ 변경 (교재: org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest)
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

    // Store the saved entity in two separate entity objects
    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).get();
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).get();

    // Update the entity using the first entity object
    entity1.setAuthor("a1");
    repository.save(entity1);

    //  Update the entity using the second entity object.
    // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2);
    });

    // Get the updated entity from the database and verify its new sate
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

## 4. review-service
`code microservices/review-service/src/test/java/se/magnus/microservices/core/review/MySqlTestBase.java` 들어가서 붙여넣기
```java
package se.magnus.microservices.core.review;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mysql.MySQLContainer;  // ★ 변경 (교재: org.testcontainers.containers.MySQLContainer)

public abstract class MySqlTestBase {

  // Extend startup timeout since a MySQLContainer with MySQL 8 starts very slow on Win10/WSL2
  @ServiceConnection
  static final JdbcDatabaseContainer database = new MySQLContainer("mysql:9.2.0").withStartupTimeoutSeconds(300);

  static {
    database.start();
  }

}
```

`code microservices/review-service/src/test/java/se/magnus/microservices/core/review/PersistenceTests.java` 들어가서 붙여넣기
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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;  // ★ 변경 (교재: org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest)
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

    // Store the saved entity in two separate entity objects
    ReviewEntity entity1 = repository.findById(savedEntity.getId()).get();
    ReviewEntity entity2 = repository.findById(savedEntity.getId()).get();

    // Update the entity using the first entity object
    entity1.setAuthor("a1");
    repository.save(entity1);

    // Update the entity using the second entity object.
    // This should fail since the second entity now holds an old version number, i.e. an Optimistic Lock Error
    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2);
    });

    // Get the updated entity from the database and verify its new sate
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

## 동작 원리
- **`@ServiceConnection`**: 컨테이너가 뜬 뒤 그 주소와 포트를 스프링이 읽어서 DB 연결 설정으로 쓴다. 그래서 테스트용 yml에 DB 주소를 적지 않아도 된다.
- **`static { database.start(); }`**: 테스트 클래스마다 컨테이너를 새로 띄우면 느리므로, 베이스 클래스에서 한 번만 띄워 여러 테스트가 같이 쓴다.
- **`@DataMongoTest` / `@DataJpaTest`**: 웹 계층 없이 **DB 관련 빈만** 띄운다. 그래서 빠르다.
- review의 `@Transactional(propagation = NOT_SUPPORTED)`: `@DataJpaTest`는 기본으로 테스트마다 트랜잭션을 걸고 끝나면 롤백한다. 이러면 중복 키, 낙관적 잠금 오류가 DB까지 가지 않아 확인이 안 되므로 트랜잭션을 끈다.

| 테스트 | 확인하는 것 |
|---|---|
| `create` | 저장하고 다시 읽으면 같은 값 |
| `update` | 고쳐서 저장하면 version이 1이 된다 |
| `delete` | 지우면 없어진다 |
| `getByProductId` | `findByProductId`가 동작한다 |
| `duplicateError` | 같은 productId(또는 productId+id 조합)를 또 넣으면 오류 |
| `optimisticLockError` | 옛날 version으로 저장하면 오류 (02의 `@Version`) |
| `paging` (product만) | 4개씩 나눠 읽기 |

## 교재와 다른 점
| 바뀐 것 | 이유 |
|---|---|
| `@DataMongoTest`, `@DataJpaTest` 패키지 | Boot 4가 테스트 자동 설정을 기술별 모듈(`spring-boot-data-mongodb-test`, `spring-boot-data-jpa-test`)로 옮겼다. 01에서 넣은 테스트 스타터 안에 있다 |
| `MongoDBContainer`, `MySQLContainer` 패키지 | Testcontainers 2.0에서 `org.testcontainers.mongodb`, `org.testcontainers.mysql`로 옮겨졌다. 옛 위치(`...containers...`)도 남아 있지만 **deprecated**라서 새 위치를 쓴다 |

확인 방법(클래스가 어느 jar의 어느 패키지에 있는지):
```bash
find ~/.gradle/caches -name "spring-boot-data-mongodb-test-4.0.9.jar" | head -1 | xargs unzip -l | grep DataMongoTest.class
# org/springframework/boot/data/mongodb/test/autoconfigure/DataMongoTest.class
find ~/.gradle/caches -name "testcontainers-mongodb-2.0.5.jar" | head -1 | xargs unzip -l | grep MongoDBContainer.class
# org/testcontainers/containers/MongoDBContainer.class   ← 옛 위치 (deprecated)
# org/testcontainers/mongodb/MongoDBContainer.class      ← 새 위치
```

## 실행
Docker Desktop이 켜져 있는지 먼저 확인한다(`docker ps`가 오류 없이 나오면 된다).
```bash
./gradlew :microservices:product-service:test --tests PersistenceTests
./gradlew :microservices:recommendation-service:test --tests PersistenceTests
./gradlew :microservices:review-service:test --tests PersistenceTests
```
- `--tests PersistenceTests`: 이번에 만든 테스트만 돌린다. 기존 `...ApplicationTests`는 05에서 새로 바꾼다.
- 처음 실행은 `mongo:8.0.5`, `mysql:9.2.0` 이미지를 내려받아서 오래 걸린다. MySQL은 뜨는 데 특히 오래 걸린다(그래서 `withStartupTimeoutSeconds(300)`).
- 테스트가 도는 동안 다른 터미널에서 `docker ps`를 치면 테스트용 컨테이너(그리고 `testcontainers/ryuk` 청소용 컨테이너)가 보인다. 끝나면 사라진다.
- `BUILD SUCCESSFUL`이면 통과. 결과 보고서: `microservices/<서비스>/build/reports/tests/test/index.html`
