# 9장: 넷플릭스 유레카를 사용한 서비스 검색

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 9장 (8장은 스프링 클라우드 소개라 코드 없음)
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4** (Spring Cloud Netflix Eureka, Spring Cloud LoadBalancer)
> 이전: [../chapter7/README.md](../chapter7/README.md) / 상태: **진행 중**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경` / `★ 삭제` = 7장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 9장과 다르게 쓰는 부분.

## 무엇을 하나
**먼저 [00-background.md](00-background.md)** 로 "왜 필요한지, 7장까지와 무엇이 다른지"를 본다.

한 줄 요약: composite가 `product:8080`처럼 **주소를 직접 적어 부르던 것**을, Eureka 서버에 등록된 **서비스 이름(`http://product`)으로 찾아 부르게** 바꾼다. 인스턴스가 여러 개면 돌아가며 부른다(클라이언트 측 로드 밸런싱).

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | DNS 방식의 문제, 7장까지와 달라지는 점, 용어 | 읽기 |
| 1 | [01-eureka-server.md](01-eureka-server.md) | 새 프로젝트 `spring-cloud/eureka-server` | `:spring-cloud:eureka-server:build` |
| 2 | [02-eureka-clients.md](02-eureka-clients.md) | 서비스 4개를 Eureka 클라이언트로 (의존성, yml, 테스트) | 컴파일 |
| 3 | [03-composite-load-balancing.md](03-composite-load-balancing.md) | composite: `@LoadBalanced WebClient.Builder`, `http://product` | composite `build` |
| 4 | [04-docker-compose-and-test.md](04-docker-compose-and-test.md) | compose에 eureka, test-em-all, 웹 화면, 인스턴스 늘리기, Eureka 중지 실험 | `End, all tests OK` |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

02를 하면 3개 핵심 서비스와 composite의 테스트가 Eureka 서버를 찾으려 하지 않도록 `eureka.client.enabled=false`가 들어간다. 03까지 끝나야 composite가 컴파일된다.

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| Spring Cloud 버전 | `2025.0.0` | `2025.1.4` (7장부터) | 01 |
| eureka-server의 `TestRestTemplate` | `org.springframework.boot.test.web.client` | **`org.springframework.boot.resttestclient`** + `@AutoConfigureTestRestTemplate` + 테스트 의존성 2줄 | 01 |
| eureka-server `healthy()` 테스트 기대값 | `{"status":"UP"}` | `{"groups":["liveness","readiness"],"status":"UP"}` | 01 |
| eureka-server Dockerfile | `jarmode=layertools`, Java 24 이미지 | 우리 서비스 Dockerfile과 같은 형식(`jarmode=tools`, `21-jre-noble`), 포트만 8761 | 01 |
| 서비스 이름 | `spring.application.name: product` 등 | 우리는 3장부터 `product-service` 등이었음 → **교재 값으로 변경** (Eureka 서비스 ID가 되고 composite가 `http://product`로 부름) | 02 |
| 6·7장부터 유지 | `spring.data.mongodb.*`, `server.error.include-message`, healthcheck `2s` 등 | `spring.mongodb.*`, `spring.web.error.include-message`, `timeout: 10s`, Jackson 3 설정 | 전체 |

## 검증한 것 / 안 한 것
- 7장 커밋(`11acccf`)을 복사한 곳에 이 문서의 파일을 그대로 넣고 **모든 프로젝트의 main/test 컴파일**을 통과했다.
- 도커 없이 도는 테스트 통과: eureka-server 3개, composite 8개, ReactorTests 2개.
- 다른 포트(18761, 17001, 17002)로 Eureka 서버와 composite jar를 직접 띄워 확인했다: composite가 Eureka에 `PRODUCT-COMPOSITE`로 등록됨, `product`로 등록된 인스턴스가 있으면 `http://product/...` 호출이 그 인스턴스의 실제 주소로 바뀌어 감, 없으면 `503 Service Unavailable`.
- Testcontainers를 쓰는 핵심 서비스 테스트와 docker compose 구동(04)은 실습 과정이라 직접 돌리지 않았다. 실패하면 troubleshooting.md에 추가한다.
