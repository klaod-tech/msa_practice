# 00. 배경: 서킷 브레이커는 왜 필요한가, 12장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서. 교재 13장 앞부분 요약.

## 1. 12장까지
```
 client ─▶ gateway ─▶ product-composite ─▶ product (느림/죽음)
                            │  응답 올 때까지 계속 기다림
                            ▼
                 요청이 쌓임 → composite도 느려짐 → gateway도 느려짐
```
- product 하나가 느려지면 composite는 **끝없이 기다린다.** 기다리는 요청이 쌓여 composite도 느려지고, 결국 전체가 같이 느려진다(**연쇄 장애**).
- 잠깐 실패한 요청(일시적 네트워크 오류 등)도 그냥 실패로 끝난다.

## 2. 13장이 끝나면
```
 composite.getProduct ─▶ [재시도] ─▶ [타임아웃 2초] ─▶ [서킷 브레이커] ─▶ product
                         500이면                       실패가 많으면 회로를 열어
                         1초 뒤 최대 3번                product를 아예 부르지 않고
                                                       대체 응답(Fallback product1)을 바로
```
- **타임아웃**: 2초 넘게 기다리지 않는다.
- **재시도**: product가 500을 주면 1초 뒤 다시(최대 3번). 잠깐의 오류는 사용자가 모르게 넘어간다.
- **서킷 브레이커**: 최근 5번 중 절반 이상 실패하면 **회로를 연다.** 열려 있는 동안은 product를 부르지 않고 바로 대체 응답을 준다(fail fast). product가 회복할 시간을 벌어 준다.

## 3. 서킷 브레이커 상태
```
            최근 5번 중 50% 이상 실패
  CLOSED ───────────────────────────▶ OPEN ── 호출 안 함, 바로 대체 응답
    ▲                                  │
    │ 시험 호출 3번 성공                 │ 10초 뒤 저절로
    │                                  ▼
    └──────────────────────────── HALF_OPEN ── 시험 호출 3번만 통과
                시험 호출 실패 → 다시 OPEN
```
| 상태 | 뜻 (전기 차단기 비유) |
|---|---|
| CLOSED | 회로가 닫혀 전기가 흐른다 = 평소처럼 product를 부른다 |
| OPEN | 차단기가 내려갔다 = product를 부르지 않는다 |
| HALF_OPEN | 조심스럽게 다시 올려 본다 = 몇 번만 시험으로 부른다 |

404(없음), 422(잘못된 입력)는 product가 **정상적으로** 답한 것이라 실패로 세지 않는다.

## 4. 12장과 달라지는 점
| 항목 | 12장 | 13장 |
|---|---|---|
| `GET /product/{id}`, `GET /product-composite/{id}` | 파라미터 없음 | `?delay=초&faultPercent=%` (선택, 기본 0) |
| product 서비스 | 그대로 응답 | `delay`만큼 늦게, `faultPercent` 확률로 500 |
| composite 의존성 | - | `resilience4j-spring-boot4`, `resilience4j-reactor` |
| composite `getProduct` | 그냥 WebClient 호출 | `@Retry` + `@TimeLimiter` + `@CircuitBreaker` + 대체 메서드 |
| config-repo `product-composite.yml` | - | `resilience4j.*` 설정, health에 회로 상태 |
| test-em-all.bash | - | `testCircuitBreaker` (회로 열기 → 대체 응답 → 10초 → 닫기) |
| compose, 다른 서비스 | - | 변경 없음 |

## 5. 문서별로 생기는 것
| 문서 | 이전 상태 → 이후 상태 |
|---|---|
| 01 | product가 항상 바로 응답 → `delay`/`faultPercent`로 일부러 늦게/실패 (composite는 아직 컴파일 안 됨) |
| 02 | composite가 product를 무작정 기다림 → 타임아웃·재시도·서킷 브레이커, 빌드 성공 |
| 03 | 수동 확인 → test-em-all이 회로를 열고 닫는 것까지 자동 확인 |

## 6. 용어
| 용어 | 뜻 |
|---|---|
| 회복성(resilience) | 일부가 고장 나도 전체가 버티는 성질 |
| 연쇄 장애 | 한 서비스의 느림/고장이 부르는 쪽으로 번지는 것 |
| 서킷 브레이커 | 실패가 많으면 호출을 끊어 장애가 번지지 않게 하는 장치 |
| fallback (대체 응답) | 회로가 열렸을 때 대신 돌려주는 값 |
| fail fast | 오래 기다렸다 실패하는 대신 바로 실패(또는 대체 응답) |
| 슬라이딩 윈도 | 판단에 쓰는 "최근 N번 호출" 묶음 (여기서는 5번) |
| Resilience4j | 서킷 브레이커, 재시도, 타임아웃 등을 제공하는 라이브러리 |
