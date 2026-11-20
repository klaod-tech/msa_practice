# 01. api: 데이터 클래스, 생성·삭제 API

> 시작: 교수님 `Chapter05` 상태 / 결과: 교수님 `Chapter06`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- 핵심 서비스 인터페이스(`ProductService` 등)에 `create...`(POST), `delete...`(DELETE)가 추가된다.
- composite 인터페이스에도 생성(POST, 묶음 하나를 받아 세 서비스로 나눠 저장)과 삭제(DELETE)가 추가된다.
- 데이터 클래스(`Product` 등)에 setter가 추가된다. 매퍼(MapStruct)와 JSON 변환이 값을 넣을 때 쓴다.

## 1-1. `api/src/main/java/se/magnus/api/composite/product/ProductAggregate.java`
```bash
code api/src/main/java/se/magnus/api/composite/product/ProductAggregate.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.composite.product;

import java.util.List;

public class ProductAggregate {
  private int productId;
  private String name;
  private int weight;
  private List<RecommendationSummary> recommendations;
  private List<ReviewSummary> reviews;
  private ServiceAddresses serviceAddresses;
  // ★ 추가 (8줄)

  public ProductAggregate() {
    productId = 0;
    name = null;
    weight = 0;
    recommendations = null;
    reviews = null;
    serviceAddresses = null;
  }

  public ProductAggregate(
    int productId,
    String name,
    int weight,
    List<RecommendationSummary> recommendations,
    List<ReviewSummary> reviews,
    ServiceAddresses serviceAddresses) {

    this.productId = productId;
    this.name = name;
    this.weight = weight;
    this.recommendations = recommendations;
    this.reviews = reviews;
    this.serviceAddresses = serviceAddresses;
  }

  public int getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public int getWeight() {
    return weight;
  }

  public List<RecommendationSummary> getRecommendations() {
    return recommendations;
  }

  public List<ReviewSummary> getReviews() {
    return reviews;
  }

  public ServiceAddresses getServiceAddresses() {
    return serviceAddresses;
  }
}
```

## 1-2. `api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java`
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
import org.springframework.web.bind.annotation.*;  // ★ 변경 (전: import org.springframework.web.bind.annotation.GetMapping;)
// ★ 삭제: import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "ProductComposite", description = "REST API for composite product information.")
public interface ProductCompositeService {
  // ★ 추가 (11줄)

  @Operation(
    summary = "${api.product-composite.create-composite-product.description}",
    description = "${api.product-composite.create-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @PostMapping(
    value    = "/product-composite",
    consumes = "application/json")
  void createProduct(@RequestBody ProductAggregate body);

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
  ProductAggregate getProduct(@PathVariable int productId);
  // ★ 추가 (9줄)

  @Operation(
    summary = "${api.product-composite.delete-composite-product.description}",
    description = "${api.product-composite.delete-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @DeleteMapping(value = "/product-composite/{productId}")
  void deleteProduct(@PathVariable int productId);
}
```

## 1-3. `api/src/main/java/se/magnus/api/composite/product/RecommendationSummary.java`
```bash
code api/src/main/java/se/magnus/api/composite/product/RecommendationSummary.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.composite.product;

public class RecommendationSummary {

  private int recommendationId;
  private String author;
  private int rate;
  private String content;  // ★ 추가

  // ★ 변경 (7줄, 전: 1줄)
  public RecommendationSummary() {
    this.recommendationId = 0;
    this.author = null;
    this.rate = 0;
    this.content = null;
  }

  public RecommendationSummary(int recommendationId, String author, int rate, String content) {
    this.recommendationId = recommendationId;
    this.author = author;
    this.rate = rate;
    this.content = content;  // ★ 추가
  }

  public int getRecommendationId() {
    return recommendationId;
  }

  public String getAuthor() {
    return author;
  }

  public int getRate() {
    return rate;
  }

  public String getContent() {  // ★ 추가
    return content;  // ★ 추가
  }  // ★ 추가
}
```

## 1-4. `api/src/main/java/se/magnus/api/composite/product/ReviewSummary.java`
```bash
code api/src/main/java/se/magnus/api/composite/product/ReviewSummary.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.composite.product;

public class ReviewSummary {

  private int reviewId;
  private String author;
  private String subject;
  private String content;  // ★ 추가

  // ★ 변경 (7줄, 전: 1줄)
  public ReviewSummary() {
    this.reviewId = 0;
    this.author = null;
    this.subject = null;
    this.content = null;
  }

  public ReviewSummary(int reviewId, String author, String subject, String content) {
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;  // ★ 추가
  }

  public int getReviewId() {
    return reviewId;
  }

  public String getAuthor() {
    return author;
  }

  public String getSubject() {
    return subject;
  }

  public String getContent() {  // ★ 추가
    return content;  // ★ 추가
  }  // ★ 추가
}
```

## 1-5. `api/src/main/java/se/magnus/api/core/product/Product.java`
```bash
code api/src/main/java/se/magnus/api/core/product/Product.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.product;

public class Product {
  private int productId;
  private String name;
  private int weight;
  private String serviceAddress;

  public Product() {
    productId = 0;
    name = null;
    weight = 0;
    serviceAddress = null;
  }

  public Product(int productId, String name, int weight, String serviceAddress) {
    this.productId = productId;
    this.name = name;
    this.weight = weight;
    this.serviceAddress = serviceAddress;
  }

  public int getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public int getWeight() {
    return weight;
  }

  public String getServiceAddress() {
    return serviceAddress;
  }
  // ★ 추가 (12줄)

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public void setServiceAddress(String serviceAddress) {
    this.serviceAddress = serviceAddress;
  }
}
```

## 1-6. `api/src/main/java/se/magnus/api/core/product/ProductService.java`
```bash
code api/src/main/java/se/magnus/api/core/product/ProductService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.*;  // ★ 변경 (전: import org.springframework.web.bind.annotation.GetMapping;)
// ★ 삭제: import org.springframework.web.bind.annotation.PathVariable;

public interface ProductService {
  // ★ 추가 (5줄)

  @PostMapping(
    value    = "/product",
    consumes = "application/json",
    produces = "application/json")
  Product createProduct(@RequestBody Product body);

  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Product getProduct(@PathVariable int productId);

  @DeleteMapping(value = "/product/{productId}")  // ★ 추가
  void deleteProduct(@PathVariable int productId);  // ★ 추가
}
```

## 1-7. `api/src/main/java/se/magnus/api/core/recommendation/Recommendation.java`
```bash
code api/src/main/java/se/magnus/api/core/recommendation/Recommendation.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.recommendation;

public class Recommendation {
  private int productId;
  private int recommendationId;
  private String author;
  private int rate;
  private String content;
  private String serviceAddress;

  public Recommendation() {
    productId = 0;
    recommendationId = 0;
    author = null;
    rate = 0;
    content = null;
    serviceAddress = null;
  }

  public Recommendation(
    int productId,
    int recommendationId,
    String author,
    int rate,
    String content,
    String serviceAddress) {

    this.productId = productId;
    this.recommendationId = recommendationId;
    this.author = author;
    this.rate = rate;
    this.content = content;
    this.serviceAddress = serviceAddress;
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

  public int getRate() {
    return rate;
  }

  public String getContent() {
    return content;
  }

  public String getServiceAddress() {
    return serviceAddress;
  }
  // ★ 추가 (18줄)

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public void setRecommendationId(int recommendationId) {
    this.recommendationId = recommendationId;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setRate(int rate) {
    this.rate = rate;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setServiceAddress(String serviceAddress) {
    this.serviceAddress = serviceAddress;
  }
}
```

## 1-8. `api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java`
```bash
code api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.recommendation;

import java.util.List;
import org.springframework.web.bind.annotation.*;  // ★ 변경 (전: import org.springframework.web.bind.annotation.GetMapping;)
// ★ 삭제: import org.springframework.web.bind.annotation.RequestParam;

public interface RecommendationService {
  // ★ 추가 (5줄)

  @PostMapping(
    value    = "/recommendation",
    consumes = "application/json",
    produces = "application/json")
  Recommendation createRecommendation(@RequestBody Recommendation body);

  @GetMapping(
    value = "/recommendation",
    produces = "application/json")
  List<Recommendation> getRecommendations(
    @RequestParam(value = "productId", required = true) int productId);

  @DeleteMapping(value = "/recommendation")  // ★ 추가
  void deleteRecommendations(@RequestParam(value = "productId", required = true)  int productId);  // ★ 추가
}
```

## 1-9. `api/src/main/java/se/magnus/api/core/review/Review.java`
```bash
code api/src/main/java/se/magnus/api/core/review/Review.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

public class Review {
  private int productId;
  private int reviewId;
  private String author;
  private String subject;
  private String content;
  private String serviceAddress;

  public Review() {
    productId = 0;
    reviewId = 0;
    author = null;
    subject = null;
    content = null;
    serviceAddress = null;
  }

  public Review(
    int productId,
    int reviewId,
    String author,
    String subject,
    String content,
    String serviceAddress) {

    this.productId = productId;
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
    this.content = content;
    this.serviceAddress = serviceAddress;
  }

  public int getProductId() {
    return productId;
  }

  public int getReviewId() {
    return reviewId;
  }

  public String getAuthor() {
    return author;
  }

  public String getSubject() {
    return subject;
  }

  public String getContent() {
    return content;
  }

  public String getServiceAddress() {
    return serviceAddress;
  }
  // ★ 추가 (18줄)

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public void setReviewId(int reviewId) {
    this.reviewId = reviewId;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setServiceAddress(String serviceAddress) {
    this.serviceAddress = serviceAddress;
  }
}
```

## 1-10. `api/src/main/java/se/magnus/api/core/review/ReviewService.java`
```bash
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

import java.util.List;
import org.springframework.web.bind.annotation.*;  // ★ 변경 (전: import org.springframework.web.bind.annotation.GetMapping;)
// ★ 삭제: import org.springframework.web.bind.annotation.RequestParam;

public interface ReviewService {
  // ★ 추가 (5줄)

  @PostMapping(
      value    = "/review",
      consumes = "application/json",
      produces = "application/json")
  Review createReview(@RequestBody Review body);

  @GetMapping(
    value = "/review",
    produces = "application/json")
  List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

  @DeleteMapping(value = "/review")  // ★ 추가
  void deleteReviews(@RequestParam(value = "productId", required = true)  int productId);  // ★ 추가
}
```

다음: [02-core-services.md](02-core-services.md)
