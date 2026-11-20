# 6장: 영속성 추가

> 기준 코드: 교수님 저장소 [spring-microservice-boot4](https://github.com/NohYounghoon3651/spring-microservice-boot4) `Chapter06`
> 시작: 교수님 `Chapter05` 상태 (중간부터 하는 법: [../README.md](../README.md))
> 이전: [../chapter5/README.md](../chapter5/README.md) / 다음: [../chapter7/README.md](../chapter7/README.md) / 상태: **검증 완료**

## 문서 읽는 법
- 모든 명령은 작업 폴더(`~/msa`)에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.
- 결과는 교수님 `Chapter06`와 같은 코드다(교재의 영어 주석만 뺐다).

## 무엇을 하나
핵심 서비스가 DB에 데이터를 저장한다. product·recommendation은 MongoDB, review는 MySQL(JPA). 핵심 서비스와 composite에 생성(POST)과 삭제(DELETE) API가 생긴다.

**먼저 [00-background.md](00-background.md)** 를 읽는다.

## 진행 순서

| 순서 | 문서 | 내용 | 바뀌는 파일 |
|---|---|---|---|
| 00 | [00-background.md](00-background.md) | 이 장에서 무엇이 달라지는지, 용어 | 읽기 |
| 01 | [01-api-util.md](01-api-util.md) | api: 데이터 클래스, 생성·삭제 API | 파일 10개 |
| 02 | [02-core-services.md](02-core-services.md) | 핵심 서비스: DB, 엔티티, 매퍼, 테스트 | 파일 36개 |
| 03 | [03-composite.md](03-composite.md) | composite: 생성·삭제를 세 서비스로 나눠 호출 | 파일 4개 |
| 04 | [04-build-compose-test.md](04-build-compose-test.md) | compose에 DB 추가, test-em-all, 실행 | 파일 2개 |
| - | [troubleshooting.md](troubleshooting.md) | 막히면 | |

## 검증
- 교수님 `Chapter05`를 빈 작업 폴더에 복사하고 이 장 문서를 글자 그대로 적용한 결과가 교수님 `Chapter06`과 같다(주석 제외).
- 그 폴더에서 `./gradlew build`(Testcontainers 테스트 포함) → `docker compose build` → `./test-em-all.bash start stop` → `End, all tests OK`.

## 교재(Boot 3.5)와 다른 점 — 교수님 코드에 이미 반영된 것
따라 할 단계는 아니고, 교재 본문과 코드가 왜 다른지 볼 때 참고한다.

| 항목 | 내용 (교수님 MIGRATION-BOOT4.md 요약) |
|---|---|
| Spring Boot 3.5.0 → 4.0.9 | `org.springframework.boot` 플러그인과 `springBootVersion`(api/util의 BOM)을 4.0.9로 변경. |
| Java 24 → 21 | `JavaLanguageVersion.of(24)` → `of(21)`. 코드에 Java 22 이상 전용 문법/API는 없음을 확인함. |
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
| `HttpStatus.UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` | Spring Framework 7에서 422 상수 이름이 바뀌었고, `HttpStatus.resolve(422)`는 새 상수를 반환합니다. 옛 상수를 쓰면 422 검사 테스트가 실패하고, 복합 서비스의 `switch` 분기가 매칭되지 않아 500 응답이 나갑니다. (GitHub Actions 빌드에서 발견) |
