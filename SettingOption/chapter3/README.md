# 3장: 협업 마이크로서비스 집합 생성

> **상태: 완료 (2026-11-02)** — 서비스 4개 빌드, JUnit 테스트 4개 통과, `test-em-all.bash` 전체 통과
> 이전: [../setting/README.md](../setting/README.md) (환경 설정) / 다음: 4장

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 3장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 (Spring 7, Jackson 3) / Gradle 9.8.2**
> 교재 소스(`$BOOK_HOME/Chapter03/2-basic-rest-services`)는 **Java 24 / Boot 3.5.0** 기준이라 그대로 쓰면 안 되는 부분이 있다.

## 진행 순서 (문서 목록)

| 순서 | 문서 | 내용 | 교재 |
|---|---|---|---|
| 1 | [01-skeleton.md](01-skeleton.md) | 기본 골격 마이크로서비스 4개 생성 | 3장 앞부분 |
| 2 | [02-api-util.md](02-api-util.md) | api·util 라이브러리 프로젝트 추가 | API 구현 준비 |
| 3 | [03-product-service.md](03-product-service.md) | product-service 구현 + 단독 실행 | 68~70쪽 |
| 4 | [04-error-handling.md](04-error-handling.md) | 오류 처리 추가 (예외, HttpErrorInfo, 핸들러) | 오류 처리 추가 |
| 5 | [05-recommendation-review.md](05-recommendation-review.md) | recommendation, review 서비스 구현 | |
| 6 | [06-composite.md](06-composite.md) | 복합 서비스 (프로퍼티, Integration, ServiceImpl) | 복합 마이크로서비스 |
| 7 | [07-test.md](07-test.md) | 수동 테스트(81쪽) + 자동화 테스트(84쪽) + `test-em-all.bash` | 81쪽~ |
| - | [troubleshooting.md](troubleshooting.md) | 겪은 오류와 해결 모음 | |

---

## Boot 4 때문에 교재와 다르게 한 것 (핵심 요약)

| 항목 | 교재 (Boot 3) | 이 실습 (Boot 4) | 문서 |
|---|---|---|---|
| Java toolchain (api/util build.gradle) | `JavaLanguageVersion.of(24)` | `of(21)` | 02 |
| Boot 버전 (api/util build.gradle) | `springBootVersion = '3.5.0'` | `'4.0.9'` | 02 |
| 오류 메시지 포함 설정 (yml) | `server.error.include-message` | `spring.web.error.include-message` (옛 이름은 **조용히 무시됨**) | 03 |
| 422 상태 코드 상수 | `HttpStatus.UNPROCESSABLE_ENTITY` | `UNPROCESSABLE_CONTENT` (ENTITY는 deprecated) | 04 |
| 422 응답의 `error` 문구 | `"Unprocessable Entity"` | `"Unprocessable Content"` | 07 |
| ObjectMapper import | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` (Jackson 3) | 06 |
| `readValue()` 예외 처리 | `catch (IOException e)` | `catch (JacksonException e)` (`tools.jackson.core`) | 06 |
| final 필드 역직렬화 | 기본 동작으로 됨 | **기본값이 꺼짐** → RestTemplate에 Boot의 JsonMapper 연결 + `spring.jackson.mapper.allow-final-fields-as-mutators: true` | 06 |
| 테스트의 `MediaType` | 본문: `APPLICATION_JSON_UTF8` | `APPLICATION_JSON` (UTF8 상수는 Spring 7에서 삭제) | 07 |
| 테스트의 WebTestClient | `@SpringBootTest`만으로 주입됨 | `@AutoConfigureWebTestClient` 추가 필요 | 07 |
| JSON 필드 순서 | 선언 순서 | 알파벳순 (Jackson 3 기본) | 03 |

## 교재와 무관하게 내가 바꾼 것
- 데이터 클래스(Product, Recommendation, Review, composite의 ProductAggregate 등)는 **롬복**으로 작성했다. 단, 예외 클래스와 HttpErrorInfo는 롬복에 맞지 않아 교재 원본 그대로 두었다 → [04](04-error-handling.md)

---

## 교재 소스를 가져올 때의 요령
- 교재 소스(`2-basic-rest-services`)는 **3장을 끝낸 최종 상태**다. 중간 단계에서 그대로 붙이면 아직 만들지 않은 클래스(예: `exceptions`)를 참조해서 오류가 난다.
- 붙이기 전에 `import se.magnus...` 줄을 먼저 본다. 아직 없는 클래스를 참조하는 부분은 나중 단계로 미룬다.
- 본문은 코드를 다 싣지 않고 "소스에서 확인하라"고 넘기는 경우가 많다(settings.gradle의 api/util 등록, `jar { enabled = false }`, ServiceUtil 등). 빠진 단계는 소스와 비교해서 찾는다.
  ```bash
  diff -rq $BOOK_HOME/Chapter03/2-basic-rest-services ~/msa -x build -x .gradle -x .git -x bin
  ```
- **본문과 소스가 다를 때는 소스를 따른다.** 예: 본문은 예외 클래스를 `se.magnus.util.exceptions`에 둔다고 하지만, 소스는 `se.magnus.api.exceptions`에 둔다.

## 새 서비스를 만들 때 체크리스트
1. `build.gradle`: `jar { enabled = false }`, `implementation project(':api')`, `implementation project(':util')`
2. 메인 클래스: `@ComponentScan("se.magnus")`
3. `services/XxxServiceImpl.java`
4. `application.properties` 삭제 → `src/main/resources/application.yml` (서비스 이름, 포트, `spring.web.error.include-message: always`)

서비스별 포트: composite **7001** / product **7002** / recommendation **7003** / review **7004**
