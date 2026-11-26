# 05. recommendation, review 서비스 구현

> 표기: 한 줄만 바뀌면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`. 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 지우고 바꿀 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(`★ 지움:` 줄을 지운다). `★ 참고:` = 교재와 다른 이유. 표시 없는 줄 = 이미 있는 줄. ★ 주석은 입력하지 않아도 된다.

product-service([03](03-product-service.md))와 **똑같은 패턴**이다. 예외 클래스와 util이 이미 있으므로 ServiceImpl은 교재와 같은 코드다(아래 전체 코드).
절마다 recommendation, review 파일을 차례로 만든다(review는 포트 `7004`).

## 0. 사전 확인: api 쪽 (02에서 만듦)
- `api/.../core/recommendation/Recommendation.java` (롬복, 필드 6개)
- `api/.../core/recommendation/RecommendationService.java` (교재와 같음)

비어 있으면 composite의 ServiceImpl에서 `getRecommendationId()` 등에 `cannot find symbol` 오류가 난다.

---

## 1. `build.gradle` 수정 (recommendation, review)
group은 `spring init`이 이미 서비스별로 만들어 두었으므로 그대로 둔다. 두 파일은 `group` 줄만 다르다.

### `microservices/recommendation-service/build.gradle`
```bash
code microservices/recommendation-service/build.gradle
```
**두 군데 추가**
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
// ★ 추가 시작 (3줄)
jar {
	enabled = false
}
// ★ 추가 끝
group = 'se.magnus.microservices.core.recommendation'
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
	// ★ 추가 시작 (2줄)
	implementation project(':api')
	implementation project(':util')
	// ★ 추가 끝
}

tasks.named('test') {
	useJUnitPlatform()
}
```

### `microservices/review-service/build.gradle`
```bash
code microservices/review-service/build.gradle
```
**두 군데 추가**
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
// ★ 추가 시작 (3줄)
jar {
	enabled = false
}
// ★ 추가 끝
group = 'se.magnus.microservices.core.review'
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
	// ★ 추가 시작 (2줄)
	implementation project(':api')
	implementation project(':util')
	// ★ 추가 끝
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 2. 메인 클래스 수정 (recommendation, review)
### `RecommendationServiceApplication.java`
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java
```
**import 한 줄, 애너테이션 한 줄 추가**
```java
package se.magnus.microservices.core.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;  // ★ 추가

@ComponentScan("se.magnus")  // ★ 추가
@SpringBootApplication
public class RecommendationServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(RecommendationServiceApplication.class, args);
	}

}
```

### `ReviewServiceApplication.java`
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java
```
**import 한 줄, 애너테이션 한 줄 추가**
```java
package se.magnus.microservices.core.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;  // ★ 추가

@ComponentScan("se.magnus")  // ★ 추가
@SpringBootApplication
public class ReviewServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReviewServiceApplication.class, args);
	}

}
```

빠뜨리면 빌드의 테스트 단계에서 `contextLoads() FAILED`(`No qualifying bean ... ServiceUtil`)가 난다. review에서 실제로 빠뜨렸었다.

## 3. `services/RecommendationServiceImpl.java`, `ReviewServiceImpl.java` (새 파일, 교재와 같음)
```bash
mkdir -p microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services
mkdir -p microservices/review-service/src/main/java/se/magnus/microservices/core/review/services
```

### `recommendation-service/.../services/RecommendationServiceImpl.java` (전체)
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java
```
아래 내용 전체 입력

```java
package se.magnus.microservices.core.recommendation.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private final ServiceUtil serviceUtil;

    public RecommendationServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        if (productId == 113) {
            LOG.debug("No recommendations found for productId: {}", productId);
            return new ArrayList<>();
        }

        List<Recommendation> list = new ArrayList<>();
        list.add(new Recommendation(productId, 1, "Author 1", 1, "Content 1", serviceUtil.getServiceAddress()));
        list.add(new Recommendation(productId, 2, "Author 2", 2, "Content 2", serviceUtil.getServiceAddress()));
        list.add(new Recommendation(productId, 3, "Author 3", 3, "Content 3", serviceUtil.getServiceAddress()));

        LOG.debug("/recommendation response size: {}", list.size());

        return list;
    }
}
```
- 클래스 위 **`@RestController`를 빠뜨리지 않는다.** 없으면 컴파일·실행은 되지만 `/recommendation` 주소가 등록되지 않아, composite 응답에서 추천이 `[]`로 나온다(review도 같다).
- productId가 1보다 작으면 `InvalidInputException`(422), **113**이면 빈 목록(추천 없음 테스트용), 그 밖에는 가짜 추천 3개를 돌려준다.
- `ServiceUtil`은 생성자로 주입받는다(위 2절의 `@ComponentScan` 덕분).

### `review-service/.../services/ReviewServiceImpl.java` (전체)
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/services/ReviewServiceImpl.java
```
아래 내용 전체 입력

```java
package se.magnus.microservices.core.review.services;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;

    public ReviewServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public List<Review> getReviews(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        if (productId == 213) {
            LOG.debug("No reviews found for productId: {}", productId);
            return new ArrayList<>();
        }

        List<Review> list = new ArrayList<>();
        list.add(new Review(productId, 1, "Author 1", "Subject 1", "Content 1", serviceUtil.getServiceAddress()));
        list.add(new Review(productId, 2, "Author 2", "Subject 2", "Content 2", serviceUtil.getServiceAddress()));
        list.add(new Review(productId, 3, "Author 3", "Subject 3", "Content 3", serviceUtil.getServiceAddress()));

        LOG.debug("/reviews response size: {}", list.size());

        return list;
    }
}
```
- RecommendationServiceImpl과 같은 모양이다. 빈 목록이 되는 번호가 **213**이고, `Review`를 만든다.

## 4. `application.properties` 삭제 → `application.yml` (새 파일, 전체)
```bash
rm microservices/recommendation-service/src/main/resources/application.properties
rm microservices/review-service/src/main/resources/application.properties
```

### `recommendation-service/.../application.yml` (전체)
```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
아래 내용 전체 입력

```yaml
spring.application.name: recommendation-service
server.port: 7003
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
- 이름 `recommendation-service`, 포트 **7003**.

### `review-service/.../application.yml` (전체)
```bash
code microservices/review-service/src/main/resources/application.yml
```
아래 내용 전체 입력

```yaml
spring.application.name: review-service
server.port: 7004
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
- 이름 `review-service`, 포트 **7004**.

product의 yml을 복사해서 **이름과 포트를 바꾸지 않는 실수**를 했었다(review가 recommendation-service / 7003으로 남음). 그러면 81쪽에서 `Port 7003 was already in use`가 나고, composite가 7004에서 review를 찾지 못한다.

---

## 특수 케이스 (테스트에 쓰임)
| productId | recommendation | review |
|---|---|---|
| `< 1` | 422 (InvalidInputException) | 422 |
| `113` | 빈 목록 | 정상 3개 |
| `213` | 정상 3개 | 빈 목록 |

## 확인
```bash
./gradlew build      # 4개 서비스 모두 BUILD SUCCESSFUL
```

다음: [06-composite.md](06-composite.md)
