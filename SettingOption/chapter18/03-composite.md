# 03. composite: 헤더 전달, B3 추적

> 시작: 17장까지 따라 한 상태 / 결과: 18장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

`getHeaders`로 요청 헤더 중 `X-group`만 골라 핵심 서비스 호출에 붙인다. 추적은 핵심 서비스와 같은 B3 설정.

## 3-1. `microservices/product-composite-service/build.gradle`
```bash
code microservices/product-composite-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    springCloudVersion = '2025.1.4'
    resilience4jVersion = '2.4.0'
}
repositories {
    mavenCentral()
}

jar {
    enabled = false
}

springBoot {
    buildInfo()
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-webclient'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    // ★ 변경 시작 (전 4줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    // ★ 전: implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'org.springframework.boot:spring-boot-starter-zipkin'
    // ★ 변경 끝
    implementation 'org.springframework.retry:spring-retry'
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
    implementation "io.github.resilience4j:resilience4j-spring-boot4:${resilience4jVersion}"
    implementation "io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}"
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        mavenBom "io.github.resilience4j:resilience4j-bom:${resilience4jVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 3-2. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/TracingConfig.java` (새 파일)
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/TracingConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationCustomizer;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TracingConfig {

  @Bean
  BaggagePropagation.FactoryBuilder myPropagationFactoryBuilder(
    ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
    Propagation.Factory delegate = B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.MULTI).build();
    BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
    baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
    return builder;
  }

}
```

## 3-3. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java`
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

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpHeaders;  // ★ 추가
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
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
import se.magnus.util.http.ServiceUtil;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private static final String PRODUCT_SERVICE_URL = "http://product";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
  private static final String REVIEW_SERVICE_URL = "http://review";

  private final Scheduler publishEventScheduler;
  private final WebClient webClient;
  private final ObjectMapper mapper;
  private final StreamBridge streamBridge;

  private final ServiceUtil serviceUtil;

  public ProductCompositeIntegration(
    @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
    WebClient webClient,
    ObjectMapper mapper,
    StreamBridge streamBridge,
    ServiceUtil serviceUtil
  ) {
    this.webClient = webClient;

    this.publishEventScheduler = publishEventScheduler;
    this.mapper = mapper;
    this.streamBridge = streamBridge;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Product> createProduct(Product body) {

    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  @Retry(name = "product")
  @TimeLimiter(name = "product")
  @CircuitBreaker(name = "product", fallbackMethod = "getProductFallbackValue")
  public Mono<Product> getProduct(HttpHeaders headers, int productId, int delay, int faultPercent) {  // ★ 변경 (전: public Mono<Product> getProduct(int productId, int delay, int fault...)

    URI url = UriComponentsBuilder.fromUriString(PRODUCT_SERVICE_URL
      + "/product/{productId}?delay={delay}&faultPercent={faultPercent}").build(productId, delay, faultPercent);
    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url)
      .headers(h -> h.addAll(headers))  // ★ 추가
      .retrieve().bodyToMono(Product.class).log(LOG.getName(), FINE)
      .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
  }

  private Mono<Product> getProductFallbackValue(HttpHeaders headers, int productId, int delay, int faultPercent, CallNotPermittedException ex) {  // ★ 변경 (전: private Mono<Product> getProductFallbackValue(int productId, int de...)

    LOG.warn("Creating a fail-fast fallback product for productId = {}, delay = {}, faultPercent = {} and exception = {} ",
      productId, delay, faultPercent, ex.toString());

    // ★ 추가 시작 (3줄)
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    // ★ 추가 끝

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
  public Flux<Recommendation> getRecommendations(HttpHeaders headers, int productId) {  // ★ 변경 (전: public Flux<Recommendation> getRecommendations(int productId) {)

    URI url = UriComponentsBuilder.fromUriString(RECOMMENDATION_SERVICE_URL + "/recommendation?productId={productId}").build(productId);

    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    return webClient.get().uri(url).headers(h -> h.addAll(headers)).retrieve().bodyToFlux(Recommendation.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());  // ★ 변경 (전: return webClient.get().uri(url).retrieve().bodyToFlux(Recommendatio...)
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
  public Flux<Review> getReviews(HttpHeaders headers, int productId) {  // ★ 변경 (전: public Flux<Review> getReviews(int productId) {)

    URI url = UriComponentsBuilder.fromUriString(REVIEW_SERVICE_URL + "/review?productId={productId}").build(productId);

    LOG.debug("Will call the getReviews API on URL: {}", url);

    return webClient.get().uri(url).headers(h -> h.addAll(headers)).retrieve().bodyToFlux(Review.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());  // ★ 변경 (전: return webClient.get().uri(url).retrieve().bodyToFlux(Review.class)...)
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

      case UNPROCESSABLE_CONTENT:
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
    } catch (JacksonException ioex) {
      return ex.getMessage();
    }
  }
}
```

## 3-4. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java`
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;  // ★ 추가
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
import se.magnus.microservices.composite.product.services.tracing.ObservationUtil;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

  private final SecurityContext nullSecCtx = new SecurityContextImpl();

  private final ServiceUtil serviceUtil;
  private final ObservationUtil observationUtil;
  private final ProductCompositeIntegration integration;

  public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ObservationUtil observationUtil, ProductCompositeIntegration integration) {
    this.serviceUtil = serviceUtil;
    this.observationUtil = observationUtil;
    this.integration = integration;
  }

  @Override
  public Mono<Void> createProduct(ProductAggregate body) {
    return observationWithProductInfo(body.getProductId(), () -> createProductInternal(body));
  }

  private Mono<Void> createProductInternal(ProductAggregate body) {
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
  // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: public Mono<ProductAggregate> getProduct(int productId, int delay, int faultPercent) {
  // ★ 전: return observationWithProductInfo(productId, () -> getProductInternal(productId, delay, faultPercent));
  public Mono<ProductAggregate> getProduct(HttpHeaders requestHeaders, int productId, int delay, int faultPercent) {
    return observationWithProductInfo(productId, () -> getProductInternal(requestHeaders, productId, delay, faultPercent));
  // ★ 변경 끝
  }

  @SuppressWarnings("unchecked")
  private Mono<ProductAggregate> getProductInternal(HttpHeaders requestHeaders, int productId, int delay, int faultPercent) {  // ★ 변경 (전: private Mono<ProductAggregate> getProductInternal(int productId, in...)
    LOG.info("Will get composite product info for product.id={}", productId);

    HttpHeaders headers = getHeaders(requestHeaders, "X-group");  // ★ 추가

    return Mono.zip(
        values -> createProductAggregate(
          (SecurityContext) values[0], (Product) values[1], (List<Recommendation>) values[2], (List<Review>) values[3], serviceUtil.getServiceAddress()),
        getSecurityContextMono(),
        // ★ 변경 시작 (전 3줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
        // ★ 전: integration.getProduct(productId, delay, faultPercent),
        // ★ 전: integration.getRecommendations(productId).collectList(),
        // ★ 전: integration.getReviews(productId).collectList())
        integration.getProduct(headers, productId, delay, faultPercent),
        integration.getRecommendations(headers, productId).collectList(),
        integration.getReviews(headers, productId).collectList())
        // ★ 변경 끝
      .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
      .log(LOG.getName(), FINE);
  }

  // ★ 추가 시작 (12줄)
  private HttpHeaders getHeaders(HttpHeaders requesthHeaders, String... headers) {
    LOG.trace("Will look for {} headers: {}", headers.length, headers);
    HttpHeaders h = new HttpHeaders();
    for (String header : headers) {
      List<String> value = requesthHeaders.get(header);
      if (value != null) {
        h.addAll(header, value);
      }
    }
    LOG.trace("Will transfer {}, headers: {}", h.size(), h);
    return h;
  }
  // ★ 추가 끝

  @Override
  public Mono<Void> deleteProduct(int productId) {
    return observationWithProductInfo(productId, () -> deleteProductInternal(productId));
  }

  private Mono<Void> deleteProductInternal(int productId) {
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

  private <T> T observationWithProductInfo(int productInfo, Supplier<T> supplier) {
    return observationUtil.observe(
      "composite observation",
      "product info",
      "productId",
      String.valueOf(productInfo),
      supplier);
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

## 3-5. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
// ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import static org.mockito.ArgumentMatchers.anyInt;
// ★ 전: import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.*;
// ★ 변경 끝
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
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

@AutoConfigureWebTestClient
@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true"})
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

    when(compositeIntegration.getProduct(any(), eq(PRODUCT_ID_OK), anyInt(), anyInt()))  // ★ 변경 (전: when(compositeIntegration.getProduct(eq(PRODUCT_ID_OK), anyInt(), a...)
      .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));

    when(compositeIntegration.getRecommendations(any(), eq(PRODUCT_ID_OK)))  // ★ 변경 (전: when(compositeIntegration.getRecommendations(PRODUCT_ID_OK)))
      .thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address"))));

    when(compositeIntegration.getReviews(any(), eq(PRODUCT_ID_OK)))  // ★ 변경 (전: when(compositeIntegration.getReviews(PRODUCT_ID_OK)))
      .thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));

    when(compositeIntegration.getProduct(any(), eq(PRODUCT_ID_NOT_FOUND), anyInt(), anyInt())).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));  // ★ 변경 (전: when(compositeIntegration.getProduct(eq(PRODUCT_ID_NOT_FOUND), anyI...)

    when(compositeIntegration.getProduct(any(), eq(PRODUCT_ID_INVALID), anyInt(), anyInt())).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));  // ★ 변경 (전: when(compositeIntegration.getProduct(eq(PRODUCT_ID_INVALID), anyInt...)
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

    getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_CONTENT)
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

다음: [04-spring-cloud.md](04-spring-cloud.md)
