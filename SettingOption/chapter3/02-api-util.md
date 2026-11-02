# 02. api, util 프로젝트 추가

## 핵심 개념
- **api**: 인터페이스(`ProductService` 등)와 데이터 클래스(`Product` 등)를 둔다. 여러 서비스가 함께 쓰는 "약속"이다.
- **util**: 여러 서비스가 함께 쓰는 유틸리티(`ServiceUtil`, 오류 처리 클래스)를 둔다.
- 둘 다 **실행하는 앱이 아니라 라이브러리**다. 그래서 `org.springframework.boot` 플러그인을 쓰지 않는다. 쓰면 main 클래스가 없어서 `bootJar`가 실패한다.
- 의존 방향은 한쪽으로만 흐른다. 반대 방향(api → util)으로 넣으면 순환 의존 오류가 난다.
  ```
  각 서비스 ──→ api
      │          ↑
      └───→ util ┘
  ```

## 1. 루트 `settings.gradle`에 등록
교재 본문에는 없고 소스에만 있다. 빠뜨리면 `Project with path ':api' could not be found` 오류가 난다.
```groovy
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
include ':api'
include ':util'
```

## 2. 폴더 구조
api와 util은 **루트 바로 아래**, `microservices`와 나란히 둔다. 서비스 폴더 안에 두지 않는다.
```
msa/
├── api/
│   ├── build.gradle
│   └── src/main/java/se/magnus/api/
│       ├── core/product/          Product, ProductService
│       ├── core/recommendation/   Recommendation, RecommendationService
│       ├── core/review/           Review, ReviewService
│       ├── composite/product/     ProductAggregate, ProductCompositeService, RecommendationSummary, ReviewSummary, ServiceAddresses
│       └── exceptions/            InvalidInputException, NotFoundException   (04에서 추가)
├── util/
│   ├── build.gradle
│   └── src/main/java/se/magnus/util/http/
│       ├── ServiceUtil.java
│       ├── HttpErrorInfo.java                     (04에서 추가)
│       └── GlobalControllerExceptionHandler.java  (04에서 추가)
├── microservices/
└── settings.gradle
```
폴더는 터미널에서 먼저 만들고, `code 경로`로 연 다음 저장한다. 중간 폴더가 없으면 저장에 실패한다.
```bash
cd ~/msa
mkdir -p api/src/main/java/se/magnus/api/core/product
code api/src/main/java/se/magnus/api/core/product/Product.java   # Ctrl+S 하는 순간 파일 생성
```

## 3. `api/build.gradle`
교재 소스를 가져오되 **Java 24 → 21, Boot 3.5.0 → 4.0.9**로 바꾸고 롬복을 추가했다.
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.api'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ext {
    springBootVersion = '4.0.9'
}

dependencies {
    implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    compileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}
```
- `platform(...)`: boot 플러그인이 없으므로, Boot 4.0.9에 맞는 의존성 버전 목록(BOM)을 직접 지정한다.
- `implementation platform`은 `implementation`에만 적용된다. 롬복 버전도 정하려면 `compileOnly`와 `annotationProcessor`에도 platform을 넣는다.

## 4. `util/build.gradle`
api/build.gradle과 같고 두 가지만 다르다.
```groovy
group = 'se.magnus.microservices.util'
...
dependencies {
    ...
    implementation project(':api')    // util → api (핸들러가 api의 예외 클래스를 씀)
}
```

## 5. 데이터 클래스: 롬복 사용 (교재와 다름)
Product, Recommendation, Review, composite의 ProductAggregate·RecommendationSummary·ReviewSummary·ServiceAddresses 모두 같은 패턴이다.
```java
package se.magnus.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Product {
    private final int productId;
    private final String name;
    private final int weight;
    private final String serviceAddress;
}
```
- 필드가 `final`이라 세터는 없고 게터만 있다.
- 기본 생성자는 JSON 역직렬화용이다. final 필드라 `force = true`가 있어야 0/null로 채운 생성자를 만들 수 있다.
- **필드 순서는 교재와 똑같이** 맞춘다. `@AllArgsConstructor`가 필드 순서대로 생성자를 만들기 때문에, 순서가 다르면 `new Recommendation(productId, 1, "Author 1", ...)`가 값을 엉뚱한 필드에 넣는다.
  - Recommendation: `productId, recommendationId, author, rate, content, serviceAddress`
  - Review: `productId, reviewId, author, subject, content, serviceAddress`

## 6. 인터페이스 (class가 아니라 interface)
교재 소스 그대로 쓴다. REST 매핑을 인터페이스에 붙이므로 구현 클래스에는 `@RestController`만 있으면 된다.
```java
public interface ProductService {
    @GetMapping(value = "/product/{productId}", produces = "application/json")
    Product getProduct(@PathVariable int productId);
}
```

## 7. util: `ServiceUtil.java`
본문에는 코드가 없고 "`$BOOK_HOME/Chapter03/2-basic-rest-services/util`에서 확인"하라고만 되어 있다. **교재 소스를 그대로 복사**했다.
| 부분 | 역할 |
|---|---|
| `@Component` | 스프링 빈으로 등록해 각 ServiceImpl에 주입되게 한다 |
| 생성자의 `@Value("${server.port}")` | yml의 포트를 받아 저장한다 |
| `getServiceAddress()` | `호스트명/IP:포트` 문자열을 돌려준다 (응답의 `serviceAddress`) |

본문에 나오는 util의 나머지 클래스(`GlobalControllerExceptionHandler`, `HttpErrorInfo`)는 [04](04-error-handling.md)에서 만든다.

## VS Code 주의
- build.gradle을 바꾼 뒤에는 그 파일을 열고 `Shift+Alt+U`(Java: Reload Projects)를 눌러야 오류 표시와 자동 import가 동작한다.
- 자동으로 하려면 원격(WSL) 설정에서 `java.configuration.updateBuildConfiguration`을 `automatic`으로 바꾼다.

다음: [03-product-service.md](03-product-service.md)
