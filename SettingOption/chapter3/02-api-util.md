# 02. api, util 프로젝트 추가

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄, 표시 없는 줄 = 이미 있는 줄(넣을 위치 확인용)

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

---

## 1. 루트 `settings.gradle` 수정
```bash
code settings.gradle
```
**맨 아래에** 두 줄 추가
교재 본문에는 없고 소스에만 있다. 빠뜨리면 `Project with path ':api' could not be found` 오류가 난다.
```groovy
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
include ':api'                                     // ★ 추가
include ':util'                                    // ★ 추가
```

## 2. 폴더 만들기
api와 util은 **루트 바로 아래**, `microservices`와 나란히 둔다. 서비스 폴더 안에 두지 않는다.
```bash
cd ~/msa
mkdir -p api/src/main/java/se/magnus/api/core/product
mkdir -p api/src/main/java/se/magnus/api/core/recommendation
mkdir -p api/src/main/java/se/magnus/api/core/review
mkdir -p api/src/main/java/se/magnus/api/composite/product
mkdir -p util/src/main/java/se/magnus/util/http
```

완성 후 구조:
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

---

## 3. `api/build.gradle` (새 파일, 전체)
```bash
code api/build.gradle
```
아래 내용 전체 입력

교재 소스(`$BOOK_HOME/Chapter03/2-basic-rest-services/api/build.gradle`)를 가져와 **Java 24 → 21, Boot 3.5.0 → 4.0.9**로 바꾸고 롬복 4줄을 추가했다.
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.api'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)          // ★ 변경 (교재: 24)
    }
}

repositories {
    mavenCentral()
}

ext {
    springBootVersion = '4.0.9'                               // ★ 변경 (교재: '3.5.0')
}

dependencies {
    implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    compileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")          // ★ 추가 (롬복)
    annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")  // ★ 추가 (롬복)
    compileOnly 'org.projectlombok:lombok'                                                                  // ★ 추가 (롬복)
    annotationProcessor 'org.projectlombok:lombok'                                                          // ★ 추가 (롬복)
}

tasks.named('test') {
    useJUnitPlatform()
}
```
- `platform(...)`: boot 플러그인이 없으므로, Boot 4.0.9에 맞는 의존성 버전 목록(BOM)을 직접 지정한다.
- `implementation platform`은 `implementation`에만 적용된다. 롬복 버전도 정하려면 `compileOnly`와 `annotationProcessor`에도 platform을 넣는다.

## 4. `util/build.gradle` (새 파일, 전체)
```bash
code util/build.gradle
```
아래 내용 전체 입력

api/build.gradle을 복사한 뒤 **group 한 줄 변경 + `project(':api')` 한 줄 추가**.
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.util'                        // ★ 변경 (api → util)
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
    implementation project(':api')                            // ★ 추가 (util → api: 핸들러가 api의 예외 클래스를 씀)
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

build.gradle을 저장한 뒤 VS Code에서 그 파일을 열고 `Shift+Alt+U`(Java: Reload Projects)를 눌러야 오류 표시와 자동 import가 동작한다.

---

## 5. 데이터 클래스: 롬복으로 작성 (교재와 다름, 새 파일)
교재는 생성자 2개와 게터를 직접 쓰지만, 롬복 애너테이션 3개로 같은 코드를 만든다.

### `api/.../core/product/Product.java` (전체)
```bash
code api/src/main/java/se/magnus/api/core/product/Product.java
```
아래 내용 전체 입력

```java
package se.magnus.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter                              // 모든 필드의 게터
@AllArgsConstructor                  // 필드 순서대로 받는 생성자
@NoArgsConstructor(force = true)     // 기본 생성자 (final 필드를 0/null로 채움)
public class Product {
    private final int productId;
    private final String name;
    private final int weight;
    private final String serviceAddress;
}
```
- 필드가 `final`이라 세터는 없다.
- `force = true`가 없으면 final 필드 때문에 기본 생성자를 만들 수 없어 컴파일 오류가 난다.

### 나머지 데이터 클래스 (같은 패턴)
`package`와 `import`, 애너테이션 3개는 Product와 같고 **필드만 다르다.** 필드 순서는 교재와 똑같이 맞춘다. `@AllArgsConstructor`가 필드 순서대로 생성자를 만들기 때문에, 순서가 다르면 `new Recommendation(productId, 1, "Author 1", ...)`가 값을 엉뚱한 필드에 넣는다.

| 파일 | package | 필드 (순서대로) |
|---|---|---|
| `core/recommendation/Recommendation.java` | `se.magnus.api.core.recommendation` | `int productId, int recommendationId, String author, int rate, String content, String serviceAddress` |
| `core/review/Review.java` | `se.magnus.api.core.review` | `int productId, int reviewId, String author, String subject, String content, String serviceAddress` |
| `composite/product/ProductAggregate.java` | `se.magnus.api.composite.product` | `int productId, String name, int weight, List<RecommendationSummary> recommendations, List<ReviewSummary> reviews, ServiceAddresses serviceAddresses` (+ `import java.util.List;`) |
| `composite/product/RecommendationSummary.java` | 〃 | 교재 소스의 필드 순서대로 |
| `composite/product/ReviewSummary.java` | 〃 | 교재 소스의 필드 순서대로 |
| `composite/product/ServiceAddresses.java` | 〃 | 교재 소스의 필드 순서대로 |

```bash
code api/src/main/java/se/magnus/api/core/recommendation/Recommendation.java
code api/src/main/java/se/magnus/api/core/review/Review.java
code api/src/main/java/se/magnus/api/composite/product/ProductAggregate.java
code api/src/main/java/se/magnus/api/composite/product/RecommendationSummary.java
code api/src/main/java/se/magnus/api/composite/product/ReviewSummary.java
code api/src/main/java/se/magnus/api/composite/product/ServiceAddresses.java
```
열린 파일마다 작성

---

## 6. 인터페이스 (새 파일, 교재 소스 그대로)
REST 매핑을 인터페이스에 붙이므로 구현 클래스에는 `@RestController`만 있으면 된다. `class`가 아니라 `interface`다.

### `api/.../core/product/ProductService.java` (전체)
```bash
code api/src/main/java/se/magnus/api/core/product/ProductService.java
```
아래 내용 전체 입력

```java
package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface ProductService {
    @GetMapping(
        value = "/product/{productId}",
        produces = "application/json"
    )
    Product getProduct(@PathVariable int productId);
}
```

### 나머지 인터페이스 (교재 소스 그대로)
방법 1 — 직접 작성 (교재 소스를 보며 입력)
```bash
code api/src/main/java/se/magnus/api/core/recommendation/RecommendationService.java
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
code api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java
```
열린 파일마다 작성

방법 2 — 복사 (교재 소스 파일을 그대로 가져옴)
```bash
cd ~/msa
B=$BOOK_HOME/Chapter03/2-basic-rest-services/api/src/main/java/se/magnus/api
cp $B/core/recommendation/RecommendationService.java api/src/main/java/se/magnus/api/core/recommendation/
cp $B/core/review/ReviewService.java                 api/src/main/java/se/magnus/api/core/review/
cp $B/composite/product/ProductCompositeService.java api/src/main/java/se/magnus/api/composite/product/
```
교재 소스를 옆에 띄워 보려면 `code $B/core/review/ReviewService.java`처럼 연다.

---

## 7. `util/.../http/ServiceUtil.java` (새 파일, 교재 소스 그대로)
본문에는 코드가 없고 "`$BOOK_HOME/Chapter03/2-basic-rest-services/util`에서 확인"하라고만 되어 있다.
방법 1 — 아래 파일을 열어서 교재 소스를 보며 작성
```bash
code util/src/main/java/se/magnus/util/http/ServiceUtil.java
```

방법 2 — 복사
```bash
cp $BOOK_HOME/Chapter03/2-basic-rest-services/util/src/main/java/se/magnus/util/http/ServiceUtil.java \
   util/src/main/java/se/magnus/util/http/ServiceUtil.java
```
파일 이름 오타(`SericeUtil`) 주의. 파일 이름과 클래스 이름이 다르면 컴파일 오류가 난다.
| 부분 | 역할 |
|---|---|
| `@Component` | 스프링 빈으로 등록해 각 ServiceImpl에 주입되게 한다 |
| 생성자의 `@Value("${server.port}")` | yml의 포트를 받아 저장한다 |
| `getServiceAddress()` | `호스트명/IP:포트` 문자열을 돌려준다 (응답의 `serviceAddress`) |

본문에 나오는 util의 나머지 클래스(`GlobalControllerExceptionHandler`, `HttpErrorInfo`)는 [04](04-error-handling.md)에서 만든다.

## 확인
```bash
./gradlew :api:compileJava :util:compileJava     # BUILD SUCCESSFUL
```

다음: [03-product-service.md](03-product-service.md)
