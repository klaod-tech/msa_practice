# 00. 배경: 스프링 클라우드 서비스를 쿠버네티스 기능으로, 16장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서.

## 1. 16장까지
```
 밖 ─NodePort 30443─▶ gateway(스프링 클라우드 게이트웨이, HTTPS) ─▶ product-composite ...
 서비스들 ─시작할 때─▶ config-server (config-repo → ConfigMap)
```
- 게이트웨이와 설정 서버가 **스프링 클라우드 앱**으로 따로 돈다.
- 인증서(`edge.p12`)는 게이트웨이 jar 안에 직접 넣었다.

## 2. 17장이 끝나면
```
 밖 ─HTTPS 443─▶ Ingress (ingress-nginx, 인증서는 cert-manager가 발급) ─▶ product-composite, auth-server
 서비스마다 ─▶ 자기 ConfigMap(설정 파일) + Secret(비밀번호)을 파드에 파일/환경 변수로 받음
```
| 스프링 클라우드 (16장) | 쿠버네티스 기능 (17장) |
|---|---|
| config-server + config-repo | 서비스별 **ConfigMap**(설정 파일) + **Secret**(비밀값). `SPRING_CONFIG_LOCATION`으로 읽음 |
| gateway (라우트, HTTPS) | **Ingress**(경로별 라우팅) + **cert-manager**(인증서 자동 발급) |
| `bootstrap.yml` | 필요 없음(설정 서버에 안 물어봄) |

- 운영할 서버가 두 개 줄어서 시스템이 단순해진다. 도커 컴포즈 구성(로컬 실행)에는 게이트웨이가 남아 있다.

## 3. 16장과 달라지는 점
| 항목 | 16장 | 17장 |
|---|---|---|
| config-server | 있음 | **삭제** (프로젝트, compose, 차트) |
| 서비스 설정 | config-server에서 받음 | 서비스 차트의 ConfigMap을 파일로 받음 (`bootstrap.yml`, `src/main/resources` 삭제) |
| 밖에서 들어오는 길 | gateway NodePort 30443 | **Ingress** (`minikube.me`, 443). 쿠버네티스에서는 gateway 차트 삭제 |
| 인증서 | jar 안 `edge.p12` | cert-manager가 만든 Secret |
| 의존성 | 설정 클라이언트 | 설정 클라이언트 삭제 |
| 실행 준비 | - | `minikube addons enable ingress`, cert-manager 설치 |

## 4. 용어
| 용어 | 뜻 |
|---|---|
| Ingress / 인그레스 컨트롤러 | 밖의 HTTP(S) 요청을 경로·호스트별로 서비스에 보내는 규칙 / 그 규칙을 실제로 처리하는 프로그램(ingress-nginx) |
| cert-manager | 인증서를 만들고 갱신해 Secret에 넣어 주는 쿠버네티스 도구 |
| Issuer / Certificate | cert-manager에게 "누가 서명할지" / "어떤 인증서를 만들지" 알려 주는 객체 (실습은 자체 서명) |
| Secret | 비밀번호 등을 담는 쿠버네티스 객체 |
| `SPRING_CONFIG_LOCATION` | 스프링이 설정 파일을 읽을 위치. ConfigMap이 붙은 폴더를 가리킨다 |
