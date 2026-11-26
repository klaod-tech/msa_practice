# 15장: 쿠버네티스 소개

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 15장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4** (3부 도구 버전은 [1장 4절](../chapter1/01-ubuntu-environment.md))
> 이전: [../chapter14/README.md](../chapter14/README.md) / 다음: [../chapter16/README.md](../chapter16/README.md) / 상태: **문서 재현 확인 / 실행 검증 중**

## 문서 읽는 법
- 모든 명령은 작업 폴더(`~/msa`)에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- 바뀐 곳 표시(이전 장 대비). md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.
  - 한 줄: 줄 끝에 `★ 추가` / `★ 변경 (전: 옛 코드)`
  - 여러 줄 추가: `★ 추가 시작 (N줄)` ~ `★ 추가 끝` 사이를 친다
  - 여러 줄 변경: `★ 변경 시작` ~ `★ 변경 끝`. 그 안의 `★ 전:` 줄은 **지울 옛 코드**, 나머지가 새 코드
  - 삭제: `★ 삭제: 옛 코드`(한 줄) 또는 `★ 삭제 시작` ~ `★ 삭제 끝` 안의 `★ 지움:` 줄들을 지운다
  - 표시 주석(`★`로 시작하는 줄)은 안내용이라 치지 않아도 된다
- 1~14장(SettingOption)을 따라 한 코드에서 이어진다. 우리 코드의 Lombok 데이터 클래스·엔티티는 그대로 쓴다.

## 무엇을 하나
두 가지를 한다.
1. **3부 준비: 설정 파일을 3부 기준으로 맞추기.** 16장부터는 쿠버네티스·헬름 파일이 이 기준의 compose, Dockerfile, build.gradle, config-repo, test-em-all을 그대로 이어받는다. 그래서 이 장에서 이 파일들을 한 번 맞춰 둔다. 우리 코드의 Lombok 데이터 클래스(`Product`, `Recommendation`, `Review`, `ProductAggregate`, 요약 클래스, `ServiceAddresses`)와 엔티티 3개, Lombok 의존성, `allow-final-fields-as-mutators` 설정은 **그대로 유지**한다.
2. **쿠버네티스 첫 실습.** minikube로 내 PC에 클러스터를 만들고, nginx 디플로이먼트와 서비스를 kubectl로 배포해 본다.

**먼저 [00-background.md](00-background.md)** 를 읽는다.

## 진행 순서

| 순서 | 문서 | 내용 | 바뀌는 파일 |
|---|---|---|---|
| 00 | [00-background.md](00-background.md) | 이 장에서 무엇이 달라지는지, 용어 | 읽기 |
| 01 | [01-api-util.md](01-api-util.md) | `api`, `util`: 3부 기준으로 맞추기 | 파일 12개 |
| 02 | [02-core-services.md](02-core-services.md) | 핵심 서비스: build.gradle, 테스트, Dockerfile 맞추기 | 파일 22개 |
| 03 | [03-composite.md](03-composite.md) | composite: 3부 기준으로 맞추기 | 파일 13개 |
| 04 | [04-spring-cloud.md](04-spring-cloud.md) | 스프링 클라우드 서버: build.gradle, Dockerfile, 인가 서버 정리 | 파일 22개 |
| 05 | [05-config-repo.md](05-config-repo.md) | config-repo: 3부 기준으로 맞추기 | 파일 6개 |
| 06 | [06-kubernetes.md](06-kubernetes.md) | nginx 매니페스트, minikube, kubectl | 파일 2개 |
| 07 | [07-build-compose-test.md](07-build-compose-test.md) | compose, settings.gradle, test-em-all 맞추기 + 실행 | 파일 5개 |
| - | [troubleshooting.md](troubleshooting.md) | 막히면 | |

## 검증
- 14장까지 SettingOption 문서를 따라 한 상태에 이 장 문서의 코드 블록을 글자 그대로 적용하면, 검증용으로 만든 15장 코드와 똑같이 만들어지는 것을 비교로 확인했다(`RESULT OK`).
- 컴파일 통과. docker compose 빌드·test-em-all과 minikube 배포 테스트는 진행 중이다(결과가 나오면 이 칸에 적는다).

## 교재(Boot 3.5)와 다른 점
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
| TestRestTemplate 패키지 변경 및 `@AutoConfigureTestRestTemplate` | `TestRestTemplate` → `org.springframework.boot.resttestclient.TestRestTemplate`, 테스트 클래스에 `@AutoConfigureTestRestTemplate` 추가. |
| 유레카 서버 헬스 테스트 수정 | Boot 4는 liveness/readiness 프로브가 기본 활성화되어 `/actuator/health` 응답에 `groups` 필드가 추가됨. 응답 전체 비교 대신 `"status":"UP"` 포함 여부만 검사하도록 변경. |
| Testcontainers 2.0 | 아티팩트명 변경(`junit-jupiter` → `testcontainers-junit-jupiter`, `mongodb` → `testcontainers-mongodb`, `mysql` → `testcontainers-mysql`), 클래스 패키지 변경(`org.testcontainers.mongodb.MongoDBContainer`, `org.testcontainers.mysql.M... |
| Jackson 2 → Jackson 3 | Boot 4와 Spring Cloud Stream/Function 5는 Jackson 3를 사용. `com.fasterxml.jackson.databind/core` → `tools.jackson.*`, `JsonProcessingException` → `JacksonException`, `ZonedDateTimeSerializer` → `tools.jackson.databind.ext... |
| `catch (IOException)` → `catch (JacksonException)` | Jackson 3의 `readValue()`는 checked 예외인 `IOException`을 던지지 않으므로(언체크 `JacksonException`) 그대로 두면 컴파일 오류. |
| DTO 필드의 `final` 제거 | Jackson 3는 `MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS`가 기본 비활성이라 `final` 필드를 역직렬화하지 못함(오류 없이 값이 비어 버림). `Event`, `HttpErrorInfo`, `ProductAggregate`, `*Summary`, `ServiceAddresses` 등 기본 생성자 + getter 구조의 DTO에서 `fina... |
| `server.error.*` → `spring.web.error.*` | `server.error.include-message: always` → `spring.web.error.include-message: always`. |
| 원본 오타 수정 | `authorization-server/src/test/resources/application.yml`의 `ml.server.error.include-message`(원본의 오타로 효과 없던 키)를 `spring.web.error.include-message`로 수정. |
| MongoDB 프로퍼티 `spring.data.mongodb.*` → `spring.mongodb.*` | `host`, `port`, `database`, `username`, `password`, `authentication-database`가 `spring.mongodb.*`로 이동. 환경 변수도 `SPRING_DATA_MONGODB_*` → `SPRING_MONGODB_*`(docker-compose, 헬름 values 포함). `ProductServiceApplication` 등의 ... |
| springdoc-openapi 2.8.6 → 3.0.3 | springdoc 3.x가 Boot 4용. 3.1 이상은 Boot 4.1 기준이라 Boot 4.0.x에 맞는 3.0.3을 선택. |
| Dockerfile: 베이스 이미지와 레이어 추출 | `eclipse-temurin:24_36-jre-noble` → `eclipse-temurin:21-jre-noble`. 사용 중단된 `-Djarmode=layertools` 대신 `-Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted` 사용(디렉터리 구조와 `JarLauncher` 진입점은 동일). |
| 헬스 API 패키지 변경 | `org.springframework.boot.actuate.health.*` → `org.springframework.boot.health.contributor.*` (`Health`, `ReactiveHealthIndicator`, `ReactiveHealthContributor`, `CompositeReactiveHealthContributor`). |
| `spring-boot-starter-webclient` 추가 | Boot 4에서 `WebClient.Builder` 자동 구성(관찰/추적 계측 포함)은 `spring-boot-webclient` 모듈로 분리되어 `spring-boot-starter-webflux`에 포함되지 않음. `WebClient`를 사용하는 product-composite와 gateway에 추가. |
| `spring-boot-starter-restclient` 추가 (유레카 클라이언트) | Spring Cloud Netflix 5의 유레카 클라이언트는 `RestClient` 전송을 쓰며 문서상 `spring-boot-restclient`가 필요. |
| 게이트웨이 액추에이터 엔드포인트 | 사용 중단된 `management.endpoint.gateway.enabled: true` → `management.endpoint.gateway.access: unrestricted`. |
| OAuth2 리소스 서버 스타터 | `spring-security-oauth2-resource-server` + `spring-security-oauth2-jose` → `spring-boot-starter-security-oauth2-resource-server`. Boot 4에서는 리소스 서버 자동 구성(`issuer-uri`로 JwtDecoder 생성)이 별도 모듈에 있어 스프링 시큐리티 jar만으로는 동작하지 않음. |
| 인가 서버 스타터 이름 변경 | `spring-boot-starter-oauth2-authorization-server` → `spring-boot-starter-security-oauth2-authorization-server`. |
| 인가 서버 설정 클래스 패키지 변경 | Spring Authorization Server가 Spring Security 7에 통합됨. `OAuth2AuthorizationServerConfigurer` → `org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization`, `OAuth2AuthorizationServerConf... |
| 시큐리티 테스트 스타터 | `spring-security-test` → `spring-boot-starter-security-test`. |
| `spring-boot-starter-aop` → `spring-boot-starter-aspectj` | Boot 4에서 스타터 이름 변경. Resilience4j 애너테이션과 컨피그 클라이언트 재시도(spring-retry)에 필요. |
| Resilience4j 2.3.0 → 2.4.0 | `resilience4j-spring-boot3` → `resilience4j-spring-boot4`. |
| `ObservationRegistryCustomizer` 패키지 변경 | → `org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer`. |
| 분산 추적(OpenTelemetry + Zipkin) 의존성 | Boot 4에서 추적 자동 구성이 액추에이터에서 분리됨. 브리지/익스포터 jar만으로는 추적이 활성화되지 않으므로 `spring-boot-micrometer-tracing-opentelemetry`와 `spring-boot-zipkin` 모듈을 추가. |
| Zipkin 프로퍼티 이름 변경 | `management.zipkin.tracing.endpoint` → `management.tracing.export.zipkin.endpoint`. |
| datasource-micrometer 1.0.0 → 2.2.1 | 2.x가 Boot 4용(2.2.1은 Boot 4.0.x 기준). |
| `HttpStatus.UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` | Spring Framework 7에서 422 상수 이름이 바뀌었고, `HttpStatus.resolve(422)`는 새 상수를 반환합니다. 옛 상수를 쓰면 422 검사 테스트가 실패하고, 복합 서비스의 `switch` 분기가 매칭되지 않아 500 응답이 나갑니다. (GitHub Actions 빌드에서 발견) |
| Jackson 3의 enum 직렬화 | Jackson 3는 enum을 기본적으로 `toString()`으로 씁니다. 그래서 `/actuator/circuitbreakerevents`의 `stateTransition`이 `CLOSED_TO_OPEN` 대신 `State transition from CLOSED to OPEN`으로 나왔습니다. product-composite 설정에 `spring.jackson.datatype.en... |
| StreamBridge 출력 바인딩을 기동 시점에 생성 | 상품 생성 시 이벤트 3개를 병렬로 보낼 때, 첫 전송에서 늦게 만들어지는 바인딩 때문에 Kafka 구성에서 가끔 `Dispatcher has no subscribers` 오류(500)가 났습니다. product-composite 설정에 `spring.cloud.stream.output-bindings: products-out-0;recommendations-out-0;reviews-o... |
| 실행 안정성 보강 (원본에도 있던 타이밍 문제) | 자세한 이유는 최상위 `MIGRATION-BOOT4.md`의 3-1절을 참고하세요. |
