# 03. composite: 생성·삭제를 세 서비스로 나눠 호출

> 시작: 교수님 `Chapter05` 상태 / 결과: 교수님 `Chapter06`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `ProductCompositeIntegration`: 핵심 서비스의 POST/DELETE를 부르는 메서드가 추가된다.
- `ProductCompositeServiceImpl`: 생성 요청 하나를 product, recommendation, review 생성으로 나눠 호출하고, 삭제도 세 서비스에 보낸다.
- `application.yml`: API 설명 문구 추가.

## 3-1. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java`
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import static org.springframework.http.HttpMethod.GET;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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

    // ★ 변경 (15줄, 전: 3줄)
    productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product";
    recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation";
    reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review";
  }

  @Override
  public Product createProduct(Product body) {

    try {
      String url = productServiceUrl;
      LOG.debug("Will post a new product to URL: {}", url);

      Product product = restTemplate.postForObject(url, body, Product.class);
      LOG.debug("Created a product with id: {}", product.getProductId());

      return product;

    } catch (HttpClientErrorException ex) {
      throw handleHttpClientException(ex);
    }
  }

  @Override
  public Product getProduct(int productId) {

    try {
      String url = productServiceUrl + "/" + productId;  // ★ 변경 (전: String url = productServiceUrl + productId;)
      LOG.debug("Will call the getProduct API on URL: {}", url);  // ★ 변경 (전: LOG.debug("Will call getProduct API on URL: {}", url);)

      Product product = restTemplate.getForObject(url, Product.class);
      LOG.debug("Found a product with id: {}", product.getProductId());

      return product;

    } catch (HttpClientErrorException ex) {
      // ★ 변경 (1줄, 전: 10줄)
      throw handleHttpClientException(ex);
    }
  }

  @Override  // ★ 변경 (전: private String getErrorMessage(HttpClientErrorException ex) {)
  public void deleteProduct(int productId) {  // ★ 추가
    try {
      // ★ 변경 (17줄, 전: 3줄)
      String url = productServiceUrl + "/" + productId;
      LOG.debug("Will call the deleteProduct API on URL: {}", url);

      restTemplate.delete(url);

    } catch (HttpClientErrorException ex) {
      throw handleHttpClientException(ex);
    }
  }

  @Override
  public Recommendation createRecommendation(Recommendation body) {

    try {
      String url = recommendationServiceUrl;
      LOG.debug("Will post a new recommendation to URL: {}", url);

      Recommendation recommendation = restTemplate.postForObject(url, body, Recommendation.class);
      LOG.debug("Created a recommendation with id: {}", recommendation.getProductId());

      return recommendation;

    } catch (HttpClientErrorException ex) {
      throw handleHttpClientException(ex);
    }
  }

  @Override
  public List<Recommendation> getRecommendations(int productId) {

    try {
      String url = recommendationServiceUrl + "?productId=" + productId;  // ★ 변경 (전: String url = recommendationServiceUrl + productId;)

      LOG.debug("Will call the getRecommendations API on URL: {}", url);  // ★ 변경 (전: LOG.debug("Will call getRecommendations API on URL: {}", url);)
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

  @Override
  // ★ 추가 (22줄)
  public void deleteRecommendations(int productId) {
    try {
      String url = recommendationServiceUrl + "?productId=" + productId;
      LOG.debug("Will call the deleteRecommendations API on URL: {}", url);

      restTemplate.delete(url);

    } catch (HttpClientErrorException ex) {
      throw handleHttpClientException(ex);
    }
  }

  @Override
  public Review createReview(Review body) {

    try {
      String url = reviewServiceUrl;
      LOG.debug("Will post a new review to URL: {}", url);

      Review review = restTemplate.postForObject(url, body, Review.class);
      LOG.debug("Created a review with id: {}", review.getProductId());

      return review;

    } catch (HttpClientErrorException ex) {
      throw handleHttpClientException(ex);
    }
  }

  @Override
  public List<Review> getReviews(int productId) {

    try {
      String url = reviewServiceUrl + "?productId=" + productId;  // ★ 변경 (전: String url = reviewServiceUrl + productId;)

      LOG.debug("Will call the getReviews API on URL: {}", url);  // ★ 변경 (전: LOG.debug("Will call getReviews API on URL: {}", url);)
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
  // ★ 추가 (29줄)

  @Override
  public void deleteReviews(int productId) {
    try {
      String url = reviewServiceUrl + "?productId=" + productId;
      LOG.debug("Will call the deleteReviews API on URL: {}", url);

      restTemplate.delete(url);

    } catch (HttpClientErrorException ex) {
      throw handleHttpClientException(ex);
    }
  }

  private RuntimeException handleHttpClientException(HttpClientErrorException ex) {
    switch (HttpStatus.resolve(ex.getStatusCode().value())) {

      case NOT_FOUND:
        return new NotFoundException(getErrorMessage(ex));

      case UNPROCESSABLE_CONTENT:
        return new InvalidInputException(getErrorMessage(ex));

      default:
        LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
        LOG.warn("Error body: {}", ex.getResponseBodyAsString());
        return ex;
    }
  }

  private String getErrorMessage(HttpClientErrorException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (JacksonException ioex) {
      return ex.getMessage();
    }
  }
}
```

## 3-2. `microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java`
```bash
code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product.services;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;  // ★ 추가
import org.slf4j.LoggerFactory;  // ★ 추가
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);  // ★ 추가

  private final ServiceUtil serviceUtil;
  private ProductCompositeIntegration integration;

  public ProductCompositeServiceImpl(
    ServiceUtil serviceUtil, ProductCompositeIntegration integration) {

    this.serviceUtil = serviceUtil;
    this.integration = integration;
  }

  @Override
  // ★ 추가 (24줄)
  public void createProduct(ProductAggregate body) {

    try {

      LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

      Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
      integration.createProduct(product);

      if (body.getRecommendations() != null) {
        body.getRecommendations().forEach(r -> {
          Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
          integration.createRecommendation(recommendation);
        });
      }

      if (body.getReviews() != null) {
        body.getReviews().forEach(r -> {
          Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
          integration.createReview(review);
        });
      }

      LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

    } catch (RuntimeException re) {
      LOG.warn("createCompositeProduct failed", re);
      throw re;
    }
  }

  @Override
  public ProductAggregate getProduct(int productId) {

    LOG.debug("getCompositeProduct: lookup a product aggregate for productId: {}", productId);  // ★ 추가

    Product product = integration.getProduct(productId);
    if (product == null) {
      throw new NotFoundException("No product found for productId: " + productId);
    }

    List<Recommendation> recommendations = integration.getRecommendations(productId);

    List<Review> reviews = integration.getReviews(productId);

    LOG.debug("getCompositeProduct: aggregate entity found for productId: {}", productId);  // ★ 추가

    return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
  // ★ 추가 (8줄)
  }

  @Override
  public void deleteProduct(int productId) {

    LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

    integration.deleteProduct(productId);

    integration.deleteRecommendations(productId);

    integration.deleteReviews(productId);

    LOG.debug("deleteCompositeProduct: aggregate entities deleted for productId: {}", productId);
  }

  private ProductAggregate createProductAggregate(
    Product product,
    List<Recommendation> recommendations,
    List<Review> reviews,
    String serviceAddress) {

    int productId = product.getProductId();
    String name = product.getName();
    int weight = product.getWeight();

    List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :  // ★ 변경 (전: List<RecommendationSummary> recommendationSummaries =)
      recommendations.stream()  // ★ 변경 (전: (recommendations == null) ? null : recommendations.stream())
        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))  // ★ 변경 (전: .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getA...)
        .collect(Collectors.toList());

    List<ReviewSummary> reviewSummaries = (reviews == null) ? null :  // ★ 변경 (전: List<ReviewSummary> reviewSummaries =)
      reviews.stream()  // ★ 변경 (전: (reviews == null) ? null : reviews.stream())
        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))  // ★ 변경 (전: .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSu...)
        .collect(Collectors.toList());

    String productAddress = product.getServiceAddress();
    String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
    String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
    ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

    return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
  }
}
```

## 3-3. `microservices/product-composite-service/src/main/resources/application.yml`
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

    create-composite-product:  # ★ 변경 (전: ## Product id 113)
      description: Creates a composite product  # ★ 변경 (전: 200 - Ok, but no recommendations will be returned)
      notes: |  # ★ 추가
        # Normal response
        The composite product information posted to the API will be split up and stored as separate product-info, recommendation and review entities.

        # Expected error responses
        1. If a product with the same productId as specified in the posted information already exists,
           an **422 - Unprocessable Entity** error with a "duplicate key" error message will be returned

    delete-composite-product:  # ★ 변경 (전: ## Non numerical product id)
      description: Deletes a product composite  # ★ 변경 (전: 400 - A **Bad Request** error will be returned)
      notes: |  # ★ 추가
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

logging:
  level:
    root: INFO
    se.magnus: DEBUG

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
```

문자열·블록 안이라 ★를 달지 못한 변경: `1. Product information` (★ 변경 (전: 1. Base product information)); `1. If no product information is found, a **404 - Not Foun...` (★ 변경 (전: In the following cases, only a partial response be created (used to...)); `1. In no recommendations or reviews are found for a produ...` (★ 추가); `# Normal response` (★ 추가); `The composite product information posted to the API will ...` (★ 추가); `# Expected error responses` (★ 변경 (전: ## Product id 213)); `1. If a product with the same productId as specified in t...` (★ 변경 (전: 200 - Ok, but no reviews will be returned)); `an **422 - Unprocessable Entity** error with a "duplicate...` (★ 추가); `# Normal response` (★ 추가); `Entities for product information, recommendations and rev...` (★ 추가); `The implementation of the delete method is idempotent, i....` (★ 추가); `This means that a delete request of a non-existing produc...` (★ 변경 (전: ## Product id 13)); 삭제 (3줄, 첫 줄: 404 - A **Not Found** error will be returned)

## 3-4. `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java`
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;  // ★ 변경 (전: import static org.springframework.http.HttpStatus.UNPROCESSABLE_CON...)
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;  // ★ 추가

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;  // ★ 추가
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.composite.product.ProductAggregate;  // ★ 추가
import se.magnus.api.composite.product.RecommendationSummary;  // ★ 추가
import se.magnus.api.composite.product.ReviewSummary;  // ★ 추가
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

  @Autowired private WebTestClient client;

  @MockitoBean private ProductCompositeIntegration compositeIntegration;

  @BeforeEach
  void setUp() {

    when(compositeIntegration.getProduct(PRODUCT_ID_OK))
      .thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));
    when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
      .thenReturn(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
    when(compositeIntegration.getReviews(PRODUCT_ID_OK))
      .thenReturn(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));

    when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND))
      .thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

    when(compositeIntegration.getProduct(PRODUCT_ID_INVALID))
      .thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
  }

  @Test
  void contextLoads() {}

  @Test
  // ★ 추가 (21줄)
  void createCompositeProduct1() {

    ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1, null, null, null);

    postAndVerifyProduct(compositeProduct, OK);
  }

  @Test
  void createCompositeProduct2() {
    ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
      singletonList(new RecommendationSummary(1, "a", 1, "c")),
      singletonList(new ReviewSummary(1, "a", "s", "c")), null);

    postAndVerifyProduct(compositeProduct, OK);
  }

  @Test
  void deleteCompositeProduct() {
    ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
      singletonList(new RecommendationSummary(1, "a", 1, "c")),
      singletonList(new ReviewSummary(1, "a", "s", "c")), null);

    postAndVerifyProduct(compositeProduct, OK);

    deleteAndVerifyProduct(compositeProduct.getProductId(), OK);
    deleteAndVerifyProduct(compositeProduct.getProductId(), OK);
  }

  @Test
  void getProductById() {

    // ★ 변경 (4줄, 전: 10줄)
    getAndVerifyProduct(PRODUCT_ID_OK, OK)
      .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
      .jsonPath("$.recommendations.length()").isEqualTo(1)
      .jsonPath("$.reviews.length()").isEqualTo(1);
  }

  @Test
  void getProductNotFound() {

    // ★ 변경 (3줄, 전: 9줄)
    getAndVerifyProduct(PRODUCT_ID_NOT_FOUND, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
      .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
  }

  @Test
  void getProductInvalidInput() {

    // ★ 변경 (7줄, 전: 2줄)
    getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_CONTENT)
      .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
      .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return client.get()
      .uri("/product-composite/" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)  // ★ 변경 (전: .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT))
      .expectHeader().contentType(APPLICATION_JSON)
      // ★ 변경 (14줄, 전: 3줄)
      .expectBody();
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

다음: [04-build-compose-test.md](04-build-compose-test.md)
