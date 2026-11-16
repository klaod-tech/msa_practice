# 10장: 스프링 클라우드 게이트웨이를 에지 서버로 사용

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 10장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4** (Spring Cloud Gateway Server WebFlux, Eureka)
> 이전: [../chapter9/README.md](../chapter9/README.md) / 상태: **진행 중**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경` / `★ 삭제` = 9장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = 교재 10장과 다르게 쓰는 부분.

## 무엇을 하나
**먼저 [00-background.md](00-background.md)** 로 "에지 서버가 무엇이고 9장과 무엇이 다른지"를 본다.

한 줄 요약: 밖에서 들어오는 모든 요청이 **게이트웨이(8080) 하나**를 거친다. composite와 Eureka는 밖으로 열린 포트를 닫고, 핵심 서비스 3개는 원래부터 닫혀 있다(교재 그림 10.1).

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | 에지 서버, 라우트(조건/필터), 9장과 달라지는 점 | 읽기 |
| 1 | [01-gateway-project.md](01-gateway-project.md) | 새 프로젝트 `spring-cloud/gateway` (라우트, 헬스 체크) | `:spring-cloud:gateway:build` |
| 2 | [02-composite-changes.md](02-composite-changes.md) | composite에서 헬스 체크 제거, 게이트웨이 뒤에서 쓸 설정 2줄 | composite `build` |
| 3 | [03-docker-compose-and-test.md](03-docker-compose-and-test.md) | compose(포트 정리, gateway, httpbin), test-em-all, 라우트별 수동 확인 | `End, all tests OK` |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| 헬스 체크 클래스 패키지 | `org.springframework.boot.actuate.health.*` | `org.springframework.boot.health.contributor.*` (7장과 같은 이유) | 01 |
| 헤더 라우팅 예제의 대상 | 외부 사이트 `http://httpstat.us`, `SetPath=/200` | **compose의 `httpbin` 컨테이너** `http://httpbin:8080`, `SetPath=/status/200` (httpstat.us는 지금 응답 없음) | 01, 03 |
| Dockerfile | Java 24, `jarmode=layertools` | 우리 형식(`21-jre-noble`, `jarmode=tools`) | 01 |
| Spring Cloud 버전 | `2025.0.0` | `2025.1.4` (7장부터) | 01 |

## 검증한 것 / 안 한 것
- 9장 확인본에 이 문서의 파일을 그대로 넣고 **모든 프로젝트의 main/test 컴파일**을 통과했다.
- 도커 없이 도는 테스트 통과: gateway 1개, eureka-server 3개, composite 8개.
- Eureka(8761), gateway(18080), composite(17001) jar를 직접 띄워 확인했다(헤더 라우팅만 대상 주소를 공개 `httpbin.org`로 바꾼 설정 복사본 사용, 경로는 같음):
  - `/eureka/api/apps` → gateway, product-composite 2개 등록, `/eureka/web` → 200
  - `/product-composite/**` → composite로 전달, `/openapi/swagger-ui.html` → 302(정상 리다이렉트), api-docs의 서버 주소가 게이트웨이 주소
  - `/headerrouting`: `Host: i.feel.lucky:8080` → 200, `Host: im.a.teapot:8080` → 418, 그 외 → 501
  - `/actuator/gateway/routes` → 라우트 8개
- docker compose 구동(03)과 핵심 서비스 Testcontainers 테스트는 실습 과정이라 직접 돌리지 않았다. 실패하면 troubleshooting.md에 추가한다.
