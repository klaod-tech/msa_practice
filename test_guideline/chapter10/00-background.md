# 00. 배경: 에지 서버, 9장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서. 교재 10장 앞부분 요약.

## 1. 9장까지 밖에서 보이던 것
```
내 PC(브라우저/curl) ──▶ :8080 product-composite
                    └─▶ :8761 eureka (웹 화면)
                         product / recommendation / review 는 원래 포트를 안 열었음
```
서비스마다 밖으로 포트를 열면, 클라이언트가 **서비스 구조를 다 알아야** 하고 보안 설정도 서비스마다 따로 해야 한다.

## 2. 10장: 에지 서버 하나만 연다 (교재 그림 10.1)
```
                          ┌────────────── 마이크로서비스 환경 (밖에서 직접 못 들어옴) ──────────────┐
클라이언트 ─▶ :8080 gateway ─┬─ /product-composite/** ─▶ product-composite ─▶ product, recommendation, review
                          ├─ /openapi/**           ─▶ product-composite (Swagger)
                          ├─ /eureka/**            ─▶ eureka (웹 화면, API)
                          └─ /headerrouting        ─▶ httpstat.us (예제용, 지금은 응답 없음)
                          └────────────────────────────────────────────────────────────────────┘
```
- **공개 API**(composite, Eureka 화면)는 게이트웨이를 통해서만 밖에서 접근된다.
- **비공개 API**(핵심 서비스 3개)는 안에서만 쓰인다. 게이트웨이에 라우트가 없으니 밖에서 부를 길이 없다.
- 앞으로 11장(보안: OAuth)도 이 한 곳에 붙인다.

## 3. 라우트 = ID + 대상 URI + 조건(predicate) + 필터(filter)
교재 yml의 첫 라우트:
```yaml
- id: product-composite
  uri: lb://product-composite        # 대상: Eureka에서 'product-composite'를 찾아 로드 밸런싱 (9장 @LoadBalanced와 같은 원리)
  predicates:
  - Path=/product-composite/**       # 조건: 경로가 이렇게 시작하면
```

| 부분 | 뜻 | 10장에서 쓰는 것 |
|---|---|---|
| `uri` | 보낼 곳. `lb://이름` = Eureka로 찾기, `http://호스트:포트` = 고정 주소 | `lb://product-composite`, `http://eureka:8761`, `http://httpstat.us` |
| `predicates` | 이 라우트를 탈 조건 (모두 만족해야 함) | `Path=...`, `Host=...` |
| `filters` | 보내기 전에 요청을 고침 | `SetPath=...` (경로를 바꿔서 보냄) |

라우트는 **위에서부터** 맞는 첫 번째 것을 탄다. 그래서 `Host` 조건이 있는 라우트(200, 418)를 위에, 조건 없는 501 라우트를 맨 아래에 둔다.

## 4. 9장과 달라지는 점 한눈에

| 항목 | 9장 | 10장 |
|---|---|---|
| 밖에 열린 포트 | composite `8080`, eureka `8761` | **gateway `8080` 하나** |
| 새 컨테이너 | - | `gateway` |
| composite 호출 | `localhost:8080/product-composite/1` (composite 직접) | **주소 그대로** `localhost:8080/product-composite/1` (이제 게이트웨이가 받아서 전달) |
| Eureka 웹 화면 | `localhost:8761` | `localhost:8080/eureka/web` |
| Eureka API | `localhost:8761/eureka/apps` | `localhost:8080/eureka/api/apps` |
| 헬스 체크(핵심 서비스 상태 모으기) | composite의 `HealthCheckConfiguration` | **gateway로 이동** (composite 상태까지 4개를 모음), composite에서는 삭제 |
| Eureka에 등록되는 앱 | 4개 | **5개** (gateway 추가) |
| composite yml | - | `server.forward-headers-strategy: framework`, `springdoc.cache.disabled: true` |

- 포트 번호가 8080 그대로라 **test-em-all.bash와 curl 명령은 거의 그대로** 쓴다. 받는 쪽이 composite에서 gateway로 바뀌었을 뿐이다.
- `forward-headers-strategy`: 게이트웨이가 붙여 주는 `X-Forwarded-Host` 같은 헤더를 composite가 믿고 쓰게 한다. 그래야 Swagger UI가 만드는 요청 주소가 composite 안쪽 주소가 아니라 **게이트웨이 주소**가 된다.

## 5. 문서별로 생기는 것
| 문서 | 내용 |
|---|---|
| 01 | 새 프로젝트 `spring-cloud/gateway` (라우트, 헬스 체크) |
| 02 | composite: 헬스 체크를 게이트웨이로 옮김 |
| 03 | settings.gradle, compose(게이트웨이만 8080 공개), test-em-all, 실행, 라우트 확인 |

헤더 라우팅 예제(`/headerrouting`)는 교수님 코드에서 `httpstat.us`로 보내는데, 이 사이트가 지금 응답하지 않아 결과를 볼 수 없다. test-em-all은 이 라우트를 테스트하지 않으므로 통과에는 영향이 없다. 직접 보고 싶으면 [troubleshooting.md](troubleshooting.md)의 httpbin 방법을 쓴다.
