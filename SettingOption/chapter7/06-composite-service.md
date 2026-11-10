# 06. composite: 조회는 WebClient, 생성/삭제는 이벤트 게시

> 표기: `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.
> 교재 "복합 서비스의 논블로킹 REST API", "복합 서비스에서 이벤트 게시", "상태 점검 API 추가" 절.

| 연산 | 6장 | 7장 |
|---|---|---|
| GET | RestTemplate로 3번 차례로 호출 | **WebClient로 3개를 동시에** 호출 → `Mono.zip`으로 합침 |
| POST | 3개 서비스에 HTTP POST | 토픽 3개에 `CREATE` 이벤트 게시 → **202 Accepted** |
| DELETE | 3개 서비스에 HTTP DELETE | 토픽 3개에 `DELETE` 이벤트 게시 → **202 Accepted** |

## 1. composite API 인터페이스 (전체 교체)
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;  // ★ 추가

@Tag(name = "ProductComposite", description = "REST API for composite product information.")
public interface ProductCompositeService {

  /**
   * Sample usage, see below.
   *
   * curl -X POST $HOST:$PORT/product-composite \
   *   -H "Content-Type: application/json" --data \
   *   '{"productId":123,"name":"product 123","weight":123}'
   *
   * @param body A JSON representation of the new composite product
   */
  @Operation(
    summary = "${api.product-composite.create-composite-product.description}",
    description = "${api.product-composite.create-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @ResponseStatus(HttpStatus.ACCEPTED)  // ★ 추가 (202: 받았고 처리는 나중에)
  @PostMapping(
    value    = "/product-composite",
    consumes = "application/json")
  Mono<Void> createProduct(@RequestBody ProductAggregate body);  // ★ 변경 (전: void)

  /**
   * Sample usage: "curl $HOST:$PORT/product-composite/1".
   *
   * @param productId Id of the product
   * @return the composite product info, if found, else null
   */
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
  Mono<ProductAggregate> getProduct(@PathVariable int productId);  // ★ 변경 (전: ProductAggregate)

  /**
   * Sample usage: "curl -X DELETE $HOST:$PORT/product-composite/1".
   *
   * @param productId Id of the product
   */
  @Operation(
    summary = "${api.product-composite.delete-composite-product.description}",
    description = "${api.product-composite.delete-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @ResponseStatus(HttpStatus.ACCEPTED)  // ★ 추가
  @DeleteMapping(value = "/product-composite/{productId}")
  Mono<Void> deleteProduct(@PathVariable int productId);  // ★ 변경 (전: void)
}
```

`202 Accepted` = "요청은 받았고 처리는 나중에(비동기로) 끝난다". 200과 달리 저장이 끝났다는 뜻이 아니다.

## 2. Application: 이벤트 게시용 스레드 풀 (전체 교체)
`StreamBridge.send`는 블로킹 호출이라 review의 `jdbcScheduler`(04)처럼 **`publishEventScheduler`** 에서 돌린다. 6장의 RestTemplate 빈은 지운다.
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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

  /**
  * Will exposed on $HOST:$PORT/swagger-ui.html
  *
  * @return the common OpenAPI documentation
  */
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

  // ★ 추가 (생성자와 publishEventScheduler 빈: 이벤트 게시를 별도 스레드 풀에서 실행)
  // ★ 삭제: RestTemplate 빈 (WebClient로 바뀜)
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

  public static void main(String[] args) {
    SpringApplication.run(ProductCompositeServiceApplication.class, args);
  }

}
```

## 3. ProductCompositeIntegration (전체 교체)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;  // ★ 추가 (교재: org.springframework.boot.actuate.health.Health)
import org.springframework.cloud.stream.function.StreamBridge;  // ★ 추가
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;  // ★ 추가 (RestTemplate 대신)
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

  private final WebClient webClient;  // ★ 변경 (전: RestTemplate restTemplate)
  private final ObjectMapper mapper;

  private final String productServiceUrl;
  private final String recommendationServiceUrl;
  private final String reviewServiceUrl;

  private final StreamBridge streamBridge;  // ★ 추가

  private final Scheduler publishEventScheduler;  // ★ 추가

  public ProductCompositeIntegration(
    @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,

    WebClient.Builder webClient,
    ObjectMapper mapper,
    StreamBridge streamBridge,

    @Value("${app.product-service.host}") String productServiceHost,
    @Value("${app.product-service.port}") int  productServicePort,

    @Value("${app.recommendation-service.host}") String recommendationServiceHost,
    @Value("${app.recommendation-service.port}") int  recommendationServicePort,

    @Value("${app.review-service.host}") String reviewServiceHost,
    @Value("${app.review-service.port}") int  reviewServicePort
  ) {

    this.publishEventScheduler = publishEventScheduler;
    this.webClient = webClient.build();
    this.mapper = mapper;
    this.streamBridge = streamBridge;

    productServiceUrl        = "http://" + productServiceHost + ":" + productServicePort;
    recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort;
    reviewServiceUrl         = "http://" + reviewServiceHost + ":" + reviewServicePort;
  }

  @Override
  public Mono<Product> createProduct(Product body) {  // ★ 변경: HTTP POST 대신 이벤트 게시

    return Mono.fromCallable(() -> {
      sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Product> getProduct(int productId) {
    String url = productServiceUrl + "/product/" + productId;
    LOG.debug("Will call the getProduct API on URL: {}", url);

    return webClient.get().uri(url).retrieve().bodyToMono(Product.class).log(LOG.getName(), FINE).onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
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

    String url = recommendationServiceUrl + "/recommendation?productId=" + productId;

    LOG.debug("Will call the getRecommendations API on URL: {}", url);

    // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
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

    String url = reviewServiceUrl + "/review?productId=" + productId;

    LOG.debug("Will call the getReviews API on URL: {}", url);

    // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
    return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log(LOG.getName(), FINE).onErrorResume(error -> empty());
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {

    return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event<>(DELETE, productId, null)))
      .subscribeOn(publishEventScheduler).then();
  }

  // ★ 추가 (상태 점검: HealthCheckConfiguration이 사용)
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

  // ★ 추가 (이벤트를 토픽으로 보내는 공통 메서드)
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

| 부분 | 하는 일 |
|---|---|
| `webClient.get().uri(url).retrieve().bodyToMono(...)` | 논블로킹 GET. 오류는 `onErrorMap`으로 6장과 같은 예외(404→NotFound, 422→InvalidInput)로 |
| `getRecommendations`/`getReviews`의 `onErrorResume(error -> empty())` | 추천/리뷰가 실패해도 **빈 목록**으로 부분 응답 (6장과 같은 정책) |
| `sendMessage(바인딩, event)` | `StreamBridge`로 `products-out-0` 등에 이벤트 전송. `partitionKey` 헤더에 productId (07 파티션 실습) |
| `Mono.fromCallable(...).subscribeOn(publishEventScheduler)` | 전송을 별도 스레드 풀에서 |
| `getProductHealth()` 등 | 각 서비스의 `/actuator/health`를 불러 UP/DOWN 판단 |

## 4. 헬스 체크 설정 (새 파일)
composite의 `/actuator/health`에 **세 핵심 서비스의 상태**를 같이 보여 준다. 07의 `test-em-all.bash`가 이걸로 "전부 떴는지"를 기다린다.
```bash
mkdir -p microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/HealthCheckConfiguration.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.CompositeReactiveHealthContributor;  // 교재와 다름 (교재: org.springframework.boot.actuate.health.*)
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

**교재와 다름:** Boot 4에서 헬스 관련 클래스가 `org.springframework.boot.actuate.health` → **`org.springframework.boot.health.contributor`** 로 옮겨졌다(Integration의 `Health` import도 같음).

## 5. ProductCompositeServiceImpl (전체 교체)
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private final ServiceUtil serviceUtil;
  private final ProductCompositeIntegration integration;

  public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  public Mono<Void> createProduct(ProductAggregate body) {  // ★ 변경: 결과 Mono들을 모아 Mono.zip으로 합침

    try {

      List<Mono> monoList = new ArrayList<>();

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
  public Mono<ProductAggregate> getProduct(int productId) {  // ★ 변경: 세 서비스를 동시에 부르고 Mono.zip으로 합침

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
  public Mono<Void> deleteProduct(int productId) {  // ★ 변경: 삭제 이벤트 3개를 Mono.zip으로 합침

    try {

      LOG.info("Will delete a product aggregate for product.id: {}", productId);

      return Mono.zip(
        r -> "",
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

  private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

    // 1. Setup product info
    int productId = product.getProductId();
    String name = product.getName();
    int weight = product.getWeight();

    // 2. Copy summary recommendation info, if available
    List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
       recommendations.stream()
        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
        .collect(Collectors.toList());

    // 3. Copy summary review info, if available
    List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
      reviews.stream()
        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
        .collect(Collectors.toList());

    // 4. Create info regarding the involved microservices addresses
    String productAddress = product.getServiceAddress();
    String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
    String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

    return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
  }
}
```

- `getProduct`: `Mono.zip(합치는 함수, 상품, 추천목록, 리뷰목록)` → 세 호출이 **동시에** 나가고 셋 다 오면 `ProductAggregate`를 만든다.
- `createProduct`: 이벤트 게시 `Mono`들을 리스트에 모아 `Mono.zip(...)`. 하나라도 실패하면 오류.

## 6. application.yml (전체 교체)
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
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 3장부터 유지)

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

spring.cloud.stream:  # ★ 추가 (여기부터 management까지)
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

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

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

spring.rabbitmq.host: rabbitmq  # ★ 추가

spring.cloud.stream.kafka.binder.brokers: kafka  # ★ 추가

# ★ 추가 (여기부터 파일 끝까지)
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

| 설정 | 뜻 |
|---|---|
| `bindings.products-out-0.destination: products` | Integration의 `sendMessage("products-out-0", ...)`가 `products` 토픽으로 |
| `required-groups: auditGroup` | 받을 서비스가 아직 안 떠 있어도 메시지가 사라지지 않게 `auditGroup` 큐를 미리 만든다(07에서 RabbitMQ 화면으로 확인) |
| `streaming_partitioned` 프로필 | `partitionKey` 헤더(productId)로 파티션 2개에 나눠 보냄 |

## 7. 테스트 (4개)
### 7-1. ProductCompositeServiceApplicationTests (전체 교체)
Integration이 `Mono`/`Flux`를 돌려주니 목(mock)도 `Mono.just(...)`, `Flux.fromIterable(...)`로 바뀐다. 생성/삭제 테스트는 7-4의 MessagingTests로 옮겨간다.
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

@SpringBootTest(webEnvironment = RANDOM_PORT)
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

    when(compositeIntegration.getProduct(PRODUCT_ID_OK))
      .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));

    when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address"))));

    when(compositeIntegration.getReviews(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));

    when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND)).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

    when(compositeIntegration.getProduct(PRODUCT_ID_INVALID)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
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

### 7-2. IsSameEvent (새 파일)
보낸 메시지(JSON 문자열)와 기대 이벤트를 비교하는 Hamcrest 매처. **`eventCreatedAt`(만든 시각)만 빼고** 비교한다(시각은 매번 다르니까).
```bash
mkdir -p microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/IsSameEvent.java
```
붙여넣기 (새 파일)
```java
package se.magnus.microservices.composite.product;

import tools.jackson.core.JacksonException;  // 교재와 다름 (Jackson 3: tools.jackson.*, 교재: com.fasterxml.jackson.*)
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.magnus.api.event.Event;

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

### 7-3. IsSameEventTests (새 파일)
```bash
mkdir -p microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product
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

import tools.jackson.databind.ObjectMapper;  // 교재와 다름 (교재: com.fasterxml.jackson.databind.ObjectMapper)
import org.junit.jupiter.api.Test;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;

class IsSameEventTests {

  ObjectMapper mapper = new ObjectMapper();

  @Test
  void testEventObjectCompare() {  // 교재와 다름 (교재: throws JsonProcessingException, Jackson 3 예외는 unchecked)

    // Event #1 and #2 are the same event, but occurs as different times
    // Event #3 and #4 are different events
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

### 7-4. MessagingTests (새 파일)
`TestChannelBinderConfiguration`을 넣으면 진짜 RabbitMQ 대신 **테스트 바인더**가 쓰인다. `OutputDestination.receive(0, "products")`로 composite가 보낸 메시지를 꺼내서 7-2 매처로 확인한다.
```bash
mkdir -p microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product
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
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
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

@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
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

    // Assert one expected new product event queued up
    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedEvent =
      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));

    // Assert no recommendation and review events
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

    // Assert one create product event queued up
    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedProductEvent =
      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

    // Assert one create recommendation event queued up
    assertEquals(1, recommendationMessages.size());

    RecommendationSummary rec = composite.getRecommendations().get(0);
    Event<Integer, Recommendation> expectedRecommendationEvent =
      new Event<>(CREATE, composite.getProductId(),
        new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(), rec.getRate(), rec.getContent(), null));
    assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

    // Assert one create review event queued up
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

    // Assert one delete product event queued up
    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedProductEvent = new Event<>(DELETE, 1, null);
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

    // Assert one delete recommendation event queued up
    assertEquals(1, recommendationMessages.size());

    Event<Integer, Recommendation> expectedRecommendationEvent = new Event<>(DELETE, 1, null);
    assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

    // Assert one delete review event queued up
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
      // If the messageQueues member variable in the target object contains no queues when the receive method is called, it will cause a NPE to be thrown.
      // So we catch the NPE here and return null to indicate that no messages were found.
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

**교재와 다름 (7-2, 7-3):** Jackson 3이라 import가 `tools.jackson.*`이고, `JsonProcessingException`/`IOException` 대신 `JacksonException`을 잡는다. Jackson 3 예외는 unchecked라 `IsSameEventTests`의 `throws JsonProcessingException`도 뺐다.

## 확인 (도커 필요 없음)
```bash
./gradlew :microservices:product-composite-service:build
```
`BUILD SUCCESSFUL`이면 된다(테스트 8개: ApplicationTests 4, MessagingTests 3, IsSameEventTests 1). 전체도 한 번:
```bash
./gradlew build
```
