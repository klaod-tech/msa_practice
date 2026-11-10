# 02. 영속성 계층: 엔티티와 리포지토리

> 새 파일 6개. 리포지토리는 교재 6장 그대로, **엔티티는 getter/setter를 Lombok으로 바꾼 것**만 교재와 다르다(필드, 애너테이션, 생성자는 교재 그대로).

각 서비스에 `persistence` 패키지를 만들고, **DB에 저장되는 모양(엔티티)** 과 **DB에 접근하는 인터페이스(리포지토리)** 를 둔다.
API 클래스(`Product` 등)를 그대로 저장하지 않고 엔티티를 따로 두는 이유는, DB 전용 필드(`id`, `version`)를 외부 API에 노출하지 않기 위해서다.

## 1. product-service (MongoDB)
```bash
mkdir -p microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductEntity.java
```
붙여넣기
```java
package se.magnus.microservices.core.product.persistence;

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
}
```

```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/persistence/ProductRepository.java
```
붙여넣기
```java
package se.magnus.microservices.core.product.persistence;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String>, CrudRepository<ProductEntity, String> {
  Optional<ProductEntity> findByProductId(int productId);
}
```

## 2. recommendation-service (MongoDB)
```bash
mkdir -p microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationEntity.java
```
붙여넣기
```java
package se.magnus.microservices.core.recommendation.persistence;

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
}
```

```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/persistence/RecommendationRepository.java
```
붙여넣기
```java
package se.magnus.microservices.core.recommendation.persistence;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface RecommendationRepository extends CrudRepository<RecommendationEntity, String> {
  List<RecommendationEntity> findByProductId(int productId);
}
```

## 3. review-service (MySQL, JPA)
```bash
mkdir -p microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewEntity.java
```
붙여넣기
```java
package se.magnus.microservices.core.review.persistence;

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
}
```

```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/persistence/ReviewRepository.java
```
붙여넣기
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

## 엔티티의 Lombok 규칙 (교재: getter/setter 직접 작성)
| 무엇 | 누가 만드나 |
|---|---|
| getter / setter | Lombok `@Getter @Setter` |
| 빈 생성자 (MapStruct, Spring Data, JPA가 사용) | Lombok `@NoArgsConstructor` |
| 값을 받는 생성자 (테스트가 사용) | **직접 작성** (`this.x = x`) |

- 값을 받는 생성자를 `@AllArgsConstructor`로 만들지 않는 이유: ① `@AllArgsConstructor`는 `id`, `version`까지 **모든 필드**를 받는 생성자가 되어, 테스트의 `new ProductEntity(1, "n", 1)` 같은 모양과 맞지 않는다. ② 필드를 적은 **순서대로** 매개변수가 정해져서, 같은 타입 필드(예: `author`, `subject`)의 순서를 바꾸면 컴파일 오류 없이 값이 뒤바뀐다. 직접 쓴 생성자는 순서가 코드에 그대로 보여서 이런 일이 없다.
- 생성자의 `this.`: 매개변수 이름이 필드 이름과 같아서, `this.productId`(필드)와 `productId`(매개변수)를 구분하려고 쓴다. Lombok이 만드는 setter 안에도 같은 `this.x = x`가 들어 있다(화면에 안 보일 뿐).
- api 클래스(`Product` 등)는 5장부터 `@AllArgsConstructor`를 쓰고 있다. 동작에는 문제가 없지만 **필드 순서는 바꾸지 않는다**(추가는 맨 뒤에).

## 애너테이션 정리

| 애너테이션 | MongoDB (product, recommendation) | JPA (review) |
|---|---|---|
| 저장 단위 | `@Document(collection = "products")` → 컬렉션 | `@Entity` + `@Table(name = "reviews")` → 테이블 |
| 기본 키 | `@Id String id` (MongoDB가 자동으로 만든다) | `@Id @GeneratedValue int id` (Hibernate가 번호를 매긴다) |
| 낙관적 잠금 | `@Version Integer version` | `@Version int version` |
| 중복 방지 | `@Indexed(unique = true)` productId / `@CompoundIndex` (productId + recommendationId) | `@Index(unique = true, columnList = "productId,reviewId")` |

- **`@Version` (낙관적 잠금)**: 저장할 때마다 version이 1씩 오른다. 두 사람이 같은 데이터를 읽고 각자 고쳐 저장하면, 늦게 저장한 쪽은 version이 옛날 값이라 실패한다(`OptimisticLockingFailureException`). 03의 `optimisticLockError` 테스트가 이것을 확인한다.
- **`productId`와 `id`는 다르다**: `productId`는 비즈니스 키(API로 주고받는 번호), `id`는 DB 내부 키다.

## 리포지토리 정리
- 인터페이스만 만들면 **Spring Data가 구현 클래스를 자동으로 만든다**. `save`, `findById`, `deleteAll` 등은 `CrudRepository`에 이미 있다.
- `findByProductId(int productId)`처럼 **메서드 이름 규칙**으로 쿼리가 만들어진다(`findBy` + 필드 이름).
- product만 `PagingAndSortingRepository`를 같이 상속한다(페이징 테스트용). Spring Data 3부터 이 인터페이스가 `CrudRepository`를 상속하지 않아서 둘 다 적는다.
- review의 `@Transactional(readOnly = true)`: JPA는 트랜잭션 안에서 동작하므로, 읽기 전용 트랜잭션을 지정해 둔다.

## 확인
```bash
./gradlew :microservices:product-service:compileJava :microservices:recommendation-service:compileJava :microservices:review-service:compileJava
```
`BUILD SUCCESSFUL`이면 된다.
