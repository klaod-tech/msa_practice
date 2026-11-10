# 7장: 반응형 마이크로서비스 개발

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 7장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4** (Spring Cloud Stream 5.0.4, Reactor, Jackson 3)
> 이전: [../chapter6/README.md](../chapter6/README.md) / 상태: **진행 중**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- 바뀌는 곳이 많아서 **거의 모든 파일을 전체 파일로 교체**한다.
- `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.

## 무엇을 하나
교재는 두 가지 방식을 섞어 쓴다(교재 "논블로킹 동기 API와 이벤트 기반 비동기 서비스 중에서 선택하기").

| 연산 | 방식 | 어떻게 |
|---|---|---|
| 조회 (GET) | **논블로킹 동기 API** | 응답을 기다리는 사용자가 있으니 HTTP 그대로. 대신 스레드를 막지 않게 `Mono`/`Flux`로 |
| 생성/삭제 (POST/DELETE) | **이벤트 기반 비동기** | composite가 토픽에 **이벤트를 게시**하고 바로 `202 Accepted`. 핵심 서비스는 토픽을 **구독**해서 처리 |

```
                         GET (WebClient, 논블로킹 HTTP)
            ┌──────────────────────────────────────────────────────────────┐
 client ──▶ product-composite                                               ▼
            │  POST/DELETE → Event 게시                   product / recommendation / review
            ▼                                                    ▲ (messageProcessor가 소비)
       RabbitMQ (또는 Kafka) ── 토픽: products, recommendations, reviews ──┘
```

- **영속성도 논블로킹**: MongoDB는 리액티브 리포지토리(`ReactiveCrudRepository`). MySQL(JPA)은 리액티브 드라이버가 없어서 **블로킹 코드를 별도 스레드 풀(`jdbcScheduler`)** 에서 돌린다.
- 핵심 서비스의 POST/DELETE HTTP API는 **없어진다**(이벤트로만 생성/삭제).

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 1 | [01-dependencies.md](01-dependencies.md) | build.gradle 5개 (Spring Cloud BOM, 리액티브 MongoDB, Stream, WebClient) | 의존성 조회 |
| 2 | [02-reactor-basics.md](02-reactor-basics.md) | 프로젝트 리액터 맛보기 (`ReactorTests`) | `--tests ReactorTests` |
| 3 | [03-reactive-persistence.md](03-reactive-persistence.md) | 리액티브 리포지토리, 엔티티 toString, Application 인덱스, PersistenceTests | (04 끝나고) |
| 4 | [04-core-services-nonblocking.md](04-core-services-nonblocking.md) | 핵심 API 인터페이스, 서비스 구현 (Mono/Flux, review의 jdbcScheduler) | 3개 서비스 `compileJava` |
| 5 | [05-core-event-consumers.md](05-core-event-consumers.md) | Event 클래스, MessageProcessorConfig, 핵심 yml, 서비스 테스트 | 3개 서비스 `build` (도커 필요) |
| 6 | [06-composite-service.md](06-composite-service.md) | composite API, WebClient + 이벤트 게시, 헬스 체크, yml, 메시징 테스트 | composite `build` |
| 7 | [07-docker-compose-and-test.md](07-docker-compose-and-test.md) | compose에 RabbitMQ, 파티션, Kafka / test-em-all.bash / 수동 확인 | `End, all tests OK` |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

03~05는 서로 물려 있어서 **중간에는 컴파일이 깨진 상태**가 된다(예: 03에서 리포지토리가 `Mono`를 돌려주는데 서비스 구현은 04에서 바뀜). 각 문서의 "확인" 칸에 적힌 시점에만 빌드한다.

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| Spring Cloud 버전 | `2025.0.0` | **`2025.1.4`** (Boot 4.0.x용, Stream 5.0.4) | 01 |
| WebClient | `spring-boot-starter-webflux`에 포함 | **`spring-boot-starter-webclient` 추가** (없으면 `WebClient.Builder` 빈이 없음) | 01 |
| 리액티브 MongoDB 테스트 스타터 | 없음 | `spring-boot-starter-data-mongodb-reactive-test` (6장 `...-mongodb-test` 대신) | 01 |
| util 테스트 실행 | 교재 build.gradle에 이미 있음 | `junit-platform-launcher` 한 줄 추가 (우리 util에는 없었음) | 01 |
| `Event`의 날짜 직렬화 | `@JsonSerialize(using = ZonedDateTimeSerializer.class)` (Jackson 2) | **삭제** (Jackson 3는 기본으로 ISO 문자열로 씀, Jackson 2 어노테이션은 무시됨) | 05 |
| 핵심 서비스 yml | 없음 | **`spring.jackson.mapper.allow-final-fields-as-mutators: true`** (없으면 받은 Event가 전부 null) | 05 |
| 헬스 체크 클래스 패키지 | `org.springframework.boot.actuate.health.*` | `org.springframework.boot.health.contributor.*` | 06 |
| composite 테스트의 Jackson | `com.fasterxml.jackson.*` | `tools.jackson.*` (예외도 `JacksonException`) | 06 |
| healthcheck `timeout` | `2s` | `10s` (6장 트러블슈팅, RabbitMQ도 같게) | 07 |
| 6장부터 유지 | `spring.data.mongodb.*`, `UNPROCESSABLE_ENTITY`, `server.error.include-message` 등 | `spring.mongodb.*`, `UNPROCESSABLE_CONTENT`, `spring.web.error.include-message`, `@AutoConfigureWebTestClient`, Lombok 엔티티 | 전체 |

## 검증한 것 / 안 한 것
- 6장 커밋(`779c3c7`)을 복사한 곳에 이 문서의 파일을 그대로 넣고 **모든 프로젝트의 main/test 컴파일**을 통과했다.
- 도커 없이 도는 테스트를 실행해 통과했다: `ReactorTests` 2개, composite 테스트 8개(`MessagingTests` 3, `IsSameEventTests` 1, `ProductCompositeServiceApplicationTests` 4), `MapperTests` 3개.
- Spring Cloud Stream이 쓰는 JSON 변환기로 `Event`를 왕복 변환해 봤다. `allow-final-fields-as-mutators`가 없으면 모든 필드가 `null`, 있으면 정상이다.
- Testcontainers를 쓰는 핵심 서비스 테스트(Persistence, ApplicationTests)와 docker compose 구동(RabbitMQ, 파티션, Kafka)은 실습 과정이라 직접 돌리지 않았다. 실패하면 troubleshooting.md에 추가한다.
