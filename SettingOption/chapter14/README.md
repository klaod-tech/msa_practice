# 14장: 분산 추적 (Micrometer Tracing, Zipkin)

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 14장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4 / Zipkin 3.5.0**
> 이전: [../chapter13/README.md](../chapter13/README.md) / 다음: [../chapter15/README.md](../chapter15/README.md) / 상태: **검증 완료**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기).
- `★ 추가` / `★ 변경` / `★ 삭제` = 13장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = 교재 14장과 다르게 쓰는 부분. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

## 무엇을 하나
**먼저 [00-background.md](00-background.md)** 로 "trace, span이 무엇이고 어떻게 서비스 사이를 따라가는지"를 본다.

요청 하나가 gateway → composite → product/recommendation/review(그리고 RabbitMQ 이벤트)를 지나가는 길을 **traceId 하나로 묶어** Zipkin에서 본다. 로그에도 같은 traceId가 찍힌다.

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | trace/span, 전파, 13장과 달라지는 점 (먼저 읽기) | 읽기 |
| 1 | [01-dependencies-and-logging.md](01-dependencies-and-logging.md) | 서비스 7개: 추적 의존성, 로그에 traceId, 리액터 컨텍스트 전파 | (02에서 같이 빌드) |
| 2 | [02-composite-observation.md](02-composite-observation.md) | composite: WebClient 빈, 사용자 정의 span(`product info`), 빌드 버전 태그 | `./gradlew build` |
| 3 | [03-config-repo.md](03-config-repo.md) | Zipkin 주소, 샘플링 100%, 로그 레벨 | - |
| 4 | [04-docker-compose-and-zipkin.md](04-docker-compose-and-zipkin.md) | compose에 Zipkin, test-em-all, 로그와 Zipkin에서 traceId 따라가기 | `End, all tests OK`, Zipkin 조회 |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| 추적 의존성 | `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-zipkin` | + **`spring-boot-micrometer-tracing-opentelemetry`**, **`spring-boot-zipkin`** (Boot 4는 추적·Zipkin 자동 설정이 별도 모듈) | 01 |
| review DB 추적 | `datasource-micrometer-spring-boot:1.0.0` | **`2.2.1`** (Boot 4용은 2.x, 2.3.x는 Boot 4.1 기준) | 01 |
| Zipkin 주소 설정 이름 | `management.zipkin.tracing.endpoint` | **`management.tracing.export.zipkin.endpoint`** | 03 |
| `ObservationRegistryCustomizer` 패키지 | `org.springframework.boot.actuate.autoconfigure.observation` | `org.springframework.boot.micrometer.observation.autoconfigure` | 02 |
| 교재 config-repo의 영어 주석(추적 헤더 보기) | 영어 설명 + 주석 처리된 설정 2줄 | 한글 설명 + 같은 설정 2줄 | 03 |
| AOP 의존성 | `spring-boot-starter-aop` (12장부터) | `spring-boot-starter-aspectj` (12장부터 유지) | - |

## 검증한 것 / 안 한 것
- (2026-11-26) 빈 폴더에서 2~14장 SettingOption 문서를 글자 그대로 재현한 코드로 `./gradlew build` → `docker compose build` → `./test-em-all.bash start stop`을 돌려 9~14장 모두 `End, all tests OK`를 확인했다.
- 저장소의 13장 완료 커밋에 이 문서의 코드 블록만 적용해 14장 상태를 만들었고, 검증한 파일과 똑같이 만들어지는 것을 비교로 확인했다.
- 테스트 60개 통과(composite 8, product 13, recommendation 15, review 15, authorization-server 4, config-server 1, eureka-server 3, gateway 1).
- **임시 폴더에서 compose 전체를 띄워** 확인했다(13장까지는 jar만 띄웠던 것을 이번에는 compose까지):
  - `./test-em-all.bash` → `End, all tests OK` (12장 설정 서버, 13장 서킷 브레이커 테스트 포함)
  - 04의 5절 명령을 문서 그대로 실행해 결과를 확인했다: Zipkin 서비스 7개, 로그의 같은 traceId(composite/product/recommendation/review), trace의 span 목록(`product info`, review `query`), 태그 `productId`/`build.version`, DELETE 이벤트의 PRODUCER/CONSUMER span
  - 그 과정에서 "요청 직후 바로 Zipkin을 부르면 아직 trace가 없다"를 발견해 5-3에 `sleep 5`를 넣었다.
- 안 한 것: Zipkin **브라우저 화면**(API로만 확인), Kafka/파티션 compose 파일 구동.
