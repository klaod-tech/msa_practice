# 3장: 협업 마이크로서비스 집합 생성

> **상태: 완료 (2026-11-02)** — 서비스 4개 빌드, JUnit 테스트 4개 통과, `test-em-all.bash` 전체 통과
> 이전: [../chapter2/README.md](../chapter2/README.md) (1·2장 환경 설정, 기본 골격) / 다음: [../chapter4/README.md](../chapter4/README.md)

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 3장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 (Spring 7, Jackson 3) / Gradle 9.8.2**
> 교재 코드는 **Java 24 / Boot 3.5.0** 기준이라 그대로 쓰면 안 되는 부분이 있다. 이 문서들은 Boot 4에 맞춘 **전체 코드**를 담고 있어서 문서만 보고 따라 하면 된다.

## 문서 읽는 법
- **새 파일**은 만드는 명령부터 적었다.
  - 명령 블록을 위에서부터 그대로 입력하면 된다 (`mkdir -p` → `code 파일`)
  - `code`로 연 탭은 `Ctrl+S`로 저장해야 디스크에 파일이 생긴다
- 새 파일은 내용 전체를 보여 준다.
- **기존 파일 수정**은 넣을 위치의 **앞뒤 줄을 함께** 보여 준다.
  - 한 줄만 바뀌면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`
  - 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 지우고 바꿀 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(`★ 지움:` 줄을 지운다)
  - `★ 참고:` / `(교재: ...)` = 교재와 다르게 쓰는 이유
  - ★ 주석은 입력하지 않아도 된다
  - 표시 없는 줄 = 이미 있는 줄 (위치 확인용, 건드리지 않음)
- 코드 블록 안의 `...`는 "이 사이의 기존 줄은 그대로"라는 뜻이다.

## 진행 순서 (문서 목록)

| 순서 | 문서 | 내용 | 교재 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | 3장 전체 그림, 문서별로 생기는 것, 용어 (먼저 읽기) | |
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

## 새 서비스를 만들 때 체크리스트
1. `build.gradle`: `jar { enabled = false }`, `implementation project(':api')`, `implementation project(':util')`
2. 메인 클래스: `@ComponentScan("se.magnus")`
3. `services/XxxServiceImpl.java`
4. `application.properties` 삭제 → `src/main/resources/application.yml` (서비스 이름, 포트, `spring.web.error.include-message: always`)

서비스별 포트: composite **7001** / product **7002** / recommendation **7003** / review **7004**

## 캡처·커밋 지점
각 시점에서 **브라우저 화면**과 **터미널 화면**을 캡처하고 커밋한다(`git add .` → `git commit -m "..."`, 2장 02의 5-1).

| 시점(문서·절) | 브라우저 주소 | 터미널에서 보일 결과 | 커밋 메시지 예시 |
|---|---|---|---|
| 03 5절 ② 호출 | http://localhost:7002/product/123 | `Started ProductServiceApplication`, curl JSON | `product-service 구현` |
| 04 확인 ② | http://localhost:7002/product/13 (404), http://localhost:7002/product/0 (422) | `curl -i` → `404`, `422` | `오류 처리 추가` |
| 07 1절 (4개 실행 중) | http://localhost:7001/product-composite/1 | `ss -ltnp \| grep 700`에 4줄 | `recommendation·review·composite 구현` |
| 07 3절 ② 테스트 | (1절 화면 사용) | `End, all tests OK` | `테스트 추가 (3장 완료)` |
