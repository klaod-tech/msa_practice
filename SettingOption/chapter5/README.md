# 5장: OpenAPI를 활용한 API 문서화

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 5장
> 실습 환경: **Java 21 / Spring Boot 4.0.9**, springdoc-openapi **3.0.3**
> 이전: [../chapter4/README.md](../chapter4/README.md) / 다음: [../chapter6/README.md](../chapter6/README.md) / 상태: **완료**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- 바뀌는 줄 표시: 한 줄이면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`, 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`. `★ 참고:` = 교재와 다른 이유. ★ 주석은 입력하지 않아도 된다.

## 무엇을 하나
product-composite-service가 외부에 노출하는 API(`/product-composite/{productId}`)에 **OpenAPI 기반 문서**를 붙이고, 브라우저에서 보고 호출해 볼 수 있는 **Swagger UI**를 내장한다. springdoc-openapi를 사용한다.

## 진행 순서

| 순서 | 문서 | 내용 |
|---|---|---|
| 0 | [00-background.md](00-background.md) | 4장과 달라지는 점(OpenAPI, Swagger UI), 용어 (먼저 읽기) |
| 1 | [01-springdoc-dependency.md](01-springdoc-dependency.md) | api, composite build.gradle에 springdoc 의존성 추가 |
| 2 | [02-openapi-config.md](02-openapi-config.md) | Application 클래스 OpenAPI 빈, application.yml (springdoc, api 문구) |
| 3 | [03-api-annotations.md](03-api-annotations.md) | 인터페이스 @Tag/@Operation/@ApiResponses, BadRequestException, 400 핸들러 |
| 4 | [04-run-and-swagger-ui.md](04-run-and-swagger-ui.md) | 빌드·구동, test-em-all.bash(Swagger 테스트), Swagger UI Try it out, 동작 원리 |
| - | troubleshooting.md | (진행하면서 추가) |

## 교재와 다르게 하는 것 (진행하면서 추가)

| 항목 | 교재 | 이 실습 | 문서 |
|---|---|---|---|
| springdoc-openapi 버전 | `2.8.6` (Spring Boot 3.4.4 기준) | `3.0.3` (Spring Boot 4.0.x 기준) | 01 |
| composite application.yml | 교재 5장 파일 그대로 | Boot 4 두 줄 유지 (`spring.web.error.include-message`, `spring.jackson.mapper.allow-final-fields-as-mutators`) | 02 |
| GlobalControllerExceptionHandler | `UNPROCESSABLE_ENTITY` | `UNPROCESSABLE_CONTENT` 유지 (교재 파일 통째 복사 주의) | 03 |

## 캡처·커밋 지점
각 시점에서 **브라우저 화면**과 **터미널 화면**을 캡처하고 커밋한다(`git add .` → `git commit -m "..."`, 2장 02의 5-1).

| 시점(문서·절) | 브라우저 주소 | 터미널에서 보일 결과 | 커밋 메시지 예시 |
|---|---|---|---|
| 04 4절 Swagger UI | http://localhost:8080/openapi/swagger-ui.html (Try it out → 200) | test-em-all의 Swagger 테스트 `Test OK`, `End, all tests OK` | `OpenAPI·Swagger UI (5장 완료)` |
