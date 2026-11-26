# 02. composite: Eureka 제거, 서비스 이름으로 호출

> 시작: 15장까지 따라 한 상태 / 결과: 16장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

Eureka와 `@LoadBalanced`/로드 밸런서 필터를 빼고, `http://product`처럼 서비스 이름으로 바로 부른다. 같은 이름의 쿠버네티스 서비스가 파드들에 나눠 준다.

## 2-1. `microservices/product-composite-service/build.gradle`
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
    // ★ 삭제: implementation 'org.springframework.boot:spring-boot-starter-restclient'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    // ★ 삭제: implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    // ★ 삭제: implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
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

## 2-2. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java`
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
// ★ 삭제: import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// ★ 삭제: import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
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

  // ★ 삭제 시작 (2줄) — 아래 줄들을 지운다
  // ★ 지움: @Autowired
  // ★ 지움: private ReactorLoadBalancerExchangeFilterFunction lbFunction;
  // ★ 삭제 끝
  @Bean
  // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: WebClient webClient(WebClient.Builder builder) {
  // ★ 전: return builder.filter(lbFunction).build();
  public WebClient webClient(WebClient.Builder builder) {
    return builder.build();
  // ★ 변경 끝
  }

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(ProductCompositeServiceApplication.class, args);
  }

}
```

## 2-3. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ResilienceConfig.java` (새 파일)
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ResilienceConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

  private static final Logger LOG = LoggerFactory.getLogger(ResilienceConfig.class);

  public ResilienceConfig(
    @Value("${app.resilience.enabled:true}") String resilienceEnabledStr,
    CircuitBreakerRegistry circuitBreakerRegistry,
    TimeLimiterRegistry timeLimiterRegistry,
    RetryRegistry retryRegistry
  ) {

    boolean resilienceEnabled = !("false".equalsIgnoreCase(resilienceEnabledStr));
    LOG.debug("Resilience4j is enabled: {} ({})", resilienceEnabled, resilienceEnabledStr);

    if (!resilienceEnabled) {
      LOG.warn("Resilience4j is disabled (\"app.resilience.enabled = {}\"). CircuitBreaker, TimeLimiter and Retry are not used.", resilienceEnabledStr);
      circuitBreakerRegistry.remove("product");
      retryRegistry.remove("product");
      timeLimiterRegistry.replace("product", io.github.resilience4j.timelimiter.TimeLimiter.of(Duration.ofHours(24)));
      LOG.warn("Instead of disabling the timelimiter, a loooong time out of {} will be used",
        timeLimiterRegistry.find("product").get().getTimeLimiterConfig().getTimeoutDuration());
    }

  }

}
```

## 2-4. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/MessagingTests.java`
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
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true",
    // ★ 삭제: "eureka.client.enabled=false",
    "spring.cloud.stream.defaultBinder=rabbit",
    "spring.cloud.config.enabled=false"})
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

## 2-5. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java`
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
    "spring.main.allow-bean-definition-overriding=true",
    // ★ 삭제: "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"})
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

다음: [03-spring-cloud.md](03-spring-cloud.md)
