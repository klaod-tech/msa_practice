# 02. composite에 Resilience4j 적용 (서킷 브레이커, 타임아웃, 재시도)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 12장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 13장과 다르게 쓰는 부분.
> 교재 "서킷 브레이커와 재시도 메커니즘 추가" 절.

## 무엇을 하나
composite가 product를 부르는 `getProduct` 하나에 세 가지를 건다.

| 애너테이션 | 설정 (config-repo) | 하는 일 |
|---|---|---|
| `@TimeLimiter(name = "product")` | `timeoutDuration: 2s` | 2초 안에 응답이 없으면 포기(TimeoutException) |
| `@Retry(name = "product")` | `maxAttempts: 3`, `waitDuration: 1000` | product가 **500**을 주면 1초 뒤 다시, 최대 3번 |
| `@CircuitBreaker(name = "product", fallbackMethod = ...)` | 아래 표 | 실패가 많으면 **회로를 열어** product를 아예 안 부르고 대체 응답 |

## 1. build.gradle
```bash
code microservices/product-composite-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar { enabled = false }
group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

ext {
	springCloudVersion = '2025.1.4'  // (교재: 2025.0.0)
	resilience4jVersion = '2.4.0'  // ★ 추가 (교재: 2.3.0)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	implementation project(':api')
	implementation project(':util')
	implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
	implementation 'org.springframework.boot:spring-boot-starter-webclient'
	implementation "io.github.resilience4j:resilience4j-spring-boot4:${resilience4jVersion}"  // ★ 추가 (교재: resilience4j-spring-boot3)
	implementation "io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}"  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
	  
}

dependencyManagement {
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
		mavenBom "io.github.resilience4j:resilience4j-bom:${resilience4jVersion}"  // ★ 추가
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

- **교재와 다름**: 교재는 `resilience4j-spring-boot3` 2.3.0. 이 모듈은 Boot 3용이라, Boot 4용으로 나온 **`resilience4j-spring-boot4`**(2.4.0부터 있음)를 쓴다.
- 교재는 `spring-boot-starter-aop`를 한 번 더 넣는데, 애너테이션이 동작하는 데 필요한 AOP는 12장에서 넣은 `spring-boot-starter-aspectj`가 이미 해 준다(Boot 4 이름). 다시 넣지 않는다.
- `resilience4j-bom`을 Spring Cloud BOM **다음에** 둔다. Spring Cloud도 Resilience4j 2.3.0을 관리하는데, 나중에 선언한 BOM(2.4.0)이 이긴다.

## 2. ProductCompositeIntegration
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import static java.util.logging.Level.FINE;
import static reactor.core.publisher.Flux.empty;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import tools.jackson.core.JacksonException;  // 교재와 다름 (교재: java.io.IOException, 3장부터)
import tools.jackson.databind.ObjectMapper;  // 교재와 다름 (교재: com.fasterxml.jackson.databind.ObjectMapper, 3장부터)
// ★ 추가 (import 5줄)
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;  // ★ 추가
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;
import se.magnus.util.http.ServiceUtil;  // ★ 추가

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final WebClient webClient;
  private final ObjectMapper mapper;

  private static final String PRODUCT_SERVICE_URL = "http://product";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
  private static final String REVIEW_SERVICE_URL = "http://review";

  private final StreamBridge streamBridge;

  private final Scheduler publishEventScheduler;

  private final ServiceUtil serviceUtil;  // ★ 추가

  public ProductCompositeIntegration(
    @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,

    WebClient.Builder webClientBuilder,
    ObjectMapper mapper,
    StreamBridge streamBridge,
    ServiceUtil serviceUtil  // ★ 추가
  ) {

    this.publishEventScheduler = publishEventScheduler;
    this.webClient = webClientBuilder.build();
    this.mapper = mapper;
    this.streamBridge = streamBridge;
    this.serviceUtil = serviceUtil;  // ★ 추가
  }

  @Override
  public Mono<Product> createProduct(Product body) {

    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  // ★ 변경 (getProduct 메서드 전체 + 아래 getProductFallbackValue 추가)
  @Override
  @Retry(name = "product")
  @TimeLimiter(name = "product")
  @CircuitBreaker(name = "product", fallbackMethod = "getProductFallbackValue")
  public Mono<Product> getProduct(int productId, int delay, int faultPercent) {

    URI url = UriComponentsBuilder.fromUriString(PRODUCT_SERVICE_URL
      + "/product/{productId}?delay={delay}&faultPercent={faultPercent}").build(productId, delay, faultPercent);
    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url)
      .retrieve().bodyToMono(Product.class).log(LOG.getName(), FINE)
      .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
  }

  private Mono<Product> getProductFallbackValue(int productId, int delay, int faultPercent, CallNotPermittedException ex) {

    LOG.warn("Creating a fail-fast fallback product for productId = {}, delay = {}, faultPercent = {} and exception = {} ",
      productId, delay, faultPercent, ex.toString());

    if (productId == 13) {
      String errMsg = "Product Id: " + productId + " not found in fallback cache!";
      LOG.warn(errMsg);
      throw new NotFoundException(errMsg);
    }

    return Mono.just(new Product(productId, "Fallback product" + productId, productId, serviceUtil.getServiceAddress()));
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {

    return Mono.fromRunnable(() -> sendMessage("products-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {

    return Mono.fromCallable(() -> {
      sendMessage("recommendations-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {

    URI url = UriComponentsBuilder.fromUriString(RECOMMENDATION_SERVICE_URL + "/recommendation?productId={productId}").build(productId);  // ★ 변경 (전: String url = ... + productId)

    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {

    return Mono.fromRunnable(() -> sendMessage("recommendations-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Mono<Review> createReview(Review body) {

    return Mono.fromCallable(() -> {
      sendMessage("reviews-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Flux<Review> getReviews(int productId) {

    URI url = UriComponentsBuilder.fromUriString(REVIEW_SERVICE_URL + "/review?productId={productId}").build(productId);  // ★ 변경 (전: String url = ... + productId)

    LOG.debug("Will call the getReviews API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {

    return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  private void sendMessage(String bindingName, Event event) {
    LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
    Message message = MessageBuilder.withPayload(event)
      .setHeader("partitionKey", event.getKey())
      .build();
    streamBridge.send(bindingName, message);
  }

  private Throwable handleException(Throwable ex) {

    if (!(ex instanceof WebClientResponseException)) {
      LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
      return ex;
    }

    WebClientResponseException wcre = (WebClientResponseException)ex;

    switch (HttpStatus.resolve(wcre.getStatusCode().value())) {

      case NOT_FOUND:
        return new NotFoundException(getErrorMessage(wcre));

      case UNPROCESSABLE_CONTENT:  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY, 3장부터)
        return new InvalidInputException(getErrorMessage(wcre));

      default:
        LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (JacksonException ioex) {  // 교재와 다름 (교재: IOException, 3장부터)
      return ex.getMessage();
    }
  }
}
```

- `getProductFallbackValue`: 회로가 **열려 있어서** 호출이 막혔을 때(`CallNotPermittedException`)만 불린다. 원래 메서드와 파라미터가 같고 마지막에 예외가 하나 더 붙어야 한다.
  - productId 13 → "캐시에도 없다"는 404
  - 그 외 → 이름이 `Fallback product<id>`인 가짜 상품(빠르게 대충이라도 응답, fail fast)
- URL을 문자열 더하기 대신 `UriComponentsBuilder`로 만든다(파라미터가 늘어서).
- `ServiceUtil`을 생성자로 받는다(대체 응답에 서비스 주소를 넣으려고).

## 3. ProductCompositeServiceImpl
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import static java.util.logging.Level.FINE;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

  private final SecurityContext nullSecCtx = new SecurityContextImpl();

  private final ServiceUtil serviceUtil;
  private final ProductCompositeIntegration integration;

  public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public Mono<Void> createProduct(ProductAggregate body) {

    try {

      List<Mono> monoList = new ArrayList<>();

      monoList.add(getLogAuthorizationInfoMono());

      LOG.info("Will create a new composite entity for product.id: {}", body.getProductId());

      Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
      monoList.add(integration.createProduct(product));

      if (body.getRecommendations() != null) {
        body.getRecommendations().forEach(r -> {
          Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
          monoList.add(integration.createRecommendation(recommendation));
        });
      }

      if (body.getReviews() != null) {
        body.getReviews().forEach(r -> {
          Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
          monoList.add(integration.createReview(review));
        });
      }

      LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

      return Mono.zip(r -> "", monoList.toArray(new Mono[0]))
        .doOnError(ex -> LOG.warn("createCompositeProduct failed: {}", ex.toString()))
        .then();

    } catch (RuntimeException re) {
      LOG.warn("createCompositeProduct failed: {}", re.toString());
      throw re;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Mono<ProductAggregate> getProduct(int productId, int delay, int faultPercent) {  // ★ 변경 (delay, faultPercent 추가)

    LOG.info("Will get composite product info for product.id={}", productId);
    return Mono.zip(
      values -> createProductAggregate(
        (SecurityContext) values[0], (Product) values[1], (List<Recommendation>) values[2], (List<Review>) values[3], serviceUtil.getServiceAddress()),
      getSecurityContextMono(),
      integration.getProduct(productId, delay, faultPercent),  // ★ 변경
      integration.getRecommendations(productId).collectList(),
      integration.getReviews(productId).collectList())
      .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
      .log(LOG.getName(), FINE);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    try {

      LOG.info("Will delete a product aggregate for product.id: {}", productId);

      return Mono.zip(r -> "",
        getLogAuthorizationInfoMono(),
        integration.deleteProduct(productId),
        integration.deleteRecommendations(productId),
        integration.deleteReviews(productId))
        .doOnError(ex -> LOG.warn("delete failed: {}", ex.toString()))
        .log(LOG.getName(), FINE).then();

    } catch (RuntimeException re) {
      LOG.warn("deleteCompositeProduct failed: {}", re.toString());
      throw re;
    }
  }

  private ProductAggregate createProductAggregate(
    SecurityContext sc, Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

    logAuthorizationInfo(sc);

    int productId = product.getProductId();
    String name = product.getName();
    int weight = product.getWeight();

    List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
       recommendations.stream()
        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
        .collect(Collectors.toList());

    List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
      reviews.stream()
        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
        .collect(Collectors.toList());

    String productAddress = product.getServiceAddress();
    String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
    String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

    return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
  }

  private Mono<SecurityContext> getLogAuthorizationInfoMono() {
    return getSecurityContextMono().doOnNext(sc -> logAuthorizationInfo(sc));
  }

  private Mono<SecurityContext> getSecurityContextMono() {
    return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSecCtx);
  }

  private void logAuthorizationInfo(SecurityContext sc) {
    if (sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
      Jwt jwtToken = ((JwtAuthenticationToken)sc.getAuthentication()).getToken();
      logAuthorizationInfo(jwtToken);
    } else {
      LOG.warn("No JWT based Authentication supplied, running tests are we?");
    }
  }

  private void logAuthorizationInfo(Jwt jwt) {
    if (jwt == null) {
      LOG.warn("No JWT supplied, running tests are we?");
    } else {
      if (LOG.isDebugEnabled()) {
        URL issuer = jwt.getIssuer();
        List<String> audience = jwt.getAudience();
        Object subject = jwt.getClaims().get("sub");
        Object scopes = jwt.getClaims().get("scope");
        Object expires = jwt.getClaims().get("exp");

        LOG.debug("Authorization info: Subject: {}, scopes: {}, expires {}: issuer: {}, audience: {}", subject, scopes, expires, issuer, audience);
      }
    }
  }
}
```

## 4. 테스트: ProductCompositeServiceApplicationTests
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyInt;  // ★ 추가
import static org.mockito.ArgumentMatchers.eq;  // ★ 추가
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true",
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"})
@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
class ProductCompositeServiceApplicationTests {

  private static final int PRODUCT_ID_OK = 1;
  private static final int PRODUCT_ID_NOT_FOUND = 2;
  private static final int PRODUCT_ID_INVALID = 3;

  @Autowired
  private WebTestClient client;

  @MockitoBean
  private ProductCompositeIntegration compositeIntegration;

  @BeforeEach
  void setUp() {

    when(compositeIntegration.getProduct(eq(PRODUCT_ID_OK), anyInt(), anyInt()))  // ★ 변경
      .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));

    when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address"))));

    when(compositeIntegration.getReviews(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));

    when(compositeIntegration.getProduct(eq(PRODUCT_ID_NOT_FOUND), anyInt(), anyInt())).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));  // ★ 변경

    when(compositeIntegration.getProduct(eq(PRODUCT_ID_INVALID), anyInt(), anyInt())).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));  // ★ 변경
  }

  @Test
  void contextLoads() {
  }

  @Test
  void getProductById() {

    getAndVerifyProduct(PRODUCT_ID_OK, OK)
      .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
      .jsonPath("$.recommendations.length()").isEqualTo(1)
      .jsonPath("$.reviews.length()").isEqualTo(1);
  }

  @Test
  void getProductNotFound() {

    getAndVerifyProduct(PRODUCT_ID_NOT_FOUND, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
      .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
  }

  @Test
  void getProductInvalidInput() {

    getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
      .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return client.get()
      .uri("/product-composite/" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }
}
```

`getProduct`의 파라미터가 3개가 됐으니 Mockito 설정도 "productId는 이 값, 나머지 두 개는 아무 int나"(`eq(...)`, `anyInt()`)로 바꾼다. Mockito는 한 파라미터라도 매처를 쓰면 나머지도 전부 매처여야 한다.

## 5. config-repo/product-composite.yml
```bash
code config-repo/product-composite.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
springdoc:
  swagger-ui.path: /openapi/swagger-ui.html
  api-docs.path: /openapi/v3/api-docs
  packagesToScan: se.magnus.microservices.composite.product
  pathsToMatch: /**
  cache.disabled: true

  swagger-ui:
    oauth2-redirect-url: /swagger-ui/oauth2-redirect.html
    oauth:
      clientId: writer
      clientSecret: '{cipher}debb77857798ba70441b229de88624213b0a00b284f95fe31d1378d117cf3ffa'
      useBasicAuthenticationWithAccessCodeGrant: true
      usePkceWithAuthorizationCodeGrant: true  # 교재와 다름 (교재에는 없음, 11장부터)
  oAuthFlow:
    authorizationUrl: https://localhost:8443/oauth2/authorize
    tokenUrl: https://localhost:8443/oauth2/token

server.forward-headers-strategy: framework

api:

  common:
    version: 1.0.0
    title: Sample API
    description: Description of the API...
    termsOfService: MY TERMS OF SERVICE
    license: MY LICENSE
    licenseUrl: MY LICENSE URL

    externalDocDesc: MY WIKI PAGE
    externalDocUrl: MY WIKI URL
    contact:
      name: NAME OF CONTACT
      url: URL TO CONTACT
      email: contact@mail.com

  responseCodes:
    ok.description: OK
    badRequest.description: Bad Request, invalid format of the request. See response message for more information
    notFound.description: Not found, the specified id does not exist
    unprocessableEntity.description: Unprocessable entity, input parameters caused the processing to fail. See response message for more information

  product-composite:

    get-composite-product:
      description: Returns a composite view of the specified product id
      notes: |
        If the requested product id is found the method will return information regarding:
        1. Product information
        1. Reviews
        1. Recommendations
        1. Service Addresses\n(technical information regarding the addresses of the microservices that created the response)

        1. If no product information is found, a **404 - Not Found** error will be returned
        1. In no recommendations or reviews are found for a product, a partial response will be returned

    create-composite-product:
      description: Creates a composite product
      notes: |
        The composite product information posted to the API will be split up and stored as separate product-info, recommendation and review entities.

        1. If a product with the same productId as specified in the posted information already exists,
           an **422 - Unprocessable Entity** error with a "duplicate key" error message will be returned

    delete-composite-product:
      description: Deletes a product composite
      notes: |
        Entities for product information, recommendations and reviews related to the specified productId will be deleted.
        The implementation of the delete method is idempotent, i.e. it can be called several times with the same response.

        This means that a delete request of a non-existing product will return **200 Ok**.

server.port: 7001
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message, 3장부터)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

spring.cloud.stream:
  default.contentType: application/json
  bindings:
    products-out-0:
      destination: products
      producer:
        required-groups: auditGroup
    recommendations-out-0:
      destination: recommendations
      producer:
        required-groups: auditGroup
    reviews-out-0:
      destination: reviews
      producer:
        required-groups: auditGroup

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}:9999

# ★ 추가 (resilience4j 설정 전체): 타임아웃, 재시도, 서킷 브레이커
resilience4j.timelimiter:
  instances:
    product:
      timeoutDuration: 2s

resilience4j.retry:
  instances:
    product:
      maxAttempts: 3
      waitDuration: 1000
      retryExceptions:
      - org.springframework.web.reactive.function.client.WebClientResponseException$InternalServerError

management.health.circuitbreakers.enabled: true

resilience4j.circuitbreaker:
  instances:
    product:
      allowHealthIndicatorToFail: false
      registerHealthIndicator: true
      slidingWindowType: COUNT_BASED
      slidingWindowSize: 5
      failureRateThreshold: 50
      waitDurationInOpenState: 10000
      permittedNumberOfCallsInHalfOpenState: 3
      automaticTransitionFromOpenToHalfOpenEnabled: true
      ignoreExceptions:
        - se.magnus.api.exceptions.InvalidInputException
        - se.magnus.api.exceptions.NotFoundException

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: TRACE

---
spring.config.activate.on-profile: docker

server.port: 8080

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.products-out-0.producer:
  partition-key-expression: headers['partitionKey']
  partition-count: 2

spring.cloud.stream.bindings.recommendations-out-0.producer:
  partition-key-expression: headers['partitionKey']
  partition-count: 2

spring.cloud.stream.bindings.reviews-out-0.producer:
  partition-key-expression: headers['partitionKey']
  partition-count: 2
```

서킷 브레이커 설정:

| 설정 | 값 | 뜻 |
|---|---|---|
| `slidingWindowType`, `slidingWindowSize` | `COUNT_BASED`, 5 | 최근 **5번** 호출을 보고 판단 |
| `failureRateThreshold` | 50 | 그중 **50% 이상** 실패하면 회로 **열림(OPEN)** |
| `waitDurationInOpenState` | 10000 | 열린 뒤 **10초** 지나면 반쯤 열림(HALF_OPEN) |
| `automaticTransitionFromOpenToHalfOpenEnabled` | true | 10초가 지나면 호출이 없어도 저절로 HALF_OPEN |
| `permittedNumberOfCallsInHalfOpenState` | 3 | HALF_OPEN에서 3번 시험 호출 → 괜찮으면 닫힘(CLOSED), 아니면 다시 OPEN |
| `ignoreExceptions` | InvalidInput, NotFound | 404/422는 product가 정상적으로 "없다/틀렸다"고 답한 것이라 **실패로 세지 않음** |
| `registerHealthIndicator` | true | `/actuator/health`에 회로 상태(CLOSED/OPEN/HALF_OPEN) 표시 |
| `allowHealthIndicatorToFail` | false | 회로가 열려도 composite health는 DOWN이 되지 않게 |

재시도 `retryExceptions`: product가 **500**(`InternalServerError`)을 줄 때만 다시 시도한다. 타임아웃이나 404에는 재시도하지 않는다.

## 확인 (도커 필요 없음)
```bash
cd ~/msa
./gradlew :api:build :microservices:product-composite-service:build
```
`BUILD SUCCESSFUL`(composite 테스트 8개). product 테스트는 Testcontainers라 도커가 필요하다.
```bash
./gradlew :microservices:product-service:build
```
`BUILD SUCCESSFUL`(13개).

다음: [03-docker-compose-and-test.md](03-docker-compose-and-test.md)
