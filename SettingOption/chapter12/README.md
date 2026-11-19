# 12장: 구성 중앙화 (스프링 클라우드 컨피그 서버)

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 12장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Cloud 2025.1.4**
> 이전: [../chapter11/README.md](../chapter11/README.md) / 다음: [../chapter13/README.md](../chapter13/README.md) / 상태: **완료**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경` / `★ 삭제` = 11장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = 교재 12장과 다르게 쓰는 부분.

## 무엇을 하나
**먼저 [00-background.md](00-background.md)** 로 "왜 설정을 한곳에 모으는지, 11장과 무엇이 다른지"를 본다.

서비스 7개가 각자 들고 있던 `application.yml`을 **설정 서버(config-server)** 한 곳으로 모은다. 서비스는 시작할 때 설정 서버에서 자기 설정을 받아 간다. 비밀번호는 설정 파일에 **암호문**으로 적는다.

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | 설정 서버가 하는 일, 시작 순서, 11장과 달라지는 점 (먼저 읽기) | 읽기 |
| 1 | [01-config-server.md](01-config-server.md) | 새 프로젝트 `spring-cloud/config-server` | `:spring-cloud:config-server:build` |
| 2 | [02-config-repo.md](02-config-repo.md) | `.env`, `config-repo/` 설정 파일 8개(암호문 포함) | (선택) 설정 서버 로컬 실행, curl |
| 3 | [03-config-clients.md](03-config-clients.md) | 서비스 7개: 의존성, `bootstrap.yml`, `application.yml` 삭제, 테스트 | `./gradlew build` |
| 4 | [04-docker-compose-and-test.md](04-docker-compose-and-test.md) | compose(config-server), test-em-all, 설정 조회, 암호화/복호화 | `End, all tests OK` |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| 재시도용 AOP 의존성 | `spring-boot-starter-aop` | **`spring-boot-starter-aspectj`** (Boot 4에서 이름 바뀜, 예전 이름은 버전을 못 찾음) | 03 |
| 테스트용 `application.yml` | `server.error.include-message`, Jackson 설정 없음 | `spring.web.error.include-message` + Jackson `allow-final-fields-as-mutators` (없으면 composite `MessagingTests` 2개 실패) | 03 |
| config-repo 값 | 교재 11장 application.yml 기준 | 이 실습 11장 application.yml 기준: `spring.mongodb`, `spring.web.error.include-message`, Jackson 줄, httpbin 라우트, Swagger PKCE 줄을 그대로 옮김 | 02 |
| config-server Dockerfile | Java 24, `layertools` | Java 21, `-Djarmode=tools` (다른 서비스와 같은 형식, 4장부터) | 01 |
| 인가 서버 `AuthorizationServerConfig` | import 한 줄 추가 | 추가하지 않음 (쓰이지 않는 import) | - |
| 인가 서버 테스트용 설정 파일 | `ml.`로 시작하는 쓰이지 않는 설정 | 만들지 않음 | 03 |

## 검증한 것 / 안 한 것
- 저장소(11장 완료 커밋)에 이 문서의 코드 블록만 적용해 12장 상태를 만들었고, 검증한 파일과 똑같이 만들어지는 것을 비교로 확인했다.
- 테스트 통과: config-server 1개, eureka-server 3개, gateway 1개, authorization-server 4개, composite 8개, product 13개, recommendation 15개, review 15개(핵심 서비스는 Testcontainers).
- jar를 직접 띄워 확인했다(로컬, native 프로필):
  - 설정 서버: 계정 없으면 401, 교재 암호문 5개가 `p`, `guest`, `pwd`, `password`, `secret-writer`로 풀림, `/encrypt` → `/decrypt` 왕복
  - Eureka, 인가 서버, 게이트웨이가 설정 서버에서 설정을 받아 시작(`Located environment: name=...`), Eureka 기본 인증(암호문으로 받은 `p`) 동작, 게이트웨이 HTTPS(암호문으로 받은 인증서 비밀번호) 동작
  - 게이트웨이 `/config/**` 라우트로 설정 조회(계정 없으면 401), 암호화/복호화
  - 설정 서버를 늦게 켜도 클라이언트가 retry로 기다렸다가 시작
- 실습에서 확인: 전체 빌드(테스트 포함), docker compose 구동과 `test-em-all.bash`.
- 실습 중 막힌 것: recommendation 테스트 하나에 `spring.cloud.config.enabled=false`를 빠뜨렸고, 02에서 띄운 설정 서버가 8888에 남아 있어서 테스트가 `Unauthorized`로 실패했다. 테스트 파일을 03대로 고치고 설정 서버를 끄니 통과(troubleshooting.md).
