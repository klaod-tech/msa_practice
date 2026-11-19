# 11장 트러블슈팅

## 인가 서버 컴파일 실패: `package org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers does not exist`
**원인:** Spring Security 7에서 Spring Authorization Server가 Spring Security에 합쳐지며 설정 클래스 패키지가 바뀌었다.
**해결:** [01](01-authorization-server.md) 4절처럼 import 두 줄을 바꾼다.
```java
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
```

## 인가 서버 테스트 컴파일 실패: `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`
**원인:** Boot 4에서 `@AutoConfigureMockMvc`가 별도 모듈로 옮겨졌다.
**해결:** build.gradle에 `testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'`, import는 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.

## 게이트웨이/composite가 안 뜸: `No qualifying bean of type 'org.springframework.security.oauth2.jwt.ReactiveJwtDecoder'`
**원인:** 교재처럼 `spring-security-oauth2-resource-server`, `spring-security-oauth2-jose`만 넣으면 Boot 4에서는 JWT 검증 자동 설정이 켜지지 않는다.
**해결:** 두 줄 대신 스타터 한 줄([03](03-gateway-https-security.md) 2절).
```groovy
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```
(composite 테스트는 `TestSecurityConfig`로 보안을 바꿔 끼워서 이 문제가 있어도 통과한다. 실제로 띄워야 드러난다.)

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
**해결:** composite `application.yml`의 `springdoc.swagger-ui.oauth`에 한 줄 추가([04](04-composite-security.md)). Swagger UI가 `code_challenge`를 같이 보낸다.
```yaml
      usePkceWithAuthorizationCodeGrant: true
```
그다음 `./gradlew :microservices:product-composite-service:build && docker compose up -d --build product-composite` → 브라우저 새로고침 후 다시 Authorize.
