# 1장: Ubuntu 환경 설정 (교재 22장, 처음 한 번만)

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손)
> 범위: **WSL2 + Ubuntu 24.04에 실습 도구 설치** (교재 22장 "WSL2 개발 환경 설정")
> 다음: [../chapter2/README.md](../chapter2/README.md) / 명령은 실제 작업 기록 중 **성공한 경로만** 추렸다.

> **도구가 이미 설치되어 있으면 이 장은 건너뛰고 [2장](../chapter2/README.md)부터 시작한다.**

## 진행 순서
| 순서 | 문서 | 내용 |
|---|---|---|
| 1 | [01-ubuntu-environment.md](01-ubuntu-environment.md) | WSL2 + Ubuntu 24.04, Docker Desktop, VS Code(WSL·Java 확장), apt 도구, Helm, SDKMAN → Java 21·Spring CLI, kubectl·minikube·istioctl |
| - | [troubleshooting.md](troubleshooting.md) | 설치 중 시행착오 |

## 설치 결과 요약

| 도구 | 버전 | 설치 방법 | 문서 |
|---|---|---|---|
| Ubuntu (WSL2) | 24.04.5 LTS | Windows에서 WSL로 설치 | 01 |
| Docker | 29.0.1 (Compose v2.40.3) | Windows용 Docker Desktop의 WSL 연동 | 01 |
| VS Code | - | Windows에 설치한 뒤 WSL 원격 연결(`code .`) | 01 |
| git | 2.43.0 | Ubuntu 기본 포함 | - |
| jq / zip / unzip / siege | jq-1.7 / - / - / 4.0.7 | apt | 01 |
| Helm | v3.22.1 | 공식 설치 스크립트 | 01 |
| Java | 21.0.13 (Temurin) | SDKMAN | 01 |
| Spring Boot CLI | 4.0.7 | SDKMAN | 01 |
| kubectl | v1.37.2 | 바이너리 다운로드 | 01 |
| minikube | v1.39.0 (Kubernetes 1.37.0) | 바이너리 다운로드 | 01 |
| istioctl | 1.31.4 | Istio 다운로드 스크립트 | 01 |

> 교재는 Spring Boot 3 / Java 17 기준(교재 코드는 Java 24 / Boot 3.5.0)이지만, 이 실습은 **Java 21 / Spring Boot 4.0.9**로 진행한다.

## 캡처·커밋 지점
각 시점에서 **브라우저 화면**과 **터미널 화면**을 캡처하고 커밋한다(`git add .` → `git commit -m "..."`, 2장 02의 5-1).

| 시점(문서·절) | 브라우저 주소 | 터미널에서 보일 결과 | 커밋 메시지 예시 |
|---|---|---|---|
| 01 5절 버전 확인 | (없음) VS Code 왼쪽 아래 `WSL: Ubuntu-test`, Docker Desktop `Engine running` 화면 | `java -version` 21.0.13, `spring --version` 4.0.7, `docker version` | (레포 전이라 없음) |
