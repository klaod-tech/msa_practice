# 11장 트러블슈팅

## 도커 없이 로컬에서 띄우면 토큰이 거부됨 (issuer 불일치)
**원인:** 인가 서버는 토큰 발급자(`iss`)를 `http://auth-server:9999`로 고정한다(compose 안의 이름). 로컬에서 게이트웨이/composite는 `issuer-uri: http://localhost:9999`라 이름이 맞지 않는다.
**해결:** compose로 띄워서 확인한다. 로컬 확인이 꼭 필요하면 issuer 대신 공개키 주소로만 검증하게 실행 옵션을 준다.
```bash
--spring.security.oauth2.resourceserver.jwt.issuer-uri= --spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/oauth2/jwks
```

## curl: `SSL certificate problem: self-signed certificate`
**원인:** 직접 만든 인증서라 신뢰할 수 없다고 본다.
**해결:** 실습에서는 curl에 `-k`, 브라우저는 경고 화면에서 "고급 → 계속".

## 다시 올렸더니 예전 토큰이 401
**원인:** 인가 서버가 시작할 때마다 서명 키를 새로 만든다.
**해결:** 토큰을 다시 받는다.

## Swagger UI Authorize: `Auth Error [invalid_request]: OAuth 2.0 Parameter: code_challenge`
**원인:** Spring Security 7(인가 서버 합쳐진 버전)부터 `ClientSettings.builder()`의 기본값이 `requireProofKey(true)`, 즉 authorization code 방식에 **PKCE**(`code_challenge`)가 필수다. 교재(SAS 1.x)는 기본값이 false라서 Swagger UI가 PKCE 없이 요청해도 됐다.
**해결 (교수님 코드와 다름):** 교수님 코드에는 이 설정이 없어서 Swagger UI 로그인에서 이 오류가 난다(test-em-all과는 무관). composite `application.yml`의 `springdoc.swagger-ui.oauth`에 한 줄 추가한다. Swagger UI가 `code_challenge`를 같이 보낸다.
```bash
code microservices/product-composite-service/src/main/resources/application.yml
```
`useBasicAuthenticationWithAccessCodeGrant: true` 바로 아래:
```yaml
      usePkceWithAuthorizationCodeGrant: true   # 교수님 코드와 다름 (Spring Security 7은 PKCE 필수)
```
그다음 `./gradlew :microservices:product-composite-service:build && docker compose up -d --build product-composite` → 브라우저 새로고침 후 다시 Authorize.
