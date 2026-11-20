# 00. 배경: 3장에서 무엇을 만드나

> 코드 없이 읽기만 하는 문서.

## 1. 시작 상태
[환경 설정](../../SettingOption/setting/README.md) 01~05에서 Java 21, Spring CLI, VS Code를 준비했다. 코드는 아직 없다.

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
- 터미널에서 `java -jar ...`로 하나씩 띄워서 `localhost:포트`로 부른다(도커는 4장).

## 3. 프로젝트 구성
| 프로젝트 | 종류 | 하는 일 |
|---|---|---|
| `api` | 라이브러리 | 서비스끼리 주고받는 API 인터페이스와 데이터 클래스, 예외 클래스 |
| `util` | 라이브러리 | 공통 코드: `ServiceUtil`, `GlobalControllerExceptionHandler`(예외 → HTTP 상태 코드) |
| `microservices/product-service` 등 3개 | 앱 | 핵심 서비스. api 인터페이스를 구현하는 `@RestController` |
| `microservices/product-composite-service` | 앱 | 세 서비스를 부르는 `ProductCompositeIntegration` + 합치는 `ProductCompositeServiceImpl` |

루트 `settings.gradle`이 6개 프로젝트를 묶어서 `./gradlew build` 한 번으로 전부 빌드한다.

## 4. 문서별로 생기는 것
| 문서 | 이전 상태 → 이후 상태 |
|---|---|
| 01 | 빈 폴더 → spring init으로 서비스 4개 골격, 래퍼는 루트에 하나 |
| 02 | → `api`, `util` 라이브러리 |
| 03 | → 핵심 서비스 3개가 고정 데이터로 응답, 404/422 오류 처리 |
| 04 | → composite가 셋을 불러 합친 결과 응답 |
| 05 | → 루트 빌드 설정(Gradle 8.14.6), test-em-all, **빌드·실행·자동 테스트** |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 핵심(core) 서비스 | product, recommendation, review. 자기 데이터만 다룬다 |
| 복합(composite) 서비스 | 여러 핵심 서비스를 불러 결과를 합치는 서비스 |
| 통합 계층(Integration) | composite 안에서 다른 서비스를 HTTP로 부르는 부분 |
| `serviceAddresses` | 응답에 "이 데이터를 만든 서비스의 주소"를 같이 넣는 것. 어느 인스턴스가 답했는지 볼 수 있다(9장에서 다시 씀) |
| 404 / 422 | 없는 상품(productId 13) / 잘못된 값(음수 productId) |
