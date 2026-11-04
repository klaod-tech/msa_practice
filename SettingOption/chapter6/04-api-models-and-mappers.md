# 04. API 모델 클래스 수정 + MapStruct 매퍼

> 표기: `★ 추가` / `★ 변경` = 5장 코드 대비 바뀌는 줄, `(교재: ...)` = 교재 6장과 다르게 쓰는 부분

## 왜 API 클래스를 고치나
이제 서비스가 **DB에서 읽은 엔티티 → API 객체**로 바꿔서 응답하고, 응답 직전에 `serviceAddress`를 채워 넣는다(`setServiceAddress`). 매퍼(MapStruct)도 빈 객체를 만든 뒤 setter로 값을 채운다. 그래서 `final`을 빼고 setter를 만든다.

교재는 setter 메서드를 직접 적지만, 우리는 Lombok을 쓰고 있으므로 **`@Setter` 한 줄**로 같은 setter가 만들어진다.

## 1. API 모델 3개
`code api/src/main/java/se/magnus/api/core/product/Product.java` 들어가서 아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;  // ★ 추가

@Getter
@Setter  // ★ 추가 (교재: setter 메서드를 직접 작성)
@AllArgsConstructor
@NoArgsConstructor(force = true)

public class Product {
    private int productId;  // ★ 변경: final 삭제
    private String name;  // ★ 변경: final 삭제
    private int weight;  // ★ 변경: final 삭제
    private String serviceAddress;  // ★ 변경: final 삭제
}
```

`code api/src/main/java/se/magnus/api/core/recommendation/Recommendation.java` 들어가서 아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.recommendation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;  // ★ 추가

@Getter
@Setter  // ★ 추가 (교재: setter 메서드를 직접 작성)
@AllArgsConstructor 
@NoArgsConstructor(force = true)

public class Recommendation {
  private int productId;  // ★ 변경: final 삭제
  private int recommendationId;  // ★ 변경: final 삭제
  private String author;  // ★ 변경: final 삭제
  private int rate;  // ★ 변경: final 삭제
  private String content;  // ★ 변경: final 삭제
  private String serviceAddress;  // ★ 변경: final 삭제
}
```

`code api/src/main/java/se/magnus/api/core/review/Review.java` 들어가서 아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;  // ★ 추가

@Getter
@Setter  // ★ 추가 (교재: setter 메서드를 직접 작성)
@AllArgsConstructor 
@NoArgsConstructor(force = true)

public class Review {
  private int productId;  // ★ 변경: final 삭제
  private int reviewId;  // ★ 변경: final 삭제
  private String author;  // ★ 변경: final 삭제
  private String subject;  // ★ 변경: final 삭제
  private String content;  // ★ 변경: final 삭제
  private String serviceAddress;  // ★ 변경: final 삭제
}
```

- `@NoArgsConstructor(force = true)`는 그대로 둬도 된다. `force`는 final 필드가 있을 때만 의미가 있어서, final이 빠진 지금은 영향이 없다.
- composite의 `ProductAggregate`, `RecommendationSummary`, `ReviewSummary`는 06에서 다룬다(setter가 필요 없다).

## 2. 매퍼 (새 파일 3개)
MapStruct는 인터페이스만 적으면 **컴파일할 때 구현 클래스(`ProductMapperImpl`)를 자동 생성**한다. 이름이 같은 필드는 알아서 복사하고, 다른 것만 `@Mapping`으로 알려 준다.

`code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductMapper.java` 들어가서 붙여넣기
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

`code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationMapper.java` 들어가서 붙여넣기
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

`code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewMapper.java` 들어가서 붙여넣기
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

| 매핑 | 이유 |
|---|---|
| `serviceAddress` ignore (엔티티 → API) | 엔티티에는 없는 값. 서비스에서 따로 채운다 |
| `id`, `version` ignore (API → 엔티티) | API에는 없는 값. DB가 채운다 |
| `rate` ↔ `rating` (recommendation만) | API는 `rate`, 엔티티는 `rating`으로 이름이 달라서 직접 연결 |
| `componentModel = "spring"` | 생성된 구현 클래스가 스프링 빈이 되어, 서비스에 생성자로 주입할 수 있다 |

## 3. 매퍼 테스트 (새 파일 3개)
`code microservices/product-service/src/test/java/se/magnus/microservices/core/product/MapperTests.java` 들어가서 붙여넣기
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

`code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/MapperTests.java` 들어가서 붙여넣기
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

`code microservices/review-service/src/test/java/se/magnus/microservices/core/review/MapperTests.java` 들어가서 붙여넣기
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

## 확인 (도커 필요 없음)
```bash
./gradlew :microservices:product-service:test --tests MapperTests
./gradlew :microservices:recommendation-service:test --tests MapperTests
./gradlew :microservices:review-service:test --tests MapperTests
```
`BUILD SUCCESSFUL`이면 된다. 자동 생성된 매퍼 코드도 직접 볼 수 있다:
```bash
cat microservices/recommendation-service/build/generated/sources/annotationProcessor/java/main/se/magnus/microservices/core/recommendation/services/RecommendationMapperImpl.java
```
`setRating( api.getRate() )` 같은 줄이 보이면 `@Mapping`이 적용된 것이다.
