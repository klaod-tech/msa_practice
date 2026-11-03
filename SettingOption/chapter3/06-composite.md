# 06. 복합 서비스 (product-composite-service)

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄, `★ 삭제` = 지우는 줄, 표시 없는 줄 = 이미 있는 줄(넣을 위치 확인용)

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
`code microservices/product-composite-service/build.gradle` 들어가서 **두 군데 추가**
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar { enabled = false }                // ★ 추가 ① (한 줄로 써도 같음, dependencies 블록 밖에)
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
	implementation project(':api')     // ★ 추가 ②
	implementation project(':util')    // ★ 추가 ②
}

tasks.named('test') {
	useJUnitPlatform()
}
```
처음에 `jar { enabled = false }`를 `dependencies { }` **안에** 넣었었다. 동작은 했지만 위치가 틀렸으므로 블록 밖으로 옮겼다.

## 2. 메인 클래스 `ProductCompositeServiceApplication.java` 수정
`code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplication.java` 들어가서 **import 6줄, 애너테이션 1줄, RestTemplate 빈 메서드 1개 추가**
빈 메서드는 `main()` 메서드 **아래, 클래스를 닫는 `}` 위**에 넣는다.
```java
package se.magnus.microservices.composite.product;

import java.util.List;                                                          // ★ 추가 (Boot 4 대응)

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;                             // ★ 추가
import org.springframework.context.annotation.ComponentScan;                    // ★ 추가
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter; // ★ 추가 (Boot 4 대응)
import org.springframework.web.client.RestTemplate;                             // ★ 추가

import tools.jackson.databind.json.JsonMapper;                                  // ★ 추가 (Boot 4 대응)

@ComponentScan("se.magnus")                                                     // ★ 추가
@SpringBootApplication
public class ProductCompositeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}

	@Bean                                                                       // ★ 추가
	RestTemplate restTemplate(JsonMapper jsonMapper) {                          // ★ 추가 (교재: restTemplate())
		return new RestTemplate(List.of(new JacksonJsonHttpMessageConverter(jsonMapper)));   // ★ 추가 (교재: return new RestTemplate();)
	}                                                                           // ★ 추가
}
```
교재는 `RestTemplate restTemplate() { return new RestTemplate(); }`이다. Boot 4에서 이대로 쓰면 응답 값이 전부 null/0이 된다 → 아래 [5. final 필드 역직렬화 문제](#5-final-필드-역직렬화-문제-boot-4에서만-발생) 참고.

## 3. `application.properties` 삭제 → `application.yml` (새 파일, 전체)
교재 '프로퍼티' 절의 `server.port: 7001`로 시작하는 yml은 product-service가 아니라 **composite용**이다. 핵심 서비스 주소를 코드에 하드코딩하지 않고 설정에서 읽는다.
```bash
rm microservices/product-composite-service/src/main/resources/application.properties
```
`code microservices/product-composite-service/src/main/resources/application.yml` 들어가서 아래 내용 전체 입력
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

## 4. `services/ProductCompositeIntegration.java` (새 파일, 교재 소스 + 수정)
```bash
mkdir -p microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services
cp $BOOK_HOME/Chapter03/2-basic-rest-services/microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/
```
`code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeIntegration.java` 들어가서 아래 **세 군데 수정** (직접 입력하려면 `cp` 없이 열어서 교재 소스를 보며 작성)
`code`로 열린 탭은 반드시 `Ctrl+S`로 저장한다. 저장하지 않으면 디스크에 파일이 없어서, VS Code에 오류도 안 뜨고 Gradle도 이 파일을 모른다.
복사한 뒤 **세 군데를 고친다.** Boot 4의 클래스패스에는 Jackson 3(`tools.jackson`)만 있고 Jackson 2(`com.fasterxml.jackson.databind`)는 없다.

### ① import (파일 맨 위, 3~8번 줄 근처)
```java
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.ObjectMapper;      // ★ 삭제
import java.io.IOException;                              // ★ 삭제
import tools.jackson.core.JacksonException;              // ★ 추가
import tools.jackson.databind.ObjectMapper;              // ★ 추가
import java.util.ArrayList;
import java.util.List;
```
`JacksonException` import를 두 번 넣은 적이 있다. 중복 import는 오류는 아니지만 하나만 남긴다.

### ② `getProduct()`의 `switch` (70~81번 줄 근처)
```java
    } catch (HttpClientErrorException ex) {

      switch (HttpStatus.resolve(ex.getStatusCode().value())) {
        case NOT_FOUND:
          throw new NotFoundException(getErrorMessage(ex));

        case UNPROCESSABLE_CONTENT:                      // ★ 변경 (교재: UNPROCESSABLE_ENTITY)
          throw new InvalidInputException(getErrorMessage(ex));

        default:
```

### ③ `getErrorMessage()` (85~91번 줄 근처)
```java
  private String getErrorMessage(HttpClientErrorException ex) {
    try {
      return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
    } catch (JacksonException ioex) {                    // ★ 변경 (교재: IOException)
      return ex.getMessage();
    }
  }
```
Jackson 3의 `readValue()`는 `IOException`을 던지지 않는다. 교재대로 두면 `exception IOException is never thrown in body of corresponding try statement` 컴파일 오류가 난다.

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
방법 1 — `code microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java` 들어가서 교재 소스를 보며 작성

방법 2 — 복사
```bash
cp $BOOK_HOME/Chapter03/2-basic-rest-services/microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/ProductCompositeServiceImpl.java microservices/product-composite-service/src/main/java/se/magnus/microservices/composite/product/services/
```
핵심 서비스 결과를 `ProductAggregate`로 합치고 `serviceAddresses`에 4개 서비스의 주소를 담는다.
api의 Recommendation·Review가 비어 있으면 이 파일에서 `getRecommendationId()`, `getReviewId()`에 `cannot find symbol` 오류가 난다.

## 확인
```bash
./gradlew :microservices:product-composite-service:compileJava     # BUILD SUCCESSFUL
```

다음: [07-test.md](07-test.md)
