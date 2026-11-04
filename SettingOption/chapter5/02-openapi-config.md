# 02. OpenAPI 구성 (Application 클래스 + application.yml)

> 표기: `★` = 교재와 다른 줄 (괄호 안은 교재 원래 값)

product-composite-service에서 springdoc-openapi를 활성화하려면 두 파일을 바꾼다.
- `ProductCompositeServiceApplication.java`: API 문서의 공통 정보(제목, 버전, 연락처 등)를 담은 **`OpenAPI` 빈**
- `application.yml`: springdoc 설정 + 문서에 들어갈 **문구(`api:` 블록)**

---

## 1. ProductCompositeServiceApplication.java (전체)
`code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java` 들어가서 전체를 아래로 교체

교재 5장 소스 + 4장에서 Boot 4 때문에 바꾼 RestTemplate 빈(JsonMapper 버전)을 합친 것이다.
```java
package se.magnus.microservices.composite.product;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import tools.jackson.databind.json.JsonMapper;

@ComponentScan("se.magnus")
@SpringBootApplication
public class ProductCompositeServiceApplication {

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

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}

	@Bean
	RestTemplate restTemplate(JsonMapper jsonMapper) {
		return new RestTemplate(List.of(new JacksonJsonHttpMessageConverter(jsonMapper)));
	}

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
}
```

| 부분 | 하는 일 |
|---|---|
| `@Value` 필드 11개 | application.yml의 `api.common.*` 값을 읽어 온다. 문구를 코드에 하드코딩하지 않기 위해서다 |
| `getOpenApiDocumentation()` | 그 값들로 `OpenAPI` 객체를 만들어 빈으로 등록한다. springdoc이 이 빈을 문서의 **공통 정보**(Info, Contact, License, 외부 문서)로 쓴다 |
| `restTemplate(JsonMapper)` | 4장 Boot 4 대응 그대로 (`new RestTemplate()` 아님) |

### 처음 작성할 때 실제로 틀렸던 곳
| 틀린 것 | 맞는 것 | 이유 |
|---|---|---|
| `.contract(new Contact()` | `.contact(` | 그런 메서드 없음 (오타) |
| `apiContactURl` | `apiContactUrl` | `@Value` 필드 이름과 글자까지 같아야 함 |
| `apiTermOfService` | `apiTermsOfService` | 위와 같음 |
| `new ExternalDocDesc()` | `new ExternalDocumentation()` | yml 키 이름(`externalDocDesc`)과 클래스 이름을 헷갈림 |
| import 누락 | `Info`, `ExternalDocumentation`, `Value` | |

---

## 2. application.yml (전체)
`code microservices/product-composite-service/src/main/resources/application.yml` 들어가서 전체를 아래로 교체
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
spring.web.error.include-message: always                      # ★ Boot 4 (교재: server.error.include-message)
spring.jackson.mapper.allow-final-fields-as-mutators: true    # ★ Boot 4 (교재에 없음, 4장 06에서 추가)

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
(`#` 뒤는 YAML 주석이라 그대로 붙여 넣어도 된다.)

### ⚠️ 교재 파일을 통째로 복사하면 Boot 4 설정 두 줄이 사라진다
교재 5장 yml을 그대로 복사했더니 4장까지 바꿔 둔 두 줄이 교재 값으로 돌아갔다. 위 전체 파일에는 다시 넣어 두었다.

| 줄 | 없으면 |
|---|---|
| `spring.web.error.include-message: always` (교재: `server.error.include-message`) | Boot 4에서 옛 이름은 조용히 무시 → 오류 응답의 `message`가 빈다 |
| `spring.jackson.mapper.allow-final-fields-as-mutators: true` | composite 응답 값이 **전부 null/0** (4장 06의 Jackson 3 final 필드 문제), 404·422 `message`도 전달 안 됨 |

앞으로 교재 파일을 통째로 가져올 때는 **이전 장에서 Boot 4 때문에 바꾼 곳이 덮어써지지 않는지** 확인한다.

### 5장에서 새로 들어간 부분
| 블록 | 하는 일 |
|---|---|
| `springdoc:` | Swagger UI 경로(`/openapi/swagger-ui.html`), API 문서 경로(`/openapi/v3/api-docs`), 문서를 만들 패키지(`packagesToScan`)와 경로(`pathsToMatch`) |
| `api.common` | `OpenAPI` 빈의 `@Value`가 읽는 공통 정보 |
| `api.responseCodes` | 응답 코드별 설명. 인터페이스의 `@ApiResponses`에서 `${...}`로 읽는다 |
| `api.product-composite.get-composite-product` | API 하나의 요약(`description`)과 상세 설명(`notes`, 마크다운). `@Operation`에서 읽는다 |

## 확인
```bash
./gradlew :microservices:product-composite-service:compileJava
```
`BUILD SUCCESSFUL`이면 된다. 실행해서 Swagger UI를 보는 건 인터페이스 애너테이션(다음 단계)까지 한 뒤에 한다.
