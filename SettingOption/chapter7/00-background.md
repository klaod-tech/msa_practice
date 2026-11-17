# 00. 배경: 6장과 무엇이 다른가 (반응형 마이크로서비스)

> 코드 없이 읽기만 하는 문서. 7장 전체를 한눈에 보는 요약.

## 1. 6장까지
- 모든 호출이 **블로킹**이었다. composite가 product를 부르면 응답이 올 때까지 그 스레드가 멈춰서 기다렸고, 세 서비스를 **차례로** 불렀다.
- 생성/삭제도 HTTP라서 composite는 세 서비스가 **모두 저장을 끝낼 때까지** 기다렸다. 하나라도 죽어 있으면 생성이 실패했다.

## 2. 7장이 끝나면 (교재 "논블로킹 동기 API와 이벤트 기반 비동기 서비스 중에서 선택하기")
```
                          GET (WebClient, 논블로킹 HTTP, 세 서비스 동시에)
            ┌──────────────────────────────────────────────────────────────┐
 client ──▶ product-composite                                               ▼
            │  POST/DELETE → 이벤트 게시 → 바로 202     product / recommendation / review
            ▼                                                   ▲ (messageProcessor가 꺼내서 처리)
       RabbitMQ (또는 Kafka) ── 토픽: products, recommendations, reviews ─┘
```
| 연산 | 방식 | 이유 |
|---|---|---|
| 조회 GET | **논블로킹 동기** API (`Mono`/`Flux`) | 응답을 기다리는 사용자가 있다. 대신 스레드를 막지 않는다 |
| 생성/삭제 POST/DELETE | **이벤트 기반 비동기** | 받는 서비스가 잠깐 죽어 있어도 메시지가 큐에 남았다가 처리된다. composite는 기다리지 않는다 |

## 3. 6장과 달라지는 점
| 항목 | 6장 | 7장 |
|---|---|---|
| 반환형 | `Product`, `List<Review>`, `void` | **`Mono<Product>`, `Flux<Review>`, `Mono<Void>`** |
| MongoDB 리포지토리 | `CrudRepository` (블로킹) | **`ReactiveCrudRepository`** |
| MySQL(JPA) | 블로킹 그대로 | 여전히 블로킹 → **`jdbcScheduler`**(별도 스레드 풀)에서 실행 |
| composite가 핵심 서비스 부르기 | `RestTemplate`, 차례로 | **`WebClient`**, 세 개를 **동시에** 부르고 `Mono.zip`으로 합침 |
| 핵심 서비스의 POST/DELETE HTTP API | 있음 | **없어짐** (이벤트로만 생성/삭제) |
| composite POST/DELETE 응답 | 200 (저장 끝난 뒤) | **202 Accepted** (받았고 처리는 나중) |
| 새 클래스 | - | `Event`(api), `MessageProcessorConfig`(핵심 서비스마다), `HealthCheckConfiguration`(composite) |
| compose | DB 2개 | **`rabbitmq`** 추가, 파티션용/Kafka용 compose 파일 2개 추가 |
| test-em-all.bash | 생성 직후 바로 조회 | 이벤트가 처리될 때까지 **기다렸다가** 조회(`waitForMessageProcessing`) |

## 4. 메시징 기본 용어
| 용어 | 뜻 |
|---|---|
| 토픽(topic) / exchange | 이벤트를 보내는 곳. `products`, `recommendations`, `reviews` |
| 큐(queue) | 받는 쪽마다 생기는 줄. `products.productsGroup` 등 |
| 소비자 그룹(group) | 같은 그룹의 인스턴스들은 메시지를 **나눠서** 받는다 |
| DLQ (dead-letter queue) | 재시도해도 처리 못 한 메시지를 따로 모아 두는 큐 |
| 파티션 | 같은 키(productId)의 메시지가 **항상 같은 인스턴스**로 가게 나누는 것 (순서 보장) |
| 바인더 | 브로커 연결 부분. RabbitMQ ↔ Kafka를 설정(`defaultBinder`)만으로 바꾼다 |

## 5. 리액터 기본
| 타입 | 뜻 |
|---|---|
| `Mono<T>` | 값이 0개 또는 1개 나올 스트림 |
| `Flux<T>` | 값이 0개 이상 나올 스트림 |

스트림은 **구독해야 실행된다.** 웹 요청은 웹플럭스가 구독해 주고, 테스트나 메시지 처리처럼 끝날 때까지 기다려야 하는 곳에서만 `block()`을 쓴다([02](02-reactor-basics.md)).
