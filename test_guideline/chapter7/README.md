# 7장: 리액티브 마이크로서비스 개발

> 기준 코드: 교수님 저장소 [spring-microservice-boot4](https://github.com/NohYounghoon3651/spring-microservice-boot4) `Chapter07`
> 시작: 교수님 `Chapter06` 상태 (중간부터 하는 법: [../README.md](../README.md))
> 이전: [../chapter6/README.md](../chapter6/README.md) / 다음: [../chapter9/README.md](../chapter9/README.md) (8장은 코드 없음) / 상태: **검증 완료**

## 문서 읽는 법
- 모든 명령은 작업 폴더(`~/msa`)에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.
- 결과는 교수님 `Chapter07`와 같은 코드다(교재의 영어 주석만 뺐다).

## 무엇을 하나
조회(GET)는 논블로킹 동기 API(`Mono`/`Flux`, `WebClient`)로, 생성·삭제는 RabbitMQ(또는 Kafka) 이벤트로 바꾼다. composite는 이벤트만 보내고 바로 202를 돌려준다.

**먼저 [00-background.md](00-background.md)** 를 읽는다.

## 진행 순서

| 순서 | 문서 | 내용 | 바뀌는 파일 |
|---|---|---|---|
| 00 | [00-background.md](00-background.md) | 이 장에서 무엇이 달라지는지, 용어 | 읽기 |
| 01 | [01-api-util.md](01-api-util.md) | api: 리액티브 반환형, 이벤트 / util: 리액터 테스트 | 파일 7개 |
| 02 | [02-core-services.md](02-core-services.md) | 핵심 서비스: 리액티브 저장소, 이벤트 받기 | 파일 25개 |
| 03 | [03-composite.md](03-composite.md) | composite: WebClient, 이벤트 보내기, 헬스 체크 | 파일 10개 |
| 04 | [04-build-compose-test.md](04-build-compose-test.md) | compose에 RabbitMQ, test-em-all, 실행 | 파일 4개 |
| - | [troubleshooting.md](troubleshooting.md) | 막히면 | |

## 검증
- 교수님 `Chapter06`을 빈 작업 폴더에 복사하고 이 장 문서를 글자 그대로 적용한 결과가 교수님 `Chapter07`과 같다(주석 제외).
- 그 폴더에서 `./gradlew build` → `docker compose build` → `./test-em-all.bash start stop` → `End, all tests OK`.

## 교재(Boot 3.5)와 다른 점 — 교수님 코드에 이미 반영된 것
따라 할 단계는 아니고, 교재 본문과 코드가 왜 다른지 볼 때 참고한다.

| 항목 | 내용 (교수님 MIGRATION-BOOT4.md 요약) |
|---|---|
| Spring Boot 3.5.0 → 4.0.9 | `org.springframework.boot` 플러그인과 `springBootVersion`(api/util의 BOM)을 4.0.9로 변경. |
| Java 24 → 21 | `JavaLanguageVersion.of(24)` → `of(21)`. 코드에 Java 22 이상 전용 문법/API는 없음을 확인함. |
| Spring Cloud 2025.0.0 → 2025.1.4 | Spring Cloud 2025.1.4는 Spring Boot 4.0.9를 기준으로 빌드된 릴리스 트레인. |
| Gradle 래퍼 8.14 → 8.14.6 | Boot 4 Gradle 플러그인은 Gradle 8.14 이상(또는 9.x)이 필요. 8.14 최신 패치로 변경. |
| create-projects.bash | `spring init --boot-version=4.0.9 --java-version=21`로 변경. |
| 테스트 스타터 추가 (Boot 4 모듈화) | Boot 4는 테스트 인프라도 모듈로 분리됨. 테스트가 사용하는 기능에 맞춰 `spring-boot-starter-webflux-test`(WebTestClient), `spring-boot-starter-data-mongodb(-reactive)-test`(@DataMongoTest), `spring-boot-starter-data-jpa-test`(@DataJpaTest), `spr... |
| `@AutoConfigureWebTestClient` 추가 | Boot 4에서는 `@SpringBootTest`만으로 `WebTestClient` 빈이 만들어지지 않음. WebTestClient를 주입받는 테스트 클래스에 `@AutoConfigureWebTestClient`(`org.springframework.boot.webtestclient.autoconfigure`)를 추가. |
| 테스트 애너테이션 패키지 변경 | `@DataMongoTest` → `org.springframework.boot.data.mongodb.test.autoconfigure`, `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`, `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.a... |
| Testcontainers 2.0 | 아티팩트명 변경(`junit-jupiter` → `testcontainers-junit-jupiter`, `mongodb` → `testcontainers-mongodb`, `mysql` → `testcontainers-mysql`), 클래스 패키지 변경(`org.testcontainers.mongodb.MongoDBContainer`, `org.testcontainers.mysql.M... |
| Jackson 2 → Jackson 3 | Boot 4와 Spring Cloud Stream/Function 5는 Jackson 3를 사용. `com.fasterxml.jackson.databind/core` → `tools.jackson.*`, `JsonProcessingException` → `JacksonException`, `ZonedDateTimeSerializer` → `tools.jackson.databind.ext... |
| `catch (IOException)` → `catch (JacksonException)` | Jackson 3의 `readValue()`는 checked 예외인 `IOException`을 던지지 않으므로(언체크 `JacksonException`) 그대로 두면 컴파일 오류. |
| DTO 필드의 `final` 제거 | Jackson 3는 `MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS`가 기본 비활성이라 `final` 필드를 역직렬화하지 못함(오류 없이 값이 비어 버림). `Event`, `HttpErrorInfo`, `ProductAggregate`, `*Summary`, `ServiceAddresses` 등 기본 생성자 + getter 구조의 DTO에서 `fina... |
| `server.error.*` → `spring.web.error.*` | `server.error.include-message: always` → `spring.web.error.include-message: always`. |
| MongoDB 프로퍼티 `spring.data.mongodb.*` → `spring.mongodb.*` | `host`, `port`, `database`, `username`, `password`, `authentication-database`가 `spring.mongodb.*`로 이동. 환경 변수도 `SPRING_DATA_MONGODB_*` → `SPRING_MONGODB_*`(docker-compose, 헬름 values 포함). `ProductServiceApplication` 등의 ... |
| springdoc-openapi 2.8.6 → 3.0.3 | springdoc 3.x가 Boot 4용. 3.1 이상은 Boot 4.1 기준이라 Boot 4.0.x에 맞는 3.0.3을 선택. |
| Dockerfile: 베이스 이미지와 레이어 추출 | `eclipse-temurin:24_36-jre-noble` → `eclipse-temurin:21-jre-noble`. 사용 중단된 `-Djarmode=layertools` 대신 `-Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted` 사용(디렉터리 구조와 `JarLauncher` 진입점은 동일). |
| 헬스 API 패키지 변경 | `org.springframework.boot.actuate.health.*` → `org.springframework.boot.health.contributor.*` (`Health`, `ReactiveHealthIndicator`, `ReactiveHealthContributor`, `CompositeReactiveHealthContributor`). |
| `spring-boot-starter-webclient` 추가 | Boot 4에서 `WebClient.Builder` 자동 구성(관찰/추적 계측 포함)은 `spring-boot-webclient` 모듈로 분리되어 `spring-boot-starter-webflux`에 포함되지 않음. `WebClient`를 사용하는 product-composite와 gateway에 추가. |
| `HttpStatus.UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` | Spring Framework 7에서 422 상수 이름이 바뀌었고, `HttpStatus.resolve(422)`는 새 상수를 반환합니다. 옛 상수를 쓰면 422 검사 테스트가 실패하고, 복합 서비스의 `switch` 분기가 매칭되지 않아 500 응답이 나갑니다. (GitHub Actions 빌드에서 발견) |
| StreamBridge 출력 바인딩을 기동 시점에 생성 | 상품 생성 시 이벤트 3개를 병렬로 보낼 때, 첫 전송에서 늦게 만들어지는 바인딩 때문에 Kafka 구성에서 가끔 `Dispatcher has no subscribers` 오류(500)가 났습니다. product-composite 설정에 `spring.cloud.stream.output-bindings: products-out-0;recommendations-out-0;reviews-o... |
| 실행 안정성 보강 (원본에도 있던 타이밍 문제) | 자세한 이유는 최상위 `MIGRATION-BOOT4.md`의 3-1절을 참고하세요. |
