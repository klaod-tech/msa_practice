# 00. 배경: OAuth 2.0으로 무엇을 막나, 10장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서. 교재 11장 앞부분 요약.

## 1. 10장까지
```
누구나 ──http──▶ :8080 gateway ─▶ product-composite (GET/POST/DELETE 아무나)
                              └▶ /eureka/web, /eureka/api (아무나)
```
- 통신이 **평문 HTTP**라 중간에서 엿볼 수 있다.
- **누구든** 상품을 만들고 지울 수 있다. Eureka 화면도 누구나 본다.

## 2. 11장이 끝나면
```
                ① 토큰 요청 (clientId/secret 또는 사용자 로그인)
 클라이언트 ─https─▶ :8443 gateway ─/oauth2/**─▶ auth-server(인가 서버) ── JWT 발급 (서명됨)
     │
     │ ② Authorization: Bearer <JWT>
     └─https─▶ :8443 gateway ── JWT 서명/만료 확인 ──▶ product-composite ── scope 확인
                                  (리소스 서버)              (리소스 서버)   GET: product:read
                                                                            POST/DELETE: product:write
 Eureka: 아이디/비밀번호(u / p) 있어야 접근 (HTTP 기본 인증)
```
- **HTTPS는 게이트웨이까지만**이다. 안쪽(게이트웨이 → composite 등)은 평문 HTTP 그대로다(18장 서비스 메시에서 다룸).
- 토큰 검증은 JWT의 **서명**만 확인하면 돼서, 요청마다 인가 서버에 물어보지 않는다. 서명 검증용 공개키(jwk-set)만 한 번 받아 온다.

## 3. OAuth 2.0 역할
| 역할 | 이 실습에서 | 하는 일 |
|---|---|---|
| 리소스 소유자 | 사용자 `u` (비밀번호 `p`) | 자기 데이터 접근을 허락하는 사람 (브라우저 로그인, 동의 화면) |
| 클라이언트 | `writer`, `reader` (인가 서버에 미리 등록) | 토큰을 받아 API를 부르는 앱. test-em-all, Swagger UI, curl |
| 인가 서버 | `auth-server` (Spring Authorization Server, 포트 9999) | 클라이언트/사용자 확인 후 **접근 토큰(JWT) 발급** |
| 리소스 서버 | `gateway`, `product-composite` | 요청의 토큰을 **검증**하고 scope로 허용/거부 |

| 클라이언트 | 받을 수 있는 scope | 할 수 있는 것 |
|---|---|---|
| `writer` / `secret-writer` | `product:read`, `product:write` | 조회, 생성, 삭제 |
| `reader` / `secret-reader` | `product:read` | 조회만 (삭제하면 **403**) |

## 4. 토큰 받는 방법 (grant type)
| 방법 | 누가 | 이 실습에서 |
|---|---|---|
| **client credentials** | 사용자 없이 **앱 자신**이 아이디/비밀번호(clientId/secret)로 | test-em-all, curl |
| **authorization code** | **사용자가 브라우저로 로그인**하고 동의 → 코드 → 토큰 교환 | Swagger UI의 Authorize 버튼, 교재의 브라우저 실습 |

OpenID Connect(OIDC)는 OAuth 2.0 위에 "사용자가 누구인지"(ID 토큰)를 더한 것이다. scope에 `openid`가 그것.

## 5. 10장과 달라지는 점
| 항목 | 10장 | 11장 |
|---|---|---|
| 밖에서 부르는 주소 | `http://localhost:8080` | **`https://localhost:8443`** (자체 서명 인증서라 curl은 `-k`, 브라우저는 경고 화면에서 진행) |
| 새 컨테이너 | - | **`auth-server`** (밖에 포트 안 염, 게이트웨이의 `/oauth2/**`, `/login/**`로 접근) |
| composite API 호출 | 그냥 호출 | **`Authorization: Bearer <토큰>`** 헤더 필요. 없으면 401, scope 부족하면 403 |
| Eureka | 누구나 | **`u` / `p`** 필요. 서비스들도 `http://u:p@eureka:8761/eureka/`로 등록 |
| Eureka에 등록되는 앱 | 5개 | **6개** (auth-server 추가) |
| 게이트웨이 | 라우팅만 | + HTTPS, JWT 검증, 인가 서버 라우트 3개, 헬스 체크에 auth-server |
| composite | 보안 없음 | `SecurityConfig`(scope별 권한), `OpenApiConfig`(Swagger OAuth), 요청자 정보 로그 |
| 테스트 | 그대로 | composite 테스트는 `TestSecurityConfig`로 보안을 끄고 돈다 |
| test-em-all.bash | 그냥 curl | 시작할 때 **토큰을 받아** 모든 호출에 붙임, 401/403 확인 추가 |

## 6. 용어
| 용어 | 뜻 |
|---|---|
| JWT (JSON Web Token) | `헤더.내용.서명` 세 부분. 내용에 누가(`sub`), 무엇을(`scope`), 언제까지(`exp`), 누가 발급(`iss`)이 들어 있다 |
| scope | 토큰이 허락하는 범위. 스프링에서는 `SCOPE_product:read` 같은 권한(authority)으로 바뀐다 |
| 401 Unauthorized | 누구인지 모름 (토큰 없음/잘못됨) |
| 403 Forbidden | 누구인지는 알지만 권한이 없음 (scope 부족) |
| HTTP 기본 인증 | 요청마다 `아이디:비밀번호`를 보냄. 간단하지만 HTTPS와 함께 써야 안전 |
| keystore (`edge.p12`) | HTTPS 서버의 인증서와 개인키를 담은 파일 |
