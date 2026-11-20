# 03. composite: WebClient, 이벤트 보내기, 헬스 체크

> 시작: 교수님 `Chapter06` 상태 / 결과: 교수님 `Chapter07`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `ProductCompositeIntegration`: `RestTemplate` → `WebClient`(논블로킹). 생성·삭제는 HTTP 대신 `StreamBridge`로 이벤트를 보낸다.
- `ProductCompositeServiceImpl`: 세 서비스 조회를 **동시에** 하고 `Mono.zip`으로 합친다.
- `HealthCheckConfiguration` (새 파일): `/actuator/health`에 세 핵심 서비스의 상태도 넣는다.
- 테스트: `MessagingTests`(보낸 이벤트 확인, 테스트 바인더 사용), `IsSameEvent`(이벤트 비교용 매처).

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

ext {  // ★ 추가
    springCloudVersion = '2025.1.4'  // ★ 추가
}  // ★ 추가
repositories {
    mavenCentral()
}

jar {
    enabled = false
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-webclient'  // ★ 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'  // ★ 추가
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
// ★ 추가 (5줄)
}
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 3-2. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/HealthCheckConfiguration.java` (새 파일)
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/HealthCheckConfiguration.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.CompositeReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

@Configuration
public class HealthCheckConfiguration {

  @Autowired
  ProductCompositeIntegration integration;

  @Bean
  ReactiveHealthContributor coreServices() {

    final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();

    registry.put("product", () -> integration.getProductHealth());
    registry.put("recommendation", () -> integration.getRecommendationHealth());
    registry.put("review", () -> integration.getReviewHealth());

    return CompositeReactiveHealthContributor.fromMap(registry);
  }
}
```

## 3-3. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java`
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
import org.slf4j.Logger;  // ★ 추가
import org.slf4j.LoggerFactory;  // ★ 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;  // ★ 변경 (전: import org.springframework.web.client.RestTemplate;)
import reactor.core.scheduler.Schedulers;  // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductCompositeServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceApplication.class);  // ★ 추가

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

  // ★ 추가 (9줄)
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
  public Scheduler publishEventScheduler() {  // ★ 변경 (전: RestTemplate restTemplate() {)
    LOG.info("Creates a messagingScheduler with connectionPoolSize = {}", threadPoolSize);  // ★ 변경 (전: return new RestTemplate();)
    return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool");  // ★ 추가
  }

  public static void main(String[] args) {
    SpringApplication.run(ProductCompositeServiceApplication.class, args);
  }

}
```

## 3-4. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java`
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import static java.util.logging.Level.FINE;  // ★ 변경 (전: import static org.springframework.http.HttpMethod.GET;)
import static reactor.core.publisher.Flux.empty;  // ★ 추가
import static se.magnus.api.event.Event.Type.CREATE;  // ★ 추가
import static se.magnus.api.event.Event.Type.DELETE;  // ★ 추가

// ★ 삭제 (2줄, 첫 줄: import java.util.ArrayList;)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;  // ★ 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;  // ★ 변경 (전: import org.springframework.core.ParameterizedTypeReference;)
import org.springframework.cloud.stream.function.StreamBridge;  // ★ 추가
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;  // ★ 추가
import org.springframework.messaging.support.MessageBuilder;  // ★ 추가
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;  // ★ 변경 (전: import org.springframework.web.client.HttpClientErrorException;)
import org.springframework.web.reactive.function.client.WebClientResponseException;  // ★ 변경 (전: import org.springframework.web.client.RestTemplate;)
import reactor.core.publisher.Flux;  // ★ 추가
import reactor.core.publisher.Mono;  // ★ 추가
import reactor.core.scheduler.Scheduler;  // ★ 추가
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.event.Event;  // ★ 추가
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final WebClient webClient;  // ★ 변경 (전: private final RestTemplate restTemplate;)
  private final ObjectMapper mapper;

  private final String productServiceUrl;
  private final String recommendationServiceUrl;
  private final String reviewServiceUrl;

  private final StreamBridge streamBridge;  // ★ 추가

  private final Scheduler publishEventScheduler;  // ★ 추가

  public ProductCompositeIntegration(
    @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,  // ★ 변경 (전: RestTemplate restTemplate,)

    WebClient.Builder webClient,  // ★ 추가
    ObjectMapper mapper,
    StreamBridge streamBridge,  // ★ 추가

    @Value("${app.product-service.host}") String productServiceHost,
    @Value("${app.product-service.port}") int  productServicePort,  // ★ 변경 (전: @Value("${app.product-service.port}") int productServicePort,)

    @Value("${app.recommendation-service.host}") String recommendationServiceHost,
    @Value("${app.recommendation-service.port}") int  recommendationServicePort,  // ★ 변경 (전: @Value("${app.recommendation-service.port}") int recommendationServ...)

    @Value("${app.review-service.host}") String reviewServiceHost,
    @Value("${app.review-service.port}") int  reviewServicePort  // ★ 변경 (전: @Value("${app.review-service.port}") int reviewServicePort) {)
  ) {  // ★ 추가

    this.publishEventScheduler = publishEventScheduler;  // ★ 변경 (전: this.restTemplate = restTemplate;)
    this.webClient = webClient.build();  // ★ 추가
    this.mapper = mapper;
    this.streamBridge = streamBridge;  // ★ 추가

    productServiceUrl        = "http://" + productServiceHost + ":" + productServicePort;  // ★ 변경 (전: productServiceUrl = "http://" + productServiceHost + ":" + productS...)
    recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort;  // ★ 변경 (전: recommendationServiceUrl = "http://" + recommendationServiceHost + ...)
    reviewServiceUrl         = "http://" + reviewServiceHost + ":" + reviewServicePort;  // ★ 변경 (전: reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServ...)
  }

  @Override
  public Mono<Product> createProduct(Product body) {  // ★ 변경 (전: public Product createProduct(Product body) {)

    // ★ 변경 (4줄, 전: 9줄)
    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Product> getProduct(int productId) {  // ★ 변경 (전: public Product getProduct(int productId) {)
    String url = productServiceUrl + "/product/" + productId;  // ★ 추가
    LOG.debug("Will call the getProduct API on URL: {}", url);  // ★ 추가

    // ★ 변경 (1줄, 전: 9줄)
    return webClient.get().uri(url).retrieve().bodyToMono(Product.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
  }

  @Override
  // ★ 변경 (1줄, 전: 4줄)
  public Mono<Void> deleteProduct(int productId) {

    // ★ 변경 (2줄, 전: 4줄)
    return Mono.fromRunnable(() -> sendMessage("products-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Mono<Recommendation> createRecommendation(Recommendation body) {  // ★ 변경 (전: public Recommendation createRecommendation(Recommendation body) {)

    // ★ 변경 (4줄, 전: 9줄)
    return Mono.fromCallable(() -> {
      sendMessage("recommendations-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {  // ★ 변경 (전: public List<Recommendation> getRecommendations(int productId) {)

    String url = recommendationServiceUrl + "/recommendation?productId=" + productId;  // ★ 변경 (전: try {)
// ★ 삭제: String url = recommendationServiceUrl + "?productId=" + productId;

    // ★ 변경 (1줄, 전: 4줄)
    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    // ★ 변경 (1줄, 전: 6줄)
    return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  // ★ 변경 (1줄, 전: 4줄)
  public Mono<Void> deleteRecommendations(int productId) {

    // ★ 변경 (2줄, 전: 4줄)
    return Mono.fromRunnable(() -> sendMessage("recommendations-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  @Override
  public Mono<Review> createReview(Review body) {  // ★ 변경 (전: public Review createReview(Review body) {)

    // ★ 변경 (4줄, 전: 9줄)
    return Mono.fromCallable(() -> {
      sendMessage("reviews-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Flux<Review> getReviews(int productId) {  // ★ 변경 (전: public List<Review> getReviews(int productId) {)

    String url = reviewServiceUrl + "/review?productId=" + productId;  // ★ 변경 (전: try {)
// ★ 삭제: String url = reviewServiceUrl + "?productId=" + productId;

    // ★ 변경 (1줄, 전: 4줄)
    LOG.debug("Will call the getReviews API on URL: {}", url);

    // ★ 변경 (1줄, 전: 6줄)
    return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  // ★ 변경 (1줄, 전: 4줄)
  public Mono<Void> deleteReviews(int productId) {

    // ★ 변경 (2줄, 전: 4줄)
    return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  // ★ 변경 (31줄, 전: 2줄)
  public Mono<Health> getProductHealth() {
    return getHealth(productServiceUrl);
  }

  public Mono<Health> getRecommendationHealth() {
    return getHealth(recommendationServiceUrl);
  }

  public Mono<Health> getReviewHealth() {
    return getHealth(reviewServiceUrl);
  }

  private Mono<Health> getHealth(String url) {
    url += "/actuator/health";
    LOG.debug("Will call the Health API on URL: {}", url);
    return webClient.get().uri(url).retrieve().bodyToMono(String.class)
      .map(s -> new Health.Builder().up().build())
      .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
      .log(LOG.getName(), FINE);
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
        return new NotFoundException(getErrorMessage(wcre));  // ★ 변경 (전: return new NotFoundException(getErrorMessage(ex));)

      case UNPROCESSABLE_CONTENT:
        return new InvalidInputException(getErrorMessage(wcre));  // ★ 변경 (전: return new InvalidInputException(getErrorMessage(ex));)

      default:
        LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());  // ★ 변경 (전: LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.ge...)
        LOG.warn("Error body: {}", wcre.getResponseBodyAsString());  // ★ 변경 (전: LOG.warn("Error body: {}", ex.getResponseBodyAsString());)
        return ex;
    }
  }

  private String getErrorMessage(WebClientResponseException ex) {  // ★ 변경 (전: private String getErrorMessage(HttpClientErrorException ex) {)
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (JacksonException ioex) {
      return ex.getMessage();
    }
  }
}
```

## 3-5. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java`
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import static java.util.logging.Level.FINE;  // ★ 추가

import java.util.ArrayList;  // ★ 추가
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;  // ★ 추가
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
// ★ 삭제: import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

  private final ServiceUtil serviceUtil;
  private final ProductCompositeIntegration integration;  // ★ 변경 (전: private ProductCompositeIntegration integration;)

  public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {  // ★ 변경 (전: public ProductCompositeServiceImpl()
    // ★ 삭제: ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public Mono<Void> createProduct(ProductAggregate body) {  // ★ 변경 (전: public void createProduct(ProductAggregate body) {)

    try {

      List<Mono> monoList = new ArrayList<>();  // ★ 변경 (전: LOG.debug("createCompositeProduct: creates a new composite entity f...)

      LOG.info("Will create a new composite entity for product.id: {}", body.getProductId());  // ★ 추가

      Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
      monoList.add(integration.createProduct(product));  // ★ 변경 (전: integration.createProduct(product);)

      if (body.getRecommendations() != null) {
        body.getRecommendations().forEach(r -> {
          Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
          monoList.add(integration.createRecommendation(recommendation));  // ★ 변경 (전: integration.createRecommendation(recommendation);)
        });
      }

      if (body.getReviews() != null) {
        body.getReviews().forEach(r -> {
          Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
          monoList.add(integration.createReview(review));  // ★ 변경 (전: integration.createReview(review);)
        });
      }

      LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

      return Mono.zip(r -> "", monoList.toArray(new Mono[0]))  // ★ 추가
        .doOnError(ex -> LOG.warn("createCompositeProduct failed: {}", ex.toString()))  // ★ 추가
        .then();  // ★ 추가

    } catch (RuntimeException re) {
      LOG.warn("createCompositeProduct failed: {}", re.toString());  // ★ 변경 (전: LOG.warn("createCompositeProduct failed", re);)
      throw re;
    }
  }

  @Override
  @SuppressWarnings("unchecked")  // ★ 변경 (전: public ProductAggregate getProduct(int productId) {)
  public Mono<ProductAggregate> getProduct(int productId) {  // ★ 추가

    // ★ 변경 (8줄, 전: 9줄)
    LOG.info("Will get composite product info for product.id={}", productId);
    return Mono.zip(
      values -> createProductAggregate((Product) values[0], (List<Recommendation>) values[1], (List<Review>) values[2], serviceUtil.getServiceAddress()),
      integration.getProduct(productId),
      integration.getRecommendations(productId).collectList(),
      integration.getReviews(productId).collectList())
      .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
      .log(LOG.getName(), FINE);
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {  // ★ 변경 (전: public void deleteProduct(int productId) {)

    try {  // ★ 변경 (전: LOG.debug("deleteCompositeProduct: Deletes a product aggregate for ...)

      LOG.info("Will delete a product aggregate for product.id: {}", productId);  // ★ 변경 (전: integration.deleteProduct(productId);)

      // ★ 변경 (7줄, 전: 1줄)
      return Mono.zip(
        r -> "",
        integration.deleteProduct(productId),
        integration.deleteRecommendations(productId),
        integration.deleteReviews(productId))
        .doOnError(ex -> LOG.warn("delete failed: {}", ex.toString()))
        .log(LOG.getName(), FINE).then();

    } catch (RuntimeException re) {  // ★ 변경 (전: integration.deleteReviews(productId);)
      LOG.warn("deleteCompositeProduct failed: {}", re.toString());  // ★ 추가
      throw re;  // ★ 변경 (전: LOG.debug("deleteCompositeProduct: aggregate entities deleted for p...)
    }  // ★ 추가
  }

  // ★ 변경 (1줄, 전: 5줄)
  private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

    int productId = product.getProductId();
    String name = product.getName();
    int weight = product.getWeight();

    List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
       recommendations.stream()  // ★ 변경 (전: recommendations.stream())
        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
        .collect(Collectors.toList());

    List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :  // ★ 변경 (전: List<ReviewSummary> reviewSummaries = (reviews == null) ? null :)
      reviews.stream()
        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
        .collect(Collectors.toList());

    String productAddress = product.getServiceAddress();
    String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
    String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

    return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
  }
}
```

## 3-6. `microservices/product-composite-service/src/main/resources/application.yml`
```bash
code microservices/product-composite-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
springdoc:
  swagger-ui.path: /openapi/swagger-ui.html
  api-docs.path: /openapi/v3/api-docs
  packagesToScan: se.magnus.microservices.composite.product
  pathsToMatch: /**

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
        # Normal response
        If the requested product id is found the method will return information regarding:
        1. Product information
        1. Reviews
        1. Recommendations
        1. Service Addresses\n(technical information regarding the addresses of the microservices that created the response)

        # Expected partial and error responses
        1. If no product information is found, a **404 - Not Found** error will be returned
        1. In no recommendations or reviews are found for a product, a partial response will be returned

    create-composite-product:
      description: Creates a composite product
      notes: |
        # Normal response
        The composite product information posted to the API will be split up and stored as separate product-info, recommendation and review entities.

        # Expected error responses
        1. If a product with the same productId as specified in the posted information already exists,
           an **422 - Unprocessable Entity** error with a "duplicate key" error message will be returned

    delete-composite-product:
      description: Deletes a product composite
      notes: |
        # Normal response
        Entities for product information, recommendations and reviews related to the specified productId will be deleted.
        The implementation of the delete method is idempotent, i.e. it can be called several times with the same response.

        This means that a delete request of a non-existing product will return **200 Ok**.

server.port: 7001
spring.web.error.include-message: always

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

# ★ 추가 (25줄)
spring.cloud.stream.output-bindings: products-out-0;recommendations-out-0;reviews-out-0

spring.cloud.stream:
  defaultBinder: rabbit
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

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG

management.endpoint.health.show-details: "ALWAYS"  # ★ 추가
management.endpoints.web.exposure.include: "*"  # ★ 추가
---
spring.config.activate.on-profile: docker

server.port: 8080

app:
  product-service:
    host: product
    port: 8080
  recommendation-service:
    host: recommendation
    port: 8080
  review-service:
    host: review
    port: 8080
# ★ 추가 (19줄)

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

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

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

## 3-7. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEvent.java` (새 파일)
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEvent.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.magnus.api.event.Event;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

## 3-8. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEventTests.java` (새 파일)
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEventTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

import org.junit.jupiter.api.Test;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

class IsSameEventTests {

  ObjectMapper mapper = new ObjectMapper();

  @Test
  void testEventObjectCompare() throws JacksonException {

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

## 3-9. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/MessagingTests.java` (새 파일)
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/MessagingTests.java
```
붙여넣기 (새 파일)
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
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

@AutoConfigureWebTestClient
@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
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

## 3-10. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
// ★ 삭제: import static reactor.core.publisher.Mono.just;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;  // ★ 변경 (전: import se.magnus.api.composite.product.ProductAggregate;)
import reactor.core.publisher.Mono;  // ★ 변경 (전: import se.magnus.api.composite.product.RecommendationSummary;)
// ★ 삭제: import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductCompositeServiceApplicationTests {

  private static final int PRODUCT_ID_OK = 1;
  private static final int PRODUCT_ID_NOT_FOUND = 2;
  private static final int PRODUCT_ID_INVALID = 3;

  @Autowired  // ★ 변경 (전: @Autowired private WebTestClient client;)
  private WebTestClient client;  // ★ 추가

  @MockitoBean  // ★ 변경 (전: @MockitoBean private ProductCompositeIntegration compositeIntegration;)
  private ProductCompositeIntegration compositeIntegration;  // ★ 추가

  @BeforeEach
  void setUp() {

    when(compositeIntegration.getProduct(PRODUCT_ID_OK))
      .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));  // ★ 변경 (전: .thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));)

    when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address"))));  // ★ 변경 (전: .thenReturn(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "aut...)

    when(compositeIntegration.getReviews(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));  // ★ 변경 (전: .thenReturn(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "s...)

    when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND)).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));  // ★ 변경 (전: when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND)))
// ★ 삭제: .thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOU...

    when(compositeIntegration.getProduct(PRODUCT_ID_INVALID)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));  // ★ 변경 (전: when(compositeIntegration.getProduct(PRODUCT_ID_INVALID)))
  // ★ 삭제: .thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVAL...
  }

  @Test
  // ★ 변경 (1줄, 전: 21줄)
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
// ★ 삭제 (13줄, 첫 줄: private void postAndVerifyProduct(ProductAggreg...)
}
```

다음: [04-build-compose-test.md](04-build-compose-test.md)
