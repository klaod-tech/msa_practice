# 02. composite: springdoc, OpenAPI 빈, 설명 문구

> 시작: 교수님 `Chapter04` 상태 / 결과: 교수님 `Chapter05`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `build.gradle`: `springdoc-openapi-starter-webflux-ui`(문서 생성 + Swagger UI 화면). **교재와 다름**: 교재는 2.8.6, 교수님 코드는 Boot 4용 **3.0.3**.
- `ProductCompositeServiceApplication`: `OpenAPI` 빈. 문서 전체의 제목, 버전, 연락처, 라이선스 정보를 yml의 `api.common.*`에서 읽어 넣는다.
- `application.yml`: `springdoc:`(UI와 문서 경로를 `/openapi/...`로, 문서에 넣을 패키지와 경로)와 `api:`(위 어노테이션이 가져다 쓰는 문구).

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
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'  // ★ 추가

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
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

// ★ 추가 (6줄)
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductCompositeServiceApplication {
  // ★ 추가 (29줄)

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

  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }

  public static void main(String[] args) {
    SpringApplication.run(ProductCompositeServiceApplication.class, args);
  }

}
```

## 2-3. `microservices/product-composite-service/src/main/resources/application.yml`
```bash
code microservices/product-composite-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
springdoc:  # ★ 추가
  swagger-ui.path: /openapi/swagger-ui.html  # ★ 추가
  api-docs.path: /openapi/v3/api-docs  # ★ 추가
  packagesToScan: se.magnus.microservices.composite.product  # ★ 추가
  pathsToMatch: /**  # ★ 추가

api:  # ★ 추가

  common:  # ★ 추가
    version: 1.0.0  # ★ 추가
    title: Sample API  # ★ 추가
    description: Description of the API...  # ★ 추가
    termsOfService: MY TERMS OF SERVICE  # ★ 추가
    license: MY LICENSE  # ★ 추가
    licenseUrl: MY LICENSE URL  # ★ 추가

    externalDocDesc: MY WIKI PAGE  # ★ 추가
    externalDocUrl: MY WIKI URL  # ★ 추가
    contact:  # ★ 추가
      name: NAME OF CONTACT  # ★ 추가
      url: URL TO CONTACT  # ★ 추가
      email: contact@mail.com  # ★ 추가

  responseCodes:  # ★ 추가
    ok.description: OK  # ★ 추가
    badRequest.description: Bad Request, invalid format of the request. See response message for more information  # ★ 추가
    notFound.description: Not found, the specified id does not exist  # ★ 추가
    unprocessableEntity.description: Unprocessable entity, input parameters caused the processing to fail. See response message for more information  # ★ 추가

  product-composite:  # ★ 추가

    get-composite-product:  # ★ 추가
      description: Returns a composite view of the specified product id  # ★ 추가
      notes: |  # ★ 추가
        # Normal response
        If the requested product id is found the method will return information regarding:
        1. Base product information
        1. Reviews
        1. Recommendations
        1. Service Addresses\n(technical information regarding the addresses of the microservices that created the response)

        # Expected partial and error responses
        In the following cases, only a partial response be created (used to simplify testing of error conditions)

        ## Product id 113
        200 - Ok, but no recommendations will be returned

        ## Product id 213
        200 - Ok, but no reviews will be returned

        ## Non numerical product id
        400 - A **Bad Request** error will be returned

        ## Product id 13
        404 - A **Not Found** error will be returned

        ## Negative product ids
        422 - An **Unprocessable Entity** error will be returned

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

문자열·블록 안이라 ★를 달지 못한 변경: `# Normal response` (★ 추가); `If the requested product id is found the method will retu...` (★ 추가); `1. Base product information` (★ 추가); `1. Reviews` (★ 추가); `1. Recommendations` (★ 추가); `1. Service Addresses\n(technical information regarding th...` (★ 추가); `# Expected partial and error responses` (★ 추가); `In the following cases, only a partial response be create...` (★ 추가); `## Product id 113` (★ 추가); `200 - Ok, but no recommendations will be returned` (★ 추가); `## Product id 213` (★ 추가); `200 - Ok, but no reviews will be returned` (★ 추가); `## Non numerical product id` (★ 추가); `400 - A **Bad Request** error will be returned` (★ 추가); `## Product id 13` (★ 추가); `404 - A **Not Found** error will be returned` (★ 추가); `## Negative product ids` (★ 추가); `422 - An **Unprocessable Entity** error will be returned` (★ 추가)

다음: [03-build-compose-test.md](03-build-compose-test.md)
