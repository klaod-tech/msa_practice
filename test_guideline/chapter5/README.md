# 5장: OpenAPI를 사용한 API 문서화

> 기준 코드: 교수님 저장소 [spring-microservice-boot4](https://github.com/NohYounghoon3651/spring-microservice-boot4) `Chapter05`
> 시작: 교수님 `Chapter04` 상태 (중간부터 하는 법: [../README.md](../README.md))
> 이전: [../chapter4/README.md](../chapter4/README.md) / 다음: [../chapter6/README.md](../chapter6/README.md) / 상태: **검증 완료**

## 문서 읽는 법
- 모든 명령은 작업 폴더(`~/msa`)에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.
- 결과는 교수님 `Chapter05`와 같은 코드다(교재의 영어 주석만 뺐다).

## 무엇을 하나
springdoc-openapi로 composite API의 문서(OpenAPI)를 자동으로 만들고, 브라우저의 Swagger UI에서 보고 직접 호출한다. 잘못된 요청용 400 응답도 추가한다.

**먼저 [00-background.md](00-background.md)** 를 읽는다.

## 진행 순서

| 순서 | 문서 | 내용 | 바뀌는 파일 |
|---|---|---|---|
| 00 | [00-background.md](00-background.md) | 이 장에서 무엇이 달라지는지, 용어 | 읽기 |
| 01 | [01-api-util.md](01-api-util.md) | api: 문서 어노테이션, 400 예외 / util: 400 핸들러 | 파일 4개 |
| 02 | [02-composite.md](02-composite.md) | composite: springdoc, OpenAPI 빈, 설명 문구 | 파일 3개 |
| 03 | [03-build-compose-test.md](03-build-compose-test.md) | test-em-all.bash, 실행, Swagger UI | 파일 1개 |
| - | [troubleshooting.md](troubleshooting.md) | 막히면 | |

## 검증
- 교수님 `Chapter04`를 빈 작업 폴더에 복사하고 이 장 문서를 글자 그대로 적용한 결과가 교수님 `Chapter05`와 같다(주석 제외).
- 그 폴더에서 `./gradlew build` → `docker compose build` → `./test-em-all.bash start stop` → `End, all tests OK`, OpenAPI 문서 조회 확인.

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
| Jackson 2 → Jackson 3 | Boot 4와 Spring Cloud Stream/Function 5는 Jackson 3를 사용. `com.fasterxml.jackson.databind/core` → `tools.jackson.*`, `JsonProcessingException` → `JacksonException`, `ZonedDateTimeSerializer` → `tools.jackson.databind.ext... |
| `catch (IOException)` → `catch (JacksonException)` | Jackson 3의 `readValue()`는 checked 예외인 `IOException`을 던지지 않으므로(언체크 `JacksonException`) 그대로 두면 컴파일 오류. |
| DTO 필드의 `final` 제거 | Jackson 3는 `MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS`가 기본 비활성이라 `final` 필드를 역직렬화하지 못함(오류 없이 값이 비어 버림). `Event`, `HttpErrorInfo`, `ProductAggregate`, `*Summary`, `ServiceAddresses` 등 기본 생성자 + getter 구조의 DTO에서 `fina... |
| `server.error.*` → `spring.web.error.*` | `server.error.include-message: always` → `spring.web.error.include-message: always`. |
| springdoc-openapi 2.8.6 → 3.0.3 | springdoc 3.x가 Boot 4용. 3.1 이상은 Boot 4.1 기준이라 Boot 4.0.x에 맞는 3.0.3을 선택. |
| Dockerfile: 베이스 이미지와 레이어 추출 | `eclipse-temurin:24_36-jre-noble` → `eclipse-temurin:21-jre-noble`. 사용 중단된 `-Djarmode=layertools` 대신 `-Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted` 사용(디렉터리 구조와 `JarLauncher` 진입점은 동일). |
| `HttpStatus.UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` | Spring Framework 7에서 422 상수 이름이 바뀌었고, `HttpStatus.resolve(422)`는 새 상수를 반환합니다. 옛 상수를 쓰면 422 검사 테스트가 실패하고, 복합 서비스의 `switch` 분기가 매칭되지 않아 500 응답이 나갑니다. (GitHub Actions 빌드에서 발견) |
