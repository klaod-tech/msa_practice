# 실습 환경 설정 (22장 → 3장 기본 골격)

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손)
> 범위: **22장 WSL2 개발 환경 설정 → 3장 기본 골격 마이크로서비스 4개 생성 → GitHub 업로드**
> 이후 진행(API 구현, 오류 처리, 복합 서비스, 테스트): [../chapter3/README.md](../chapter3/README.md)
> 명령은 `~/.bash_history`와 실제 작업 기록 중 **성공한 경로만** 추렸다. 시행착오는 [troubleshooting.md](troubleshooting.md)에 적었다.

## 진행 순서

| 순서 | 문서 | 내용 | 교재 |
|---|---|---|---|
| 1 | [01-windows.md](01-windows.md) | WSL2 + Ubuntu, Docker Desktop, VS Code | 22장 |
| 2 | [02-apt-helm.md](02-apt-helm.md) | apt 기본 도구(jq, zip, unzip, siege), Helm | 22장 |
| 3 | [03-sdkman-java-spring.md](03-sdkman-java-spring.md) | SDKMAN → Java 21, Spring Boot CLI | 22장 |
| 4 | [04-kubernetes-tools.md](04-kubernetes-tools.md) | kubectl, minikube, istioctl | 22장 |
| 5 | [05-verify-and-book-source.md](05-verify-and-book-source.md) | 설치 버전 확인, 교재 소스 내려받기(`$BOOK_HOME`) | 22장 |
| 6 | [06-create-projects.md](06-create-projects.md) | 실습 폴더, `create-projects.bash`로 서비스 4개 생성 | 3장 |
| 7 | [07-multi-project.md](07-multi-project.md) | 루트 `settings.gradle`, Gradle 래퍼 정리 | 3장 |
| 8 | [08-build.md](08-build.md) | 전체 빌드, 구조 확인 | 3장 |
| 9 | [09-github.md](09-github.md) | git 초기화, 커밋, GitHub 푸시 | - |
| - | [troubleshooting.md](troubleshooting.md) | 시행착오와 주의사항 | |

## 설치 결과 요약

| 도구 | 버전 | 설치 방법 | 문서 |
|---|---|---|---|
| Ubuntu (WSL2) | 24.04.5 LTS | Windows에서 WSL로 설치 | 01 |
| Docker | 29.0.1 (Compose v2.40.3) | Windows용 Docker Desktop의 WSL 연동 | 01 |
| VS Code | - | Windows에 설치한 뒤 WSL 원격 연결(`code .`) | 01 |
| git | 2.43.0 | Ubuntu 기본 포함 | - |
| jq / zip / unzip / siege | jq-1.7 / - / - / 4.0.7 | apt | 02 |
| Helm | v3.22.1 | 공식 설치 스크립트 | 02 |
| Java | 21.0.13 (Temurin) | SDKMAN | 03 |
| Spring Boot CLI | 4.0.7 | SDKMAN | 03 |
| kubectl | v1.32.0 | 바이너리 다운로드 | 04 |
| minikube | v1.35.0 | 바이너리 다운로드 | 04 |
| istioctl | 1.24.2 | Istio 다운로드 스크립트 | 04 |

## 실습 레포

| 항목 | 값 |
|---|---|
| Spring Boot | 4.0.9 |
| Gradle | 9.8.2 (Initializr가 만든 래퍼) |
| 교재 코드 위치 | `~/spring-microservice` (`$BOOK_HOME`) |
| 실습 레포 | `~/msa` → https://github.com/klaod-tech/msa_practice |

> 교재는 Spring Boot 3 / Java 17 기준(교재 소스는 Java 24 / Boot 3.5.0)이지만, 이 실습은 **Java 21 / Spring Boot 4.0.9**로 진행한다.
