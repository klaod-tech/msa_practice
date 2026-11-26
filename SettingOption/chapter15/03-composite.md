# 03. composite: 3부 기준으로 맞추기

> 시작: 14장까지 따라 한 상태 / 결과: 15장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

동작은 같고 3부 코드에 맞춰 정리한다. `WebClient` 빈 메서드의 `public`을 빼고, 테스트 로그 설정을 지우고, 테스트와 Dockerfile을 맞춘다.

## 3-1. `microservices/product-composite-service/build.gradle`
```bash
code microservices/product-composite-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'  // ★ 추가
}
// ★ 삭제: jar { enabled = false }

// ★ 삭제 시작 (3줄) — 아래 줄들을 지운다
// ★ 지움: springBoot {
// ★ 지움: buildInfo()
// ★ 지움: }
// ★ 삭제 끝
group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    springCloudVersion = '2025.1.4'  // ★ 변경 (전: springCloudVersion = '2025.1.4'  // (교재: 2025.0.0))
    resilience4jVersion = '2.4.0'
}
repositories {
    mavenCentral()
}

// ★ 추가 시작 (6줄)
jar {
    enabled = false
}

springBoot {
    buildInfo()
}
// ★ 추가 끝

dependencies {
    // ★ 추가 시작 (2줄)
    implementation project(':api')
    implementation project(':util')
    // ★ 추가 끝
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    // ★ 추가 시작 (2줄)
    implementation 'org.springframework.boot:spring-boot-starter-webclient'
    implementation 'org.springframework.boot:spring-boot-starter-restclient'
    // ★ 추가 끝
    implementation 'org.springframework.boot:spring-boot-starter-security'
    // ★ 변경 시작 (전 10줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 전: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 전: implementation project(':api')
    // ★ 전: implementation project(':util')
    // ★ 전: implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-starter-webclient'
    // ★ 전: implementation "io.github.resilience4j:resilience4j-spring-boot4:${resilience4jVersion}"
    // ★ 전: implementation "io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}"
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
    // ★ 변경 끝
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    // ★ 추가 시작 (7줄)
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
    implementation "io.github.resilience4j:resilience4j-spring-boot4:${resilience4jVersion}"
    implementation "io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}"
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    // ★ 추가 끝
    testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
    // ★ 추가 시작 (2줄)
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    // ★ 추가 끝
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        mavenBom "io.github.resilience4j:resilience4j-bom:${resilience4jVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
// ★ 삭제: testLogging.showStandardStreams = true
}
```

## 3-2. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java`
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductCompositeServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceApplication.class);

  @Value("${api.common.version}")         String apiVersion;
  @Value("${api.common.title}")           String apiTitle;
  @Value("${api.common.description}")     String apiDescription;
  @Value("${api.common.termsOfService}")  String apiTermsOfService;
  @Value("${api.common.license}")         String apiLicense;
  @Value("${api.common.licenseUrl}")      String apiLicenseUrl;
  @Value("${api.common.externalDocDesc}") String apiExternalDocDesc;
  @Value("${api.common.externalDocUrl}")  String apiExternalDocUrl;
  @Value("${api.common.contact.name}")    String apiContactName;
  @Value("${api.common.contact.url}")     String apiContactUrl;
  @Value("${api.common.contact.email}")   String apiContactEmail;

  @Bean
  public OpenAPI getOpenApiDocumentation() {
    return new OpenAPI()
      .info(new Info().title(apiTitle)
        .description(apiDescription)
        .version(apiVersion)
        .contact(new Contact()
          .name(apiContactName)
          .url(apiContactUrl)
          .email(apiContactEmail))
        .termsOfService(apiTermsOfService)
        .license(new License()
          .name(apiLicense)
          .url(apiLicenseUrl)))
      .externalDocs(new ExternalDocumentation()
        .description(apiExternalDocDesc)
        .url(apiExternalDocUrl));
  }

  private final Integer threadPoolSize;
  private final Integer taskQueueSize;

  public ProductCompositeServiceApplication(
    @Value("${app.threadPoolSize:10}") Integer threadPoolSize,
    @Value("${app.taskQueueSize:100}") Integer taskQueueSize
  ) {
    this.threadPoolSize = threadPoolSize;
    this.taskQueueSize = taskQueueSize;
  }

  @Bean
  public Scheduler publishEventScheduler() {
    LOG.info("Creates a messagingScheduler with connectionPoolSize = {}", threadPoolSize);
    return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool");
  }

  @Autowired
  private ReactorLoadBalancerExchangeFilterFunction lbFunction;

  @Bean
  WebClient webClient(WebClient.Builder builder) {  // ★ 변경 (전: public WebClient webClient(WebClient.Builder builder) {)
    return builder.filter(lbFunction).build();
  }

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(ProductCompositeServiceApplication.class, args);
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

// ★ 삭제 시작 (2줄) — 아래 줄들을 지운다
// ★ 지움: import tools.jackson.core.JacksonException;  // 교재와 다름 (교재: java.io.IOException, 3장부터)
// ★ 지움: import tools.jackson.databind.ObjectMapper;  // 교재와 다름 (교재: com.fasterxml.jackson.databind.ObjectMapper, 3장부터)
// ★ 삭제 끝
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
// ★ 추가 시작 (2줄)
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
// ★ 추가 끝

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  // ★ 삭제 시작 (2줄) — 아래 줄들을 지운다
  // ★ 지움: private final WebClient webClient;
  // ★ 지움: private final ObjectMapper mapper;
  // ★ 삭제 끝
  private static final String PRODUCT_SERVICE_URL = "http://product";
  private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
  private static final String REVIEW_SERVICE_URL = "http://review";

  // ★ 추가 시작 (3줄)
  private final Scheduler publishEventScheduler;
  private final WebClient webClient;
  private final ObjectMapper mapper;
  // ★ 추가 끝
  private final StreamBridge streamBridge;

  // ★ 삭제: private final Scheduler publishEventScheduler;
  private final ServiceUtil serviceUtil;

  public ProductCompositeIntegration(
    @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
    WebClient webClient,
    ObjectMapper mapper,
    StreamBridge streamBridge,
    ServiceUtil serviceUtil
  ) {
    this.webClient = webClient;  // ★ 추가

    this.publishEventScheduler = publishEventScheduler;
    // ★ 삭제: this.webClient = webClient;
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

    URI url = UriComponentsBuilder.fromUriString(RECOMMENDATION_SERVICE_URL + "/recommendation?productId={productId}").build(productId);

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

    URI url = UriComponentsBuilder.fromUriString(REVIEW_SERVICE_URL + "/review?productId={productId}").build(productId);

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

      case UNPROCESSABLE_CONTENT:  // ★ 변경 (전: case UNPROCESSABLE_CONTENT:  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY, 3...)
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
    } catch (JacksonException ioex) {  // ★ 변경 (전: } catch (JacksonException ioex) {  // 교재와 다름 (교재: IOException, 3장부터))
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
  public Mono<ProductAggregate> getProduct(int productId, int delay, int faultPercent) {
    return observationWithProductInfo(productId, () -> getProductInternal(productId, delay, faultPercent));
  }

  @SuppressWarnings("unchecked")
  private Mono<ProductAggregate> getProductInternal(int productId, int delay, int faultPercent) {
    LOG.info("Will get composite product info for product.id={}", productId);
    return Mono.zip(
        values -> createProductAggregate(
          (SecurityContext) values[0], (Product) values[1], (List<Recommendation>) values[2], (List<Review>) values[3], serviceUtil.getServiceAddress()),
        getSecurityContextMono(),
        integration.getProduct(productId, delay, faultPercent),
        integration.getRecommendations(productId).collectList(),
        integration.getReviews(productId).collectList())
      .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
      .log(LOG.getName(), FINE);
  }

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

## 3-5. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/tracing/ObservationRegistryConfig.java`
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/tracing/ObservationRegistryConfig.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services.tracing;

import io.micrometer.observation.ObservationRegistry;
// ★ 삭제: import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;  // 교재와 다름 (교재: org.springframework.boot.actuate.autoconfigure.observation, Boot 4)
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;  // ★ 추가
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ObservationRegistryConfig implements ObservationRegistryCustomizer<ObservationRegistry> {

  private final BuildProperties buildProperties;

  public ObservationRegistryConfig(BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Override
  public void customize(final ObservationRegistry registry) {
    registry.observationConfig().observationFilter(new BuildInfoObservationFilter(buildProperties));
  }
}
```

## 3-6. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEvent.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEvent.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

// ★ 삭제 시작 (4줄) — 아래 줄들을 지운다
// ★ 지움: import tools.jackson.core.JacksonException;  // 교재와 다름 (Jackson 3: tools.jackson.*, 교재: com.fasterxml.jackson.*)
// ★ 지움: import tools.jackson.core.type.TypeReference;
// ★ 지움: import tools.jackson.databind.JsonNode;
// ★ 지움: import tools.jackson.databind.ObjectMapper;
// ★ 삭제 끝
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.magnus.api.event.Event;
// ★ 추가 시작 (4줄)
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
// ★ 추가 끝

public class IsSameEvent extends TypeSafeMatcher<String> {

  private static final Logger LOG = LoggerFactory.getLogger(IsSameEvent.class);

  private ObjectMapper mapper = new ObjectMapper();

  private Event expectedEvent;

  private IsSameEvent(Event expectedEvent) {
    this.expectedEvent = expectedEvent;
  }

  @Override
  protected boolean matchesSafely(String eventAsJson) {

    if (expectedEvent == null) {
      return false;
    }

    LOG.trace("Convert the following json string to a map: {}", eventAsJson);
    Map mapEvent = convertJsonStringToMap(eventAsJson);
    mapEvent.remove("eventCreatedAt");

    Map mapExpectedEvent = getMapWithoutCreatedAt(expectedEvent);

    LOG.trace("Got the map: {}", mapEvent);
    LOG.trace("Compare to the expected map: {}", mapExpectedEvent);
    return mapEvent.equals(mapExpectedEvent);
  }

  @Override
  public void describeTo(Description description) {
    String expectedJson = convertObjectToJsonString(expectedEvent);
    description.appendText("expected to look like " + expectedJson);
  }

  public static Matcher<String> sameEventExceptCreatedAt(Event expectedEvent) {
    return new IsSameEvent(expectedEvent);
  }

  private Map getMapWithoutCreatedAt(Event event) {
    Map mapEvent = convertObjectToMap(event);
    mapEvent.remove("eventCreatedAt");
    return mapEvent;
  }

  private Map convertObjectToMap(Object object) {
    JsonNode node = mapper.convertValue(object, JsonNode.class);
    return mapper.convertValue(node, Map.class);
  }

  private String convertObjectToJsonString(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  private Map convertJsonStringToMap(String eventAsJson) {
    try {
      return mapper.readValue(eventAsJson, new TypeReference<HashMap>(){});
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }
}
```

## 3-7. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEventTests.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEventTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

// ★ 삭제: import tools.jackson.databind.ObjectMapper;  // 교재와 다름 (교재: com.fasterxml.jackson.databind.ObjectMapper)
import org.junit.jupiter.api.Test;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
// ★ 추가 시작 (2줄)
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
// ★ 추가 끝

class IsSameEventTests {

  ObjectMapper mapper = new ObjectMapper();

  @Test
  void testEventObjectCompare() throws JacksonException {  // ★ 변경 (전: void testEventObjectCompare() {  // 교재와 다름 (교재: throws JsonProcessi...)

    Event<Integer, Product> event1 = new Event<>(CREATE, 1, new Product(1, "name", 1, null));
    Event<Integer, Product> event2 = new Event<>(CREATE, 1, new Product(1, "name", 1, null));
    Event<Integer, Product> event3 = new Event<>(DELETE, 1, null);
    Event<Integer, Product> event4 = new Event<>(CREATE, 1, new Product(2, "name", 1, null));

    String event1Json = mapper.writeValueAsString(event1);

    assertThat(event1Json, is(sameEventExceptCreatedAt(event2)));
    assertThat(event1Json, not(sameEventExceptCreatedAt(event3)));
    assertThat(event1Json, not(sameEventExceptCreatedAt(event4)));
  }
}
```

## 3-8. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/MessagingTests.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/MessagingTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static reactor.core.publisher.Mono.just;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 변경 (전: import org.springframework.boot.webtestclient.autoconfigure.AutoCon...)
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;

@AutoConfigureWebTestClient  // ★ 추가
@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true",
    "eureka.client.enabled=false",
    "spring.cloud.stream.defaultBinder=rabbit",
    "spring.cloud.config.enabled=false"})
@Import({TestChannelBinderConfiguration.class})
// ★ 삭제: @AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
class MessagingTests {

  private static final Logger LOG = LoggerFactory.getLogger(MessagingTests.class);

  @Autowired
  private WebTestClient client;

  @Autowired
  private OutputDestination target;

  @BeforeEach
  void setUp() {
    purgeMessages("products");
    purgeMessages("recommendations");
    purgeMessages("reviews");
  }

  @Test
  void createCompositeProduct1() {

    ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
    postAndVerifyProduct(composite, ACCEPTED);

    final List<String> productMessages = getMessages("products");
    final List<String> recommendationMessages = getMessages("recommendations");
    final List<String> reviewMessages = getMessages("reviews");

    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedEvent =
      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));

    assertEquals(0, recommendationMessages.size());
    assertEquals(0, reviewMessages.size());
  }

  @Test
  void createCompositeProduct2() {

    ProductAggregate composite = new ProductAggregate(1, "name", 1,
      singletonList(new RecommendationSummary(1, "a", 1, "c")),
      singletonList(new ReviewSummary(1, "a", "s", "c")), null);
    postAndVerifyProduct(composite, ACCEPTED);

    final List<String> productMessages = getMessages("products");
    final List<String> recommendationMessages = getMessages("recommendations");
    final List<String> reviewMessages = getMessages("reviews");

    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedProductEvent =
      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

    assertEquals(1, recommendationMessages.size());

    RecommendationSummary rec = composite.getRecommendations().get(0);
    Event<Integer, Recommendation> expectedRecommendationEvent =
      new Event<>(CREATE, composite.getProductId(),
        new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(), rec.getRate(), rec.getContent(), null));
    assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

    assertEquals(1, reviewMessages.size());

    ReviewSummary rev = composite.getReviews().get(0);
    Event<Integer, Review> expectedReviewEvent =
      new Event<>(CREATE, composite.getProductId(), new Review(composite.getProductId(), rev.getReviewId(), rev.getAuthor(), rev.getSubject(), rev.getContent(), null));
    assertThat(reviewMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
  }

  @Test
  void deleteCompositeProduct() {
    deleteAndVerifyProduct(1, ACCEPTED);

    final List<String> productMessages = getMessages("products");
    final List<String> recommendationMessages = getMessages("recommendations");
    final List<String> reviewMessages = getMessages("reviews");

    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedProductEvent = new Event<>(DELETE, 1, null);
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

    assertEquals(1, recommendationMessages.size());

    Event<Integer, Recommendation> expectedRecommendationEvent = new Event<>(DELETE, 1, null);
    assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

    assertEquals(1, reviewMessages.size());

    Event<Integer, Review> expectedReviewEvent = new Event<>(DELETE, 1, null);
    assertThat(reviewMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
  }

  private void purgeMessages(String bindingName) {
    getMessages(bindingName);
  }

  private List<String> getMessages(String bindingName) {
    List<String> messages = new ArrayList<>();
    boolean anyMoreMessages = true;

    while (anyMoreMessages) {
      Message<byte[]> message = getMessage(bindingName);

      if (message == null) {
        anyMoreMessages = false;

      } else {
        messages.add(new String(message.getPayload()));
      }
    }
    return messages;
  }

  private Message<byte[]> getMessage(String bindingName) {
    try {
      return target.receive(0, bindingName);
    } catch (NullPointerException npe) {
      LOG.error("getMessage() received a NPE with binding = {}", bindingName);
      return null;
    }
  }

  private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
    client.post()
      .uri("/product-composite")
      .body(just(compositeProduct), ProductAggregate.class)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus);
  }

  private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    client.delete()
      .uri("/product-composite/" + productId)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus);
  }
}
```

## 3-9. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // ★ 변경 (전: import org.springframework.boot.webtestclient.autoconfigure.AutoCon...)
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

@AutoConfigureWebTestClient  // ★ 추가
@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true",
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"})
// ★ 삭제: @AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
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

    when(compositeIntegration.getProduct(eq(PRODUCT_ID_OK), anyInt(), anyInt()))
      .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));

    when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address"))));

    when(compositeIntegration.getReviews(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));

    when(compositeIntegration.getProduct(eq(PRODUCT_ID_NOT_FOUND), anyInt(), anyInt())).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

    when(compositeIntegration.getProduct(eq(PRODUCT_ID_INVALID), anyInt(), anyInt())).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
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

    getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_CONTENT)  // ★ 변경 (전: getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_CONTENT)  // ...)
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

## 3-10. `microservices/product-composite-service/src/test/resources/application.yml`
```bash
code microservices/product-composite-service/src/test/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 삭제: spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)
api:

  common:
    version:
    title:
    description:
    termsOfService:
    license:
    licenseUrl:
    externalDocDesc:
    externalDocUrl:
    contact:
      name:
      url:
      email:

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings:
    products-out-0.destination: products
    recommendations-out-0.destination: recommendations
    reviews-out-0.destination: reviews
```

## 3-11. `microservices/product-composite-service/Dockerfile`
```bash
code microservices/product-composite-service/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 3-12. 지울 파일
```bash
cd ~/msa
rm microservices/product-composite-service/.gitattributes
rm microservices/product-composite-service/HELP.md
```

다음: [04-spring-cloud.md](04-spring-cloud.md)
