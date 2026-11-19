# 13장: Resilience4j를 사용한 탄력성 개선

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 13장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4 / Resilience4j 2.4.0**
> 이전: [../chapter12/README.md](../chapter12/README.md) / 상태: **진행 중**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기).
- `★ 추가` / `★ 변경` / `★ 삭제` = 12장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = 교재 13장과 다르게 쓰는 부분.

## 무엇을 하나
**먼저 [00-background.md](00-background.md)** 로 "서킷 브레이커가 왜 필요하고 상태가 어떻게 바뀌는지"를 본다.

composite가 product를 부를 때 **타임아웃(2초), 재시도(최대 3번), 서킷 브레이커**를 건다. 시험해 보려고 product에 일부러 느리게/실패하게 하는 파라미터(`delay`, `faultPercent`)를 넣는다. 새 프로젝트나 compose 변경은 없다.

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | 연쇄 장애, 서킷 브레이커 상태, 12장과 달라지는 점 (먼저 읽기) | 읽기 |
| 1 | [01-product-delay-fault.md](01-product-delay-fault.md) | API에 `delay`/`faultPercent`, product가 일부러 늦게/실패 | (02에서 같이 빌드) |
| 2 | [02-composite-resilience4j.md](02-composite-resilience4j.md) | 의존성, `@CircuitBreaker`/`@TimeLimiter`/`@Retry`, 대체 응답, 설정, 테스트 | composite·product `build` |
| 3 | [03-docker-compose-and-test.md](03-docker-compose-and-test.md) | test-em-all 서킷 브레이커 테스트, 직접 열고 닫아 보기, 재시도 | `End, all tests OK` |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| Resilience4j 모듈 | `resilience4j-spring-boot3` 2.3.0 | **`resilience4j-spring-boot4` 2.4.0** (Boot 4용 모듈은 2.4.0부터) | 02 |
| AOP 의존성 | `spring-boot-starter-aop` 한 번 더 | 추가 안 함 (12장의 `spring-boot-starter-aspectj`가 이미 있음) | 02 |
| ProductCompositeIntegration | 교재 12장 코드 기준 | 이 실습 12장 코드 기준(Jackson 3 import 등 그대로) | 02 |
| config-repo/product-composite.yml | 교재 12장 파일 + resilience4j | 이 실습 12장 파일(Boot 4 차이 포함) + resilience4j | 02 |

## 검증한 것 / 안 한 것
- 저장소(11장 커밋) + 12장 문서 적용 상태에 이 문서의 코드 블록만 적용해 13장 상태를 만들었고, 검증한 파일과 똑같이 만들어지는 것을 비교로 확인했다.
- 테스트 통과: composite 8개, product 13개(Testcontainers). 전체 컴파일 통과.
- jar를 직접 띄워 확인했다(설정 서버, Eureka, 인가 서버, product, composite, 임시 MongoDB):
  - `/actuator/health`에 `circuitBreakers.product.state: CLOSED` (Boot 4용 모듈의 health 연동)
  - `?delay=3` → 500, `Did not observe any item or terminal signal within 2000ms`
  - 회로 OPEN → `Fallback product1`(200), productId 13 → 404 `not found in fallback cache!`
  - 10초 뒤 `HALF_OPEN` → 정상 3번 → `CLOSED`, `/actuator/circuitbreakerevents`에 전환 3개 기록
- 안 한 것: docker compose 구동과 `test-em-all.bash`(실습 과정), 재시도(`faultPercent`)를 따로 관찰하는 것.
