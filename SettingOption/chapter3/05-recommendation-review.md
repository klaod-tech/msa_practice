# 05. recommendation, review 서비스 구현

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄, 표시 없는 줄 = 이미 있는 줄(넣을 위치 확인용)

product-service([03](03-product-service.md))와 **똑같은 패턴**이다. 예외 클래스와 util이 이미 있으므로 ServiceImpl은 교재 소스를 그대로 쓴다.
아래는 recommendation 기준이다. **review는 `recommendation` → `review`, 포트 `7003` → `7004`로 바꿔서 똑같이 한다.**

## 0. 사전 확인: api 쪽 (02에서 만듦)
- `api/.../core/recommendation/Recommendation.java` (롬복, 필드 6개)
- `api/.../core/recommendation/RecommendationService.java` (교재 소스 그대로)

비어 있으면 composite의 ServiceImpl에서 `getRecommendationId()` 등에 `cannot find symbol` 오류가 난다.

---

## 1. `microservices/recommendation-service/build.gradle` 수정
product-service와 같은 **두 군데 추가**. group은 `spring init`이 이미 `...core.recommendation`으로 만들어 두었으므로 그대로 둔다.
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar {                          // ★ 추가 ①
	enabled = false            // ★ 추가 ①
}                              // ★ 추가 ①
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
	implementation project(':api')     // ★ 추가 ②
	implementation project(':util')    // ★ 추가 ②
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 2. 메인 클래스 수정
위치: `microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java`
(review: `.../core/review/ReviewServiceApplication.java`)
```java
package se.magnus.microservices.core.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;      // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")                                         // ★ 추가
public class RecommendationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecommendationServiceApplication.class, args);
	}

}
```
빠뜨리면 빌드의 테스트 단계에서 `contextLoads() FAILED`(`No qualifying bean ... ServiceUtil`)가 난다. review에서 실제로 빠뜨렸었다.

## 3. `services/RecommendationServiceImpl.java` (새 파일, 교재 소스 그대로)
```bash
cd ~/msa
S=microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services
mkdir -p $S
code $S/RecommendationServiceImpl.java                                          # 방법 1 — 직접 작성
cp $BOOK_HOME/Chapter03/2-basic-rest-services/$S/RecommendationServiceImpl.java $S/   # 방법 2 — 복사
```
review:
```bash
cd ~/msa
S=microservices/review-service/src/main/java/se/magnus/microservices/core/review/services
mkdir -p $S
code $S/ReviewServiceImpl.java                                                  # 방법 1
cp $BOOK_HOME/Chapter03/2-basic-rest-services/$S/ReviewServiceImpl.java $S/     # 방법 2
```
`S=...`는 긴 경로를 줄여 쓰려고 만든 변수다. 같은 터미널에서만 유지된다.

## 4. `application.properties` 삭제 → `application.yml` (새 파일, 전체)
```bash
cd ~/msa
rm microservices/recommendation-service/src/main/resources/application.properties
cp microservices/product-service/src/main/resources/application.yml microservices/recommendation-service/src/main/resources/
code microservices/recommendation-service/src/main/resources/application.yml
```
product의 yml을 복사한 뒤 **두 줄을 바꾼다.**
```yaml
spring.application.name: recommendation-service     # ★ 변경 (product-service → recommendation-service / review: review-service)
server.port: 7003                                    # ★ 변경 (7002 → 7003 / review: 7004)
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
복사하고 **이름과 포트를 바꾸지 않는 실수**를 했었다(review가 recommendation-service / 7003으로 남음). 그대로 두면 81쪽에서 `Port 7003 was already in use`가 나고, composite가 7004에서 review를 찾지 못한다.

---

## 교재 소스의 특수 케이스 (테스트에 쓰임)
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
