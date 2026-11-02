# 06. 복합 서비스 (product-composite-service)

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

## 1. 준비 (다른 서비스와 같음)
- `build.gradle`: `jar { enabled = false }`, `implementation project(':api')`, `implementation project(':util')`
- api의 `composite/product/` 클래스 5개: `ProductCompositeService`(인터페이스, 교재 그대로), 나머지 4개 데이터 클래스는 롬복

## 2. `application.yml` (교재 '프로퍼티' 절)
교재의 `server.port: 7001`로 시작하는 yml은 product-service가 아니라 **composite용**이다. 핵심 서비스 주소를 코드에 하드코딩하지 않고 설정에서 읽는다.
```yaml
server.port: 7001
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true    # Boot 4: 아래 4번 참고

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

## 3. `ProductCompositeIntegration.java`: Jackson 3 변경점
교재 소스를 가져오되 두 군데를 바꾼다. Boot 4의 클래스패스에는 Jackson 3(`tools.jackson`)만 있고 Jackson 2(`com.fasterxml.jackson.databind`)는 없다.
```java
// import com.fasterxml.jackson.databind.ObjectMapper;   ✗ 교재
// import java.io.IOException;                           ✗ 삭제
import tools.jackson.core.JacksonException;              // ✓
import tools.jackson.databind.ObjectMapper;              // ✓

private String getErrorMessage(HttpClientErrorException ex) {
  try {
    return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
  } catch (JacksonException ioex) {      // 교재: IOException. Jackson 3의 readValue는 IOException을 던지지 않음
    return ex.getMessage();
  }
}
```
- `switch` 안의 `case UNPROCESSABLE_ENTITY:`도 `UNPROCESSABLE_CONTENT`로 바꾼다.
- `RestTemplate`은 Spring 7에서도 deprecated가 아니라 그대로 쓴다. (교재 7장에서 WebClient로 바꾼다.)

## 4. final 필드 역직렬화 문제 (Boot 4에서만 발생)
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
- 게다가 `new RestTemplate()`은 Spring Boot가 설정한 Jackson이 아니라 **자기만의 기본 Jackson**을 쓴다. 그래서 yml 설정만으로는 해결되지 않는다.
- 롬복 때문이 아니다. 교재 원본 코드를 그대로 써도 Boot 4에서는 똑같이 발생한다.

### 해결 (composite 두 군데)
① 메인 클래스: RestTemplate이 Spring Boot의 JsonMapper를 쓰게 한다.
```java
import java.util.List;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;

@ComponentScan("se.magnus")
@SpringBootApplication
public class ProductCompositeServiceApplication {

  @Bean
  RestTemplate restTemplate(JsonMapper jsonMapper) {
    return new RestTemplate(List.of(new JacksonJsonHttpMessageConverter(jsonMapper)));
  }
  ...
}
```
② yml: Spring Boot의 Jackson에 Jackson 2의 동작을 다시 켠다.
```yaml
spring.jackson.mapper.allow-final-fields-as-mutators: true
```
이 설정은 Integration에 주입되는 ObjectMapper(오류 응답 `HttpErrorInfo`를 읽을 때 사용)에도 적용된다. 그래서 404·422의 `message`도 composite까지 제대로 전달된다.

## 5. `ProductCompositeServiceImpl.java`
교재 소스 그대로 쓴다. 핵심 서비스 결과를 `ProductAggregate`로 합치고 `serviceAddresses`에 4개 서비스의 주소를 담는다.
Recommendation·Review가 비어 있을 때 이 파일에서 `getRecommendationId()`, `getReviewId()`에 `cannot find symbol` 오류가 났다. api 데이터 클래스를 먼저 채우면 해결된다.

다음: [07-test.md](07-test.md)
