# 00. 배경: 3장에서 무엇을 만드나

> 코드 없이 읽기만 하는 문서. 3장 전체를 한눈에 보는 요약.

## 먼저: VS Code에 빨간 줄이 남을 때 (build.gradle·설정을 바꾼 뒤)
`build.gradle`, `settings.gradle`, `application.yml`을 바꾸거나 새 프로젝트(폴더)를 추가하면 VS Code가 바로 따라오지 못해서, 코드가 맞는데도 빨간 줄이 남는 경우가 있다. **코드 오류가 아니라 VS Code 상태 문제**다. 먼저 터미널에서 진짜 오류인지 확인한다.
```bash
cd ~/msa
./gradlew build
```
`BUILD SUCCESSFUL`이면 코드는 맞다. 아래를 위에서부터 하나씩 해 보고, 풀리면 멈춘다. (`Ctrl+Shift+P` → 명령 이름 입력)

| 순서 | 명령 | 언제 |
|---|---|---|
| 1 | `build.gradle` 저장 후 `Shift+Alt+U` (`Java: Reload Projects`) | 의존성(`implementation ...`)을 추가했는데 import가 빨갛다 |
| 2 | `Java: Import Java Projects into Workspace` → `Developer: Reload Window` | 새 프로젝트를 추가했더니 `non-project file`, `declared package ... does not match expected package` 가 뜬다 |
| 3 | `Java: Clean Java Language Server Workspace` → `Restart and delete` | 1·2로도 안 풀린다. VS Code의 자바 캐시를 지우고 다시 읽는다(1~2분 걸림) |

- 새 프로젝트는 `settings.gradle`의 `include`를 **먼저 저장**한 다음 그 프로젝트의 파일을 열면 이 문제가 덜 생긴다.
- 2를 하기 전에 열어 둔 탭은 옛 상태가 남아 있을 수 있으니 탭을 닫았다 다시 연다(Reload Window를 하면 같이 해결된다).
- `application.yml` 같은 설정은 VS Code가 아니라 **앱을 다시 띄워야** 적용된다. 실행 중인 jar나 컨테이너는 다시 빌드하고 재시작한다(4장부터는 `./gradlew build` → `docker compose build` → `docker compose up -d`).

---

## 1. 시작 상태 (환경 설정까지)
[1장](../chapter1/README.md)·[2장](../chapter2/README.md)에서 Java 21, Gradle, VS Code만 준비했다. 코드는 아직 없다.

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

다음: [01-skeleton.md](01-skeleton.md)
