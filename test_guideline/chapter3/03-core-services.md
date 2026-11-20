# 03. 핵심 서비스 (product, recommendation, review)

> 시작: 교수님 `빈 폴더 + spring init 결과(01 문서)` 상태 / 결과: 교수님 `Chapter03/2-basic-rest-services`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

핵심 서비스 3개에 REST API를 만든다. 아직 DB가 없어서 **고정된 데이터**를 돌려준다.
- `build.gradle`: `api`, `util` 프로젝트를 의존성으로 추가하고 `jar { enabled = false }`로 실행용 jar 하나만 만든다.
- `XxxServiceImpl`: `api`의 인터페이스를 구현하는 `@RestController`. productId가 13이면 404(없음), 1보다 작으면 422(잘못된 값).
- `application.yml`: 포트(product 7002, recommendation 7003, review 7004). spring init이 만든 `application.properties`는 지운다.
- 테스트: `WebTestClient`로 API를 불러 응답을 확인한다.

## 3-1. `microservices/product-service/build.gradle`
```bash
code microservices/product-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'  // ★ 변경 (전: id 'java')
    id 'io.spring.dependency-management' version '1.1.7'  // ★ 변경 (전: id 'org.springframework.boot' version '4.0.9')
    id 'java'  // ★ 변경 (전: id 'io.spring.dependency-management' version '1.1.7')
}

group = 'se.magnus.microservices.core.product'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {  // ★ 변경 (전: toolchain {)
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21))
    }  // ★ 변경 (전: })
}

repositories {
    // ★ 변경 (4줄, 전: 1줄)
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    // ★ 변경 (9줄, 전: 5줄)
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()  // ★ 변경 (전: useJUnitPlatform())
}
```

## 3-2. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/ProductServiceApplication.java`
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/ProductServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;  // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")  // ★ 추가
public class ProductServiceApplication {

  // ★ 변경 (3줄, 전: 3줄)
  public static void main(String[] args) {
    SpringApplication.run(ProductServiceApplication.class, args);
  }
}
```

## 3-3. `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java` (새 파일)
```bash
mkdir -p microservices/product-service/src/main/java/se/magnus/microservices/core/product/services
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.product.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

  private final ServiceUtil serviceUtil;

  public ProductServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Product getProduct(int productId) {
    LOG.debug("/product return the found product for productId={}", productId);

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    if (productId == 13) {
      throw new NotFoundException("No product found for productId: " + productId);
    }

    return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
  }
}
```

## 3-4. `microservices/product-service/src/main/resources/application.yml` (새 파일)
```bash
code microservices/product-service/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```

`spring.web.error.include-message: always`: 오류 응답에 메시지를 넣는다. 교재의 `server.error.include-message`는 Boot 4에서 이름이 바뀌어 **조용히 무시된다**.

## 3-5. `microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java`
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

// ★ 추가 (4줄)
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;  // ★ 추가
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 추가
import org.springframework.test.web.reactive.server.WebTestClient;  // ★ 추가

@AutoConfigureWebTestClient  // ★ 변경 (전: @SpringBootTest)
@SpringBootTest(webEnvironment = RANDOM_PORT)  // ★ 추가
class ProductServiceApplicationTests {

  @Autowired private WebTestClient client;  // ★ 변경 (전: @Test)
// ★ 삭제 (2줄, 첫 줄: void contextLoads() {)

  // ★ 추가 (50줄)
  @Test
  void getProductById() {

    int productId = 1;

    client.get()
      .uri("/product/" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void getProductInvalidParameterString() {

    client.get()
      .uri("/product/no-integer")
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(BAD_REQUEST)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/product/no-integer")
        .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getProductNotFound() {

    int productIdNotFound = 13;

    client.get()
      .uri("/product/" + productIdNotFound)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
        .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
  }

  @Test
  void getProductInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    client.get()
      .uri("/product/" + productIdInvalid)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
        .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }
}
```

## 3-6. `microservices/recommendation-service/build.gradle`
```bash
code microservices/recommendation-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'  // ★ 변경 (전: id 'java')
    id 'io.spring.dependency-management' version '1.1.7'  // ★ 변경 (전: id 'org.springframework.boot' version '4.0.9')
    id 'java'  // ★ 변경 (전: id 'io.spring.dependency-management' version '1.1.7')
}

group = 'se.magnus.microservices.core.recommendation'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {  // ★ 변경 (전: toolchain {)
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21))
    }  // ★ 변경 (전: })
}

repositories {
    // ★ 변경 (4줄, 전: 1줄)
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    // ★ 변경 (9줄, 전: 5줄)
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()  // ★ 변경 (전: useJUnitPlatform())
}
```

## 3-7. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;  // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")  // ★ 추가
public class RecommendationServiceApplication {

  // ★ 변경 (3줄, 전: 3줄)
  public static void main(String[] args) {
    SpringApplication.run(RecommendationServiceApplication.class, args);
  }
}
```

## 3-8. `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java` (새 파일)
```bash
mkdir -p microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.recommendation.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final ServiceUtil serviceUtil;

  public RecommendationServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<Recommendation> getRecommendations(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    if (productId == 113) {
      LOG.debug("No recommendations found for productId: {}", productId);
      return new ArrayList<>();
    }

    List<Recommendation> list = new ArrayList<>();
    list.add(new Recommendation(productId, 1, "Author 1", 1, "Content 1", serviceUtil.getServiceAddress()));
    list.add(new Recommendation(productId, 2, "Author 2", 2, "Content 2", serviceUtil.getServiceAddress()));
    list.add(new Recommendation(productId, 3, "Author 3", 3, "Content 3", serviceUtil.getServiceAddress()));

    LOG.debug("/recommendation response size: {}", list.size());

    return list;
  }
}
```

## 3-9. `microservices/recommendation-service/src/main/resources/application.yml` (새 파일)
```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```

## 3-10. `microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java`
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

// ★ 추가 (4줄)
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;  // ★ 추가
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 추가
import org.springframework.test.web.reactive.server.WebTestClient;  // ★ 추가

@AutoConfigureWebTestClient  // ★ 변경 (전: @SpringBootTest)
@SpringBootTest(webEnvironment = RANDOM_PORT)  // ★ 추가
class RecommendationServiceApplicationTests {

  @Autowired private WebTestClient client;  // ★ 변경 (전: @Test)
// ★ 삭제 (2줄, 첫 줄: void contextLoads() {)

  // ★ 추가 (62줄)
  @Test
  void getRecommendationsByProductId() {

    int productId = 1;

    client.get()
      .uri("/recommendation?productId=" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.length()").isEqualTo(3)
        .jsonPath("$[0].productId").isEqualTo(productId);
  }

  @Test
  void getRecommendationsMissingParameter() {

    client.get()
      .uri("/recommendation")
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(BAD_REQUEST)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getRecommendationsInvalidParameter() {

    client.get()
      .uri("/recommendation?productId=no-integer")
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(BAD_REQUEST)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getRecommendationsNotFound() {

    int productIdNotFound = 113;

    client.get()
      .uri("/recommendation?productId=" + productIdNotFound)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getRecommendationsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    client.get()
      .uri("/recommendation?productId=" + productIdInvalid)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/recommendation")
        .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }
}
```

## 3-11. `microservices/review-service/build.gradle`
```bash
code microservices/review-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'  // ★ 변경 (전: id 'java')
    id 'io.spring.dependency-management' version '1.1.7'  // ★ 변경 (전: id 'org.springframework.boot' version '4.0.9')
    id 'java'  // ★ 변경 (전: id 'io.spring.dependency-management' version '1.1.7')
}

group = 'se.magnus.microservices.core.review'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {  // ★ 변경 (전: toolchain {)
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21))
    }  // ★ 변경 (전: })
}

repositories {
    // ★ 변경 (4줄, 전: 1줄)
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    // ★ 변경 (9줄, 전: 5줄)
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()  // ★ 변경 (전: useJUnitPlatform())
}
```

## 3-12. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java`
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;  // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")  // ★ 추가
public class ReviewServiceApplication {

  // ★ 변경 (3줄, 전: 3줄)
  public static void main(String[] args) {
    SpringApplication.run(ReviewServiceApplication.class, args);
  }
}
```

## 3-13. `microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java` (새 파일)
```bash
mkdir -p microservices/review-service/src/main/java/se/magnus/microservices/core/review/services
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.core.review.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ReviewServiceImpl implements ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

  private final ServiceUtil serviceUtil;

  public ReviewServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<Review> getReviews(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }

    if (productId == 213) {
      LOG.debug("No reviews found for productId: {}", productId);
      return new ArrayList<>();
    }

    List<Review> list = new ArrayList<>();
    list.add(new Review(productId, 1, "Author 1", "Subject 1", "Content 1", serviceUtil.getServiceAddress()));
    list.add(new Review(productId, 2, "Author 2", "Subject 2", "Content 2", serviceUtil.getServiceAddress()));
    list.add(new Review(productId, 3, "Author 3", "Subject 3", "Content 3", serviceUtil.getServiceAddress()));

    LOG.debug("/reviews response size: {}", list.size());

    return list;
  }
}
```

## 3-14. `microservices/review-service/src/main/resources/application.yml` (새 파일)
```bash
code microservices/review-service/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```

## 3-15. `microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java`
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

// ★ 추가 (4줄)
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;  // ★ 추가
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 추가
import org.springframework.test.web.reactive.server.WebTestClient;  // ★ 추가

@AutoConfigureWebTestClient  // ★ 변경 (전: @SpringBootTest)
@SpringBootTest(webEnvironment = RANDOM_PORT)  // ★ 추가
class ReviewServiceApplicationTests {

  @Autowired private WebTestClient client;  // ★ 변경 (전: @Test)
// ★ 삭제 (2줄, 첫 줄: void contextLoads() {)

  // ★ 추가 (62줄)
  @Test
  void getReviewsByProductId() {

    int productId = 1;

    client.get()
      .uri("/review?productId=" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.length()").isEqualTo(3)
        .jsonPath("$[0].productId").isEqualTo(productId);
  }

  @Test
  void getReviewsMissingParameter() {

    client.get()
      .uri("/review")
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(BAD_REQUEST)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/review")
        .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getReviewsInvalidParameter() {

    client.get()
      .uri("/review?productId=no-integer")
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(BAD_REQUEST)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/review")
        .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getReviewsNotFound() {

    int productIdNotFound = 213;

    client.get()
      .uri("/review?productId=" + productIdNotFound)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getReviewsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    client.get()
      .uri("/review?productId=" + productIdInvalid)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody()
        .jsonPath("$.path").isEqualTo("/review")
        .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }
}
```

## 3-16. 지울 파일
```bash
cd ~/msa
rm microservices/product-service/.gitattributes
rm microservices/product-service/HELP.md
rm microservices/product-service/src/main/resources/application.properties
rm microservices/recommendation-service/.gitattributes
rm microservices/recommendation-service/HELP.md
rm microservices/recommendation-service/src/main/resources/application.properties
rm microservices/review-service/.gitattributes
rm microservices/review-service/HELP.md
rm microservices/review-service/src/main/resources/application.properties
```

다음: [04-composite.md](04-composite.md)
