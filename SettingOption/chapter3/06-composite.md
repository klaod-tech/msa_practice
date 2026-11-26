# 06. 복합 서비스 (product-composite-service)

> 표기: 한 줄만 바뀌면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`. 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 지우고 바꿀 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(`★ 지움:` 줄을 지운다). `★ 참고:` = 교재와 다른 이유. 표시 없는 줄 = 이미 있는 줄. ★ 주석은 입력하지 않아도 된다.

## 구조
```
curl localhost:7001/product-composite/1
      │
      ▼
 composite (7001) ──→ product (7002)
                  ──→ recommendation (7003)
                  ──→ review (7004)
```
composite는 받은 요청을 처리하려고 나머지 세 서비스를 **실제로 HTTP로 호출**해서 결과를 합친다.
```
product-composite-service/src/main/java/se/magnus/microservices/composite/product/
├── ProductCompositeServiceApplication.java    @ComponentScan + RestTemplate 빈
└── services/
    ├── ProductCompositeIntegration.java       핵심 서비스 3개를 호출하는 통합 컴포넌트
    └── ProductCompositeServiceImpl.java       /product-composite/{productId} REST 컨트롤러
```

## 0. 사전 확인: api 쪽 (02에서 만듦)
- `api/.../composite/product/`의 5개: `ProductCompositeService`(인터페이스, 교재 그대로), `ProductAggregate`·`RecommendationSummary`·`ReviewSummary`·`ServiceAddresses`(롬복)

---

## 1. `microservices/product-composite-service/build.gradle` 수정
```bash
code microservices/product-composite-service/build.gradle
```
**두 군데 추가**
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar { enabled = false }  // ★ 추가
group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	// ★ 추가 시작 (2줄)
	implementation project(':api')
	implementation project(':util')
	// ★ 추가 끝
}

tasks.named('test') {
	useJUnitPlatform()
}
```
처음에 `jar { enabled = false }`를 `dependencies { }` **안에** 넣었었다. 동작은 했지만 위치가 틀렸으므로 블록 밖으로 옮겼다.

## 2. 메인 클래스 `ProductCompositeServiceApplication.java` 수정
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java
```
**import 6줄, 애너테이션 1줄, RestTemplate 빈 메서드 1개 추가**
빈 메서드는 `main()` 메서드 **아래, 클래스를 닫는 `}` 위**에 넣는다.
```java
package se.magnus.microservices.composite.product;

import java.util.List;  // ★ 추가 — Boot 4 대응

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// ★ 추가 시작 (4줄)
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;  // ★ 참고: Boot 4 대응
import org.springframework.web.client.RestTemplate;
// ★ 추가 끝

// ★ 추가 시작 (2줄) — Boot 4 대응
import tools.jackson.databind.json.JsonMapper;

@ComponentScan("se.magnus")
// ★ 추가 끝
@SpringBootApplication
public class ProductCompositeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}

	// ★ 추가 시작 (3줄)
	@Bean
	RestTemplate restTemplate(JsonMapper jsonMapper) {  // ★ 참고: 교재: restTemplate()
		return new RestTemplate(List.of(new JacksonJsonHttpMessageConverter(jsonMapper)));  // ★ 참고: 교재: return new RestTemplate();
	// ★ 추가 끝
	}
}  // ★ 추가
```
교재는 `RestTemplate restTemplate() { return new RestTemplate(); }`이다. Boot 4에서 이대로 쓰면 응답 값이 전부 null/0이 된다 → 아래 [5. final 필드 역직렬화 문제](#5-final-필드-역직렬화-문제-boot-4에서만-발생) 참고.

## 3. `application.properties` 삭제 → `application.yml` (새 파일, 전체)
교재 '프로퍼티' 절의 `server.port: 7001`로 시작하는 yml은 product-service가 아니라 **composite용**이다. 핵심 서비스 주소를 코드에 하드코딩하지 않고 설정에서 읽는다.
```bash
rm microservices/product-composite-service/src/main/resources/application.properties
```
```bash
code microservices/product-composite-service/src/main/resources/application.yml
```
아래 내용 전체 입력
```yaml
server.port: 7001
spring.web.error.include-message: always                       # ★ 교재와 다름 (교재: server.error.include-message)
spring.jackson.mapper.allow-final-fields-as-mutators: true     # ★ 교재에 없음 (Boot 4 대응, 5번 참고)

app:
  product-service:
    host: localhost
    port: 7002
  recommendation-service:
    host: localhost
    port: 7003
  review-service:
    host: localhost
    port: 7004

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
`app:` 아래는 교재가 직접 정한 이름이라 VS Code가 "모르는 속성"으로 밑줄을 긋지만 정상이다. Integration이 `@Value("${app.product-service.host}")`로 읽는다.

## 4. `services/ProductCompositeIntegration.java` (새 파일, 교재 소스 + Boot 4 수정)
```bash
mkdir -p microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services
```
### `product-composite-service/.../services/ProductCompositeIntegration.java` (전체)
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java
```
아래 내용 전체 입력

```java
package se.magnus.microservices.composite.product.services;

import static org.springframework.http.HttpMethod.GET;

import tools.jackson.core.JacksonException;              // ★ 참고: 교재: import java.io.IOException;
import tools.jackson.databind.ObjectMapper;              // ★ 참고: 교재: com.fasterxml.jackson.databind.ObjectMapper
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    public ProductCompositeIntegration(
        RestTemplate restTemplate,
        ObjectMapper mapper,
        @Value("${app.product-service.host}") String productServiceHost,
        @Value("${app.product-service.port}") int productServicePort,
        @Value("${app.recommendation-service.host}") String recommendationServiceHost,
        @Value("${app.recommendation-service.port}") int recommendationServicePort,
        @Value("${app.review-service.host}") String reviewServiceHost,
        @Value("${app.review-service.port}") int reviewServicePort) {

        this.restTemplate = restTemplate;
        this.mapper = mapper;

        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation?productId=";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }

    public Product getProduct(int productId) {

        try {
            String url = productServiceUrl + productId;
            LOG.debug("Will call getProduct API on URL: {}", url);

            Product product = restTemplate.getForObject(url, Product.class);
            LOG.debug("Found a product with id: {}", product.getProductId());

            return product;

        } catch (HttpClientErrorException ex) {

            switch (HttpStatus.resolve(ex.getStatusCode().value())) {
                case NOT_FOUND:
                    throw new NotFoundException(getErrorMessage(ex));

                case UNPROCESSABLE_CONTENT:                      // ★ 참고: 교재: UNPROCESSABLE_ENTITY
                    throw new InvalidInputException(getErrorMessage(ex));

                default:
                    LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                    LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                    throw ex;
            }
        }
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (JacksonException ioex) {                    // ★ 참고: 교재: IOException
            return ex.getMessage();
        }
    }

    public List<Recommendation> getRecommendations(int productId) {

        try {
            String url = recommendationServiceUrl + productId;

            LOG.debug("Will call getRecommendations API on URL: {}", url);
            List<Recommendation> recommendations = restTemplate
                .exchange(url, GET, null, new ParameterizedTypeReference<List<Recommendation>>() {})
                .getBody();

            LOG.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
            return recommendations;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Review> getReviews(int productId) {

        try {
            String url = reviewServiceUrl + productId;

            LOG.debug("Will call getReviews API on URL: {}", url);
            List<Review> reviews = restTemplate
                .exchange(url, GET, null, new ParameterizedTypeReference<List<Review>>() {})
                .getBody();

            LOG.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
            return reviews;

        } catch (Exception ex) {
            LOG.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }
}
```
`code`로 열린 탭은 반드시 `Ctrl+S`로 저장한다. 저장하지 않으면 디스크에 파일이 없어서, VS Code에 오류도 안 뜨고 Gradle도 이 파일을 모른다.

### 교재와 다른 세 곳 (`★ 참고` 줄)
Boot 4의 클래스패스에는 Jackson 3(`tools.jackson`)만 있고 Jackson 2(`com.fasterxml.jackson.databind`)는 없다.
- **import**: `com.fasterxml.jackson.databind.ObjectMapper`, `java.io.IOException` 대신 `tools.jackson.databind.ObjectMapper`, `tools.jackson.core.JacksonException`. `JacksonException` import를 두 번 넣은 적이 있다. 중복은 오류는 아니지만 하나만 둔다.
- **`switch`**: `UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` (Spring 7에서 ENTITY는 deprecated).
- **`getErrorMessage()`**: `catch (IOException ioex)` → `catch (JacksonException ioex)`. Jackson 3의 `readValue()`는 `IOException`을 던지지 않아서, 교재대로 두면 `exception IOException is never thrown in body of corresponding try statement` 컴파일 오류가 난다.

`RestTemplate`은 Spring 7에서도 deprecated가 아니라 그대로 쓴다(교재 7장에서 WebClient로 바꾼다).

## 5. final 필드 역직렬화 문제 (Boot 4에서만 발생)
### 증상
서비스 4개를 다 띄우고 호출했더니 값이 전부 비어서 나왔다.
```json
{"name": null, "productId": 0, "recommendations": [{"author": null, "rate": 0, ...}], "serviceAddresses": {"pro": null, ...}}
```
핵심 서비스를 직접 호출하면(`curl localhost:7002/product/1`) 값이 정상이었다. 즉 **composite가 받은 JSON을 객체로 바꾸는 단계**에서 값이 사라졌다.

### 원인
- Product, Recommendation, Review의 필드는 모두 `final`이고 setter가 없다(교재 원본도 같다).
- Jackson 2(교재)는 기본 생성자로 객체를 만든 뒤 **final 필드에 값을 강제로 넣었다**(`MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS` 기본값 `true`).
- **Jackson 3은 이 기본값이 `false`다**(jackson-databind 3.1.7의 `MapperFeature`에서 확인). 그래서 기본 생성자가 넣은 0/null이 그대로 남는다.
- 게다가 교재의 `new RestTemplate()`은 Spring Boot가 설정한 Jackson이 아니라 **자기만의 기본 Jackson**을 쓴다. 그래서 yml 설정만으로는 해결되지 않는다.
- 롬복 때문이 아니다. 교재 원본 코드를 그대로 써도 Boot 4에서는 똑같이 발생한다.

### 해결 = 위 2번 + 3번의 두 가지
- 2번 메인 클래스: `restTemplate(JsonMapper jsonMapper)` → RestTemplate이 Spring Boot의 JsonMapper를 쓴다.
- 3번 yml: `spring.jackson.mapper.allow-final-fields-as-mutators: true` → Spring Boot의 JsonMapper에 Jackson 2의 동작을 다시 켠다. Integration에 주입되는 ObjectMapper(오류 응답 `HttpErrorInfo`를 읽을 때 사용)에도 적용되므로, 404·422의 `message`도 composite까지 제대로 전달된다.

고친 뒤에는 composite만 다시 빌드·실행하면 된다. 옛 composite를 먼저 꺼야 한다(포트 충돌) → [07](07-test.md)

## 6. `services/ProductCompositeServiceImpl.java` (새 파일, 교재 소스 그대로)
### `product-composite-service/.../services/ProductCompositeServiceImpl.java` (전체)
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java
```
아래 내용 전체 입력

```java
package se.magnus.microservices.composite.product.services;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    public ProductCompositeServiceImpl(
        ServiceUtil serviceUtil, ProductCompositeIntegration integration) {

        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public ProductAggregate getProduct(int productId) {

        Product product = integration.getProduct(productId);
        if (product == null) {
            throw new NotFoundException("No product found for productId: " + productId);
        }

        List<Recommendation> recommendations = integration.getRecommendations(productId);

        List<Review> reviews = integration.getReviews(productId);

        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
    }

    private ProductAggregate createProductAggregate(
        Product product,
        List<Recommendation> recommendations,
        List<Review> reviews,
        String serviceAddress) {

        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        List<RecommendationSummary> recommendationSummaries =
            (recommendations == null) ? null : recommendations.stream()
                .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate()))
                .collect(Collectors.toList());

        List<ReviewSummary> reviewSummaries =
            (reviews == null) ? null : reviews.stream()
                .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject()))
                .collect(Collectors.toList());

        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
```
- product를 먼저 부르고, 없으면 `NotFoundException`(404). 이어서 추천·리뷰를 불러 `createProductAggregate()`로 합친다.
- `createProductAggregate()`: 상품 정보 → 추천 요약 목록 → 리뷰 요약 목록 → 4개 서비스 주소(`ServiceAddresses`) 순서로 만든다.
- api의 Recommendation·Review가 비어 있으면 이 파일에서 `getRecommendationId()`, `getReviewId()`에 `cannot find symbol` 오류가 난다.

## 확인
```bash
./gradlew :microservices:product-composite-service:compileJava     # BUILD SUCCESSFUL
```

다음: [07-test.md](07-test.md)
