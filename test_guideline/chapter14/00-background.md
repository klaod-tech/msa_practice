# 00. 배경: 분산 추적은 왜 필요한가, 13장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서. 교재 14장 앞부분 요약.

## 1. 13장까지
```
 client ─▶ gateway ─▶ product-composite ─┬▶ product
                                         ├▶ recommendation
                                         ├▶ review ─▶ MySQL
                                         └▶ RabbitMQ ─▶ (이벤트) product / recommendation / review
```
- 요청 하나가 서비스 여러 개를 지나간다. 느리거나 실패했을 때 **어느 서비스에서** 문제가 생겼는지 알려면 서비스마다 로그를 열어서 시간을 맞춰 가며 찾아야 한다.
- 로그끼리 "같은 요청"이라는 표시가 없어서, 요청이 몰리면 어느 줄이 어느 요청인지 구분할 수 없다.

## 2. 14장이 끝나면
```
 traceId = 6e891ba2...  (요청 하나에 하나, 모든 서비스가 같은 값)

 gateway            ├────────────────────────────────────┤ span
 product-composite     ├──────────────────────────────┤    span
   product info          ├──────────────────────────┤      span (직접 만든 것)
     product               ├────────┤                       span
     recommendation        ├──────┤                         span
     review                ├─────────┤                      span
       MySQL query            ├──┤                          span

 → 모든 span이 Zipkin(:9411)으로 모인다 → 화면에서 시간 막대로 본다
 → 로그 줄에도 [서비스,traceId,spanId]가 찍힌다 → grep traceId로 모든 서비스 로그를 모은다
```
- 요청이 들어오면 처음 서비스(gateway)가 **traceId**를 만든다. 다른 서비스를 부를 때 HTTP 헤더(`traceparent`)에 실어 보내고, 받는 쪽은 같은 traceId를 이어 쓴다.
- RabbitMQ 메시지에도 실려 가서, **비동기 이벤트까지** 같은 trace로 묶인다.
- 각 서비스는 "내가 무엇을 얼마나 걸려 했는지"를 **span**으로 남겨 Zipkin에 보낸다.

## 3. 13장과 달라지는 점
| 항목 | 13장 | 14장 |
|---|---|---|
| 의존성 (서비스 7개) | - | OTel 브리지, Zipkin 내보내기, Boot 4 추적/Zipkin 모듈 (review는 DB 추적 추가) |
| bootstrap.yml | 이름, 설정 서버 | + 로그 형식 `[서비스,traceId,spanId]` |
| main() (5개) | - | `Hooks.enableAutomaticContextPropagation()` (스레드가 바뀌어도 traceId 유지) |
| composite WebClient | `@LoadBalanced WebClient.Builder` | 부트가 만든 Builder + 로드 밸런서 필터 → `WebClient` 빈 |
| composite 새 클래스 | - | `tracing/ObservationUtil` 등 3개: `product info` span, `productId`/`build.version` 태그 |
| config-repo | - | Zipkin 주소, 샘플링 100%, 로그 레벨 낮춤 |
| compose | - | `zipkin-server` (9411) |
| test-em-all.bash | - | 변경 없음 |

## 4. 문서별로 생기는 것
| 문서 | 이전 상태 → 이후 상태 |
|---|---|
| 01 | 핵심 서비스: 추적 의존성, 로그에 traceId, 리액터 컨텍스트 전파 |
| 02 | composite: WebClient 빈, 사용자 정의 span(`product info`), 빌드 버전 태그 |
| 03 | 인가 서버·Eureka·게이트웨이: 추적 의존성, 로그 형식 |
| 04 | config-repo: Zipkin 주소, 샘플링 100%, 로그 레벨 |
| 05 | compose에 Zipkin, 실행, 로그와 Zipkin에서 traceId 따라가기 |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 분산 추적 | 여러 서비스를 지나는 요청 하나의 경로와 시간을 이어서 기록하는 것 |
| trace / traceId | 요청 하나 전체. 그 요청의 모든 span이 같은 traceId를 가진다 |
| span / spanId | trace 안의 작업 하나(HTTP 호출 하나, DB 쿼리 하나 등). 시작·끝 시간과 태그를 가진다 |
| 전파 (propagation) | traceId를 다음 서비스로 넘기는 것. HTTP 헤더 `traceparent`, 메시지 헤더 |
| 샘플링 | 요청 중 몇 %를 추적할지. 1.0 = 전부 |
| Micrometer Tracing | 스프링이 쓰는 추적 API. 실제 구현은 OpenTelemetry(이 실습) 또는 Brave |
| Observation | Micrometer가 "어떤 작업을 관찰"하는 단위. 여기서 span과 메트릭이 만들어진다 |
| Zipkin | span을 모아 저장하고 화면으로 보여 주는 서버 |
| MDC | 로그에 붙는 스레드별 값. traceId가 여기 들어가서 로그 줄에 찍힌다 |
