# 00. 배경: 3장에서 무엇을 만드나

> 코드 없이 읽기만 하는 문서. 3장 전체를 한눈에 보는 요약.

## 1. 시작 상태 (환경 설정까지)
[../setting/README.md](../setting/README.md)에서 Java 21, Gradle, VS Code만 준비했다. 코드는 아직 없다.

## 2. 3장이 끝나면
```
                   GET /product-composite/1  (포트 7001)
 curl / 브라우저 ───────────────────────────▶ product-composite
                                                │ RestTemplate (HTTP)
                    ┌───────────────────────────┼───────────────────────────┐
                    ▼                           ▼                           ▼
             product (7002)          recommendation (7003)            review (7004)
             GET /product/1          GET /recommendation?productId=1  GET /review?productId=1
```
- **서비스 4개**가 각각 따로 실행되는 스프링 부트 앱이다. 아직 DB가 없어서 **고정된 가짜 데이터**를 돌려준다.
- composite는 세 핵심 서비스를 HTTP로 불러서 결과를 **하나로 합쳐** 돌려준다.
- 터미널마다 `java -jar ...`로 하나씩 띄워서 `localhost:포트`로 부른다(도커는 4장).

## 3. 프로젝트 구성
| 프로젝트 | 종류 | 하는 일 |
|---|---|---|
| `api` | 라이브러리 | 서비스끼리 주고받는 **API 인터페이스와 데이터 클래스**(`Product`, `ProductService` 등), 예외 클래스 |
| `util` | 라이브러리 | 공통 코드: `ServiceUtil`(응답에 넣을 서비스 주소), `GlobalControllerExceptionHandler`(예외 → HTTP 상태 코드) |
| `microservices/product-service` 등 3개 | 앱 | 핵심 서비스. api 인터페이스를 구현하는 `@RestController` |
| `microservices/product-composite-service` | 앱 | 세 서비스를 부르는 `ProductCompositeIntegration` + 합치는 `ProductCompositeServiceImpl` |

루트 `settings.gradle`이 6개 프로젝트를 묶어서 `./gradlew build` 한 번으로 전부 빌드한다.

## 4. 각 문서에서 생기는 것
| 문서 | 이전 상태 → 이후 상태 |
|---|---|
| 01 | 빈 폴더 → 서비스 4개의 **뼈대**(`create-projects.bash`의 `spring init`으로 만든 빈 앱) |
| 02 | 뼈대만 → `api`, `util` 라이브러리 추가, API 인터페이스와 데이터 클래스 |
| 03 | 인터페이스만 → product가 **실제로 응답**(`curl localhost:7002/product/1`) |
| 04 | 오류 처리 없음 → **404(없음, 예: productId 13), 422(잘못된 값, 예: 음수)** 로 구분해서 응답 |
| 05 | product만 → recommendation, review도 응답 |
| 06 | 핵심 서비스만 → composite가 셋을 불러 **합친 결과** 응답 |
| 07 | 손으로 curl → **JUnit 테스트 + `test-em-all.bash`** 로 자동 확인 |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 핵심(core) 서비스 | product, recommendation, review. 자기 데이터만 다룬다 |
| 복합(composite) 서비스 | 여러 핵심 서비스를 불러 결과를 합치는 서비스 |
| 통합 계층(Integration) | composite 안에서 다른 서비스를 HTTP로 부르는 부분 |
| `serviceAddresses` | 응답에 "이 데이터를 만든 서비스의 주소"를 같이 넣는 것. 어느 인스턴스가 답했는지 볼 수 있다(9장에서 다시 씀) |

## 6. 교재와 다른 핵심 (Boot 4)
교재는 Boot 3.5 / Java 24다. 3장에서 바꾼 것들(`spring.web.error.include-message`, `UNPROCESSABLE_CONTENT`, Jackson 3 `tools.jackson`, `@AutoConfigureWebTestClient` 등)은 이후 장에서도 **계속 유지**된다. 전체 목록은 [README.md](README.md)의 표.
