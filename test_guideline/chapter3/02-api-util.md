# 02. `api`, `util` 라이브러리

> 시작: 교수님 `빈 폴더 + spring init 결과(01 문서)` 상태 / 결과: 교수님 `Chapter03/2-basic-rest-services`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

두 라이브러리 프로젝트를 새로 만든다. 실행되는 앱이 아니라 다른 서비스가 가져다 쓰는 코드다.

| 프로젝트 | 들어 있는 것 |
|---|---|
| `api` | 서비스끼리 주고받는 **API 인터페이스**(`ProductService` 등)와 데이터 클래스(`Product` 등), 예외 클래스(`NotFoundException`, `InvalidInputException`) |
| `util` | 공통 코드: `ServiceUtil`(응답에 넣을 서비스 주소), `GlobalControllerExceptionHandler`(예외 → HTTP 404/422), `HttpErrorInfo`(오류 응답 모양) |

데이터 클래스는 기본 생성자 + getter/setter로 쓰고 필드에 `final`을 붙이지 않는다. Jackson 3(Boot 4)는 `final` 필드를 JSON에서 채우지 못하기 때문이다(교수님 코드의 방식).

## 2-1. `api/build.gradle` (새 파일)
```bash
mkdir -p api
code api/build.gradle
```
붙여넣기 (새 파일)
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.api'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ext {
    springBootVersion = '4.0.9'
}

dependencies {
    implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")

    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-2. `api/settings.gradle` (새 파일)
```bash
code api/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'api'
```

## 2-3. `api/.gitignore` (새 파일)
```bash
code api/.gitignore
```
붙여넣기 (새 파일)
```
.gradle
/build/
!gradle/wrapper/gradle-wrapper.jar

.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

.idea
*.iws
*.iml
*.ipr
/out/

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
```

## 2-4. `api/src/main/java/se/magnus/api/composite/product/ProductAggregate.java` (새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/composite/product
code api/src/main/java/se/magnus/api/composite/product/ProductAggregate.java
```
붙여넣기 (새 파일)
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

## 2-5. `api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.composite.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface ProductCompositeService {

  @GetMapping(
    value = "/product-composite/{productId}",
    produces = "application/json")
  ProductAggregate getProduct(@PathVariable int productId);
}
```

## 2-6. `api/src/main/java/se/magnus/api/composite/product/RecommendationSummary.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/composite/product/RecommendationSummary.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.composite.product;

public class RecommendationSummary {

  private int recommendationId;
  private String author;
  private int rate;

  public RecommendationSummary(int recommendationId, String author, int rate) {
    this.recommendationId = recommendationId;
    this.author = author;
    this.rate = rate;
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
}
```

## 2-7. `api/src/main/java/se/magnus/api/composite/product/ReviewSummary.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/composite/product/ReviewSummary.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.composite.product;

public class ReviewSummary {

  private int reviewId;
  private String author;
  private String subject;

  public ReviewSummary(int reviewId, String author, String subject) {
    this.reviewId = reviewId;
    this.author = author;
    this.subject = subject;
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
}
```

## 2-8. `api/src/main/java/se/magnus/api/composite/product/ServiceAddresses.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/composite/product/ServiceAddresses.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.composite.product;

public class ServiceAddresses {
  private String cmp;
  private String pro;
  private String rev;
  private String rec;

  public ServiceAddresses() {
    cmp = null;
    pro = null;
    rev = null;
    rec = null;
  }

  public ServiceAddresses(
    String compositeAddress,
    String productAddress,
    String reviewAddress,
    String recommendationAddress) {

    this.cmp = compositeAddress;
    this.pro = productAddress;
    this.rev = reviewAddress;
    this.rec = recommendationAddress;
  }

  public String getCmp() {
    return cmp;
  }

  public String getPro() {
    return pro;
  }

  public String getRev() {
    return rev;
  }

  public String getRec() {
    return rec;
  }
}
```

## 2-9. `api/src/main/java/se/magnus/api/core/product/Product.java` (새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/core/product
code api/src/main/java/se/magnus/api/core/product/Product.java
```
붙여넣기 (새 파일)
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
}
```

## 2-10. `api/src/main/java/se/magnus/api/core/product/ProductService.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/core/product/ProductService.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface ProductService {

  @GetMapping(
    value = "/product/{productId}",
    produces = "application/json")
  Product getProduct(@PathVariable int productId);
}
```

## 2-11. `api/src/main/java/se/magnus/api/core/recommendation/Recommendation.java` (새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/core/recommendation
code api/src/main/java/se/magnus/api/core/recommendation/Recommendation.java
```
붙여넣기 (새 파일)
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
}
```

## 2-12. `api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.core.recommendation;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface RecommendationService {

  @GetMapping(
    value = "/recommendation",
    produces = "application/json")
  List<Recommendation> getRecommendations(
    @RequestParam(value = "productId", required = true) int productId);
}
```

## 2-13. `api/src/main/java/se/magnus/api/core/review/Review.java` (새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/core/review
code api/src/main/java/se/magnus/api/core/review/Review.java
```
붙여넣기 (새 파일)
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
}
```

## 2-14. `api/src/main/java/se/magnus/api/core/review/ReviewService.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.core.review;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface ReviewService {

  @GetMapping(
    value = "/review",
    produces = "application/json")
  List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
}
```

## 2-15. `api/src/main/java/se/magnus/api/exceptions/InvalidInputException.java` (새 파일)
```bash
mkdir -p api/src/main/java/se/magnus/api/exceptions
code api/src/main/java/se/magnus/api/exceptions/InvalidInputException.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.exceptions;

public class InvalidInputException extends RuntimeException {
  public InvalidInputException() {}

  public InvalidInputException(String message) {
    super(message);
  }

  public InvalidInputException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidInputException(Throwable cause) {
    super(cause);
  }
}
```

## 2-16. `api/src/main/java/se/magnus/api/exceptions/NotFoundException.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/exceptions/NotFoundException.java
```
붙여넣기 (새 파일)
```java
package se.magnus.api.exceptions;

public class NotFoundException extends RuntimeException {
  public NotFoundException() {}

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotFoundException(Throwable cause) {
    super(cause);
  }
}
```

## 2-17. `util/build.gradle` (새 파일)
```bash
mkdir -p util
code util/build.gradle
```
붙여넣기 (새 파일)
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.util'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ext {
    springBootVersion = '4.0.9'
}

dependencies {
    implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")

    implementation project(':api')
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2-18. `util/settings.gradle` (새 파일)
```bash
code util/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'util'
```

## 2-19. `util/.gitignore` (새 파일)
```bash
code util/.gitignore
```
붙여넣기 (새 파일)
```
.gradle
/build/
!gradle/wrapper/gradle-wrapper.jar

.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

.idea
*.iws
*.iml
*.ipr
/out/

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
```

## 2-20. `util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java` (새 파일)
```bash
mkdir -p util/src/main/java/se/magnus/util/http
code util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java
```
붙여넣기 (새 파일)
```java
package se.magnus.util.http;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;

@RestControllerAdvice
class GlobalControllerExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public @ResponseBody HttpErrorInfo handleNotFoundExceptions(
    ServerHttpRequest request, NotFoundException ex) {

    return createHttpErrorInfo(NOT_FOUND, request, ex);
  }

  @ResponseStatus(UNPROCESSABLE_CONTENT)
  @ExceptionHandler(InvalidInputException.class)
  public @ResponseBody HttpErrorInfo handleInvalidInputException(
    ServerHttpRequest request, InvalidInputException ex) {

    return createHttpErrorInfo(UNPROCESSABLE_CONTENT, request, ex);
  }

  private HttpErrorInfo createHttpErrorInfo(
    HttpStatus httpStatus, ServerHttpRequest request, Exception ex) {

    final String path = request.getPath().pathWithinApplication().value();
    final String message = ex.getMessage();

    LOG.debug("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message);
    return new HttpErrorInfo(httpStatus, path, message);
  }
}
```

422는 `HttpStatus.UNPROCESSABLE_CONTENT`(교재: `UNPROCESSABLE_ENTITY`, Spring 7에서 이름 바뀜).

## 2-21. `util/src/main/java/se/magnus/util/http/HttpErrorInfo.java` (새 파일)
```bash
code util/src/main/java/se/magnus/util/http/HttpErrorInfo.java
```
붙여넣기 (새 파일)
```java
package se.magnus.util.http;

import java.time.ZonedDateTime;
import org.springframework.http.HttpStatus;

public class HttpErrorInfo {
  private ZonedDateTime timestamp;
  private String path;
  private HttpStatus httpStatus;
  private String message;

  public HttpErrorInfo() {
    timestamp = null;
    this.httpStatus = null;
    this.path = null;
    this.message = null;
  }

  public HttpErrorInfo(HttpStatus httpStatus, String path, String message) {
    timestamp = ZonedDateTime.now();
    this.httpStatus = httpStatus;
    this.path = path;
    this.message = message;
  }

  public ZonedDateTime getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public int getStatus() {
    return httpStatus.value();
  }

  public String getError() {
    return httpStatus.getReasonPhrase();
  }

  public String getMessage() {
    return message;
  }
}
```

## 2-22. `util/src/main/java/se/magnus/util/http/ServiceUtil.java` (새 파일)
```bash
code util/src/main/java/se/magnus/util/http/ServiceUtil.java
```
붙여넣기 (새 파일)
```java
package se.magnus.util.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);

  private final String port;

  private String serviceAddress = null;

  public ServiceUtil(@Value("${server.port}") String port) {

    this.port = port;
  }

  public String getServiceAddress() {
    if (serviceAddress == null) {
      serviceAddress = findMyHostname() + "/" + findMyIpAddress() + ":" + port;
    }
    return serviceAddress;
  }

  private String findMyHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown host name";
    }
  }

  private String findMyIpAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "unknown IP address";
    }
  }
}
```

다음: [03-core-services.md](03-core-services.md)
