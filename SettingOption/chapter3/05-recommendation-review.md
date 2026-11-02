# 05. recommendation, review 서비스 구현

product-service와 **똑같은 패턴**이다. 예외 클래스와 util이 이미 있으므로 교재 소스를 거의 그대로 쓸 수 있다.

## 1. api (두 서비스가 공통으로 씀)
| 파일 | 방법 |
|---|---|
| `core/recommendation/Recommendation.java` | 롬복 (Product와 같은 애너테이션 3개), 필드 6개를 교재 순서대로 |
| `core/review/Review.java` | 같은 방식 |
| `core/recommendation/RecommendationService.java` | 교재 소스 그대로 (`@GetMapping("/recommendation")` + `@RequestParam productId`) |
| `core/review/ReviewService.java` | 교재 소스 그대로 (`/review?productId=`) |

필드 순서:
- Recommendation: `productId, recommendationId, author, rate, content, serviceAddress`
- Review: `productId, reviewId, author, subject, content, serviceAddress`

## 2. 서비스별 작업 (recommendation과 review 각각)
| 파일 | 할 일 |
|---|---|
| `build.gradle` | `jar { enabled = false }` (plugins 아래), `implementation project(':api')`, `implementation project(':util')` |
| `XxxServiceApplication.java` | `@ComponentScan("se.magnus")` |
| `services/XxxServiceImpl.java` | 교재 소스 그대로 |
| `application.properties` | 삭제 |
| `application.yml` | product의 yml 복사 후 이름·포트 변경 |

```bash
cd ~/msa
mkdir -p microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/services/RecommendationServiceImpl.java
rm microservices/recommendation-service/src/main/resources/application.properties
code microservices/recommendation-service/src/main/resources/application.yml
```
review는 경로의 `recommendation`을 `review`로 바꾼다.

## 3. yml
```yaml
spring.application.name: recommendation-service   # review: review-service
server.port: 7003                                  # review: 7004
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
복사하고 **이름과 포트를 바꾸지 않는 실수**를 했었다. 그대로 두면 81쪽에서 recommendation과 포트 7003이 겹쳐 `Port 7003 was already in use`가 나고, composite가 7004에서 review를 찾지 못한다.

## 4. 교재 소스의 특수 케이스 (테스트용)
| productId | recommendation | review |
|---|---|---|
| `< 1` | 422 (InvalidInputException) | 422 |
| `113` | 빈 목록 | 정상 |
| `213` | 정상 | 빈 목록 |

## 5. 확인
```bash
./gradlew build      # 4개 서비스 모두 BUILD SUCCESSFUL
```
`@ComponentScan`을 빠뜨리면 빌드의 테스트 단계에서 `contextLoads() FAILED`(`No qualifying bean ... ServiceUtil`)가 난다.

다음: [06-composite.md](06-composite.md)
