# 11장: API에 대한 접근 보안

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 11장
> 실습 환경: **Java 21 / Spring Boot 4.0.9 / Spring Security 7.0 / Spring Cloud 2025.1.4**
> 이전: [../chapter10/README.md](../chapter10/README.md) / 다음: [../chapter12/README.md](../chapter12/README.md) / 상태: **완료**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경` / `★ 삭제` = 10장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = 교재 11장과 다르게 쓰는 부분.

## 무엇을 하나
**먼저 [00-background.md](00-background.md)** 로 "OAuth 2.0에서 누가 무슨 역할인지, 10장과 무엇이 다른지"를 본다.

교재 "시스템 환경 보호"의 세 단계:
1. 밖에서 오는 요청은 **HTTPS**(게이트웨이 8443)로 암호화한다.
2. **OAuth 2.0 / OpenID Connect**: 인가 서버가 발급한 **JWT 접근 토큰**이 있어야 API를 부를 수 있다. 읽기(`product:read`)와 쓰기(`product:write`) 권한을 나눈다.
3. **Eureka**는 HTTP 기본 인증(아이디/비밀번호)으로 막는다.

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 0 | [00-background.md](00-background.md) | OAuth 역할, 토큰 흐름, 10장과 달라지는 점 (먼저 읽기) | 읽기 |
| 1 | [01-authorization-server.md](01-authorization-server.md) | 새 프로젝트 `spring-cloud/authorization-server` | `:spring-cloud:authorization-server:build` |
| 2 | [02-eureka-basic-auth.md](02-eureka-basic-auth.md) | Eureka 기본 인증, 서비스 4개의 Eureka 주소에 계정 | eureka-server `build` |
| 3 | [03-gateway-https-security.md](03-gateway-https-security.md) | 게이트웨이 HTTPS(인증서), JWT 검증, 인가 서버 라우트 | gateway `build` |
| 4 | [04-composite-security.md](04-composite-security.md) | composite의 scope별 권한, Swagger OAuth 설정, 테스트 | composite `build` |
| 5 | [05-docker-compose-and-test.md](05-docker-compose-and-test.md) | compose(auth-server, 8443), test-em-all, 토큰 받아 호출, 401/403 | `End, all tests OK` |
| 6 | [06-auth0.md](06-auth0.md) | (선택) Auth0 테넌트 준비: env.bash, setup-tenant.bash | 스크립트 마지막 `export ...` 출력 |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| 인가 서버 설정 클래스 패키지 | `org.springframework.security.oauth2.server.authorization.config.annotation.web...` | **`org.springframework.security.config.annotation.web...`** (Spring Security 7에 합쳐짐) | 01 |
| 인가 서버 테스트의 `@AutoConfigureMockMvc` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` + `spring-boot-starter-webmvc-test` | 01 |
| 리소스 서버(JWT 검증) 의존성 | `spring-security-oauth2-resource-server`, `spring-security-oauth2-jose` | **`spring-boot-starter-oauth2-resource-server`** (없으면 `ReactiveJwtDecoder` 빈이 없어서 게이트웨이가 안 뜸) | 03, 04 |
| 게이트웨이 인증서 `edge.p12` | 교재 소스에 있는 파일 | `keytool` 명령으로 직접 만든다(비밀번호 `password`, 별칭 `localhost`는 교재와 같음) | 03 |
| Swagger UI authorization code 로그인 | PKCE 설정 없음 (SAS 1.x는 PKCE 선택) | **`usePkceWithAuthorizationCodeGrant: true`** 추가 (Spring Security 7은 PKCE 기본 필수, 없으면 `code_challenge` 오류) | 04 |
| auth-server healthcheck `timeout` | `2s` | `10s` (6장 트러블슈팅) | 05 |
| 헤더 라우팅 대상 | httpstat.us | httpbin 컨테이너 (10장부터) | 03 |
| Auth0 `.gitignore` | `ml.*`만 제외 | **`env.bash`를 제외** (비밀값이 커밋되지 않게) | 06 |

## 검증한 것 / 안 한 것
- 10장 문서의 파일을 저장소(9장 커밋)에 적용해 10장 상태를 다시 만들고(컴파일, 테스트 통과), 그 위에 이 문서의 파일을 넣어 **모든 프로젝트의 main/test 컴파일**을 통과했다.
- 도커 없이 도는 테스트 통과: authorization-server 4개(client_credentials 토큰 발급 포함), eureka-server 3개, gateway 1개, composite 8개.
- Eureka(8761), 인가 서버(9999), 게이트웨이(HTTPS 8443), composite jar를 직접 띄워 확인했다(로컬이라 토큰 검증만 `jwk-set-uri`로 바꿔 띄움, troubleshooting 참고):
  - Eureka: 계정 없으면 401, `u:p`로 3개 앱 등록 확인
  - writer/reader 토큰 발급, JWT에 `scope`, `iss: http://auth-server:9999`
  - 토큰 없이 GET 401, reader로 DELETE 403, writer/reader로 GET은 보안 통과(composite 로그에 `Authorization info: Subject: writer, scopes: [...]`)
  - Swagger api-docs: 서버 주소 `https://localhost:8443`, 보안 스킴 `oauth2`
- 실습에서 확인: docker compose 구동과 `test-em-all.bash`, 토큰으로 직접 호출(05 4절), Swagger UI 로그인(authorization code) 후 GET 200. Swagger 로그인은 처음에 `code_challenge` 오류가 나서 PKCE 설정을 추가했다(troubleshooting.md).
