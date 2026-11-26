# 2장: 실습 폴더 만들기, 기본 골격 (Ubuntu 24.04)

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 3장 앞부분
> 범위: **실습 폴더 생성 → VS Code로 열기 → 기본 골격 마이크로서비스 4개 생성 → 전체 빌드**
> 이전: [../chapter1/README.md](../chapter1/README.md) / 다음: [../chapter3/README.md](../chapter3/README.md) (API 구현, 오류 처리, 복합 서비스, 테스트)

> **시험·새로 시작할 때는 여기서부터.** 1장의 도구가 설치되어 있어야 한다.

## 진행 순서
| 순서 | 문서 | 내용 |
|---|---|---|
| 1 | [02-project-folder.md](02-project-folder.md) | 실습 폴더 생성, VS Code로 열기, `create-projects.bash`로 서비스 4개, 루트 `settings.gradle`·래퍼 정리, 전체 빌드, (선택) GitHub |
| - | [troubleshooting.md](troubleshooting.md) | 시행착오와 주의사항 |

## 끝나면
`~/msa`(또는 새로 만든 폴더)에 서비스 4개의 뼈대가 생기고 `./gradlew build`가 `BUILD SUCCESSFUL`로 끝난다.

## 실습 레포

| 항목 | 값 |
|---|---|
| Spring Boot | 4.0.9 |
| Gradle | 9.8.2 (Initializr가 만든 래퍼) |
| 교재 코드 위치 | `~/spring-microservice` (`$BOOK_HOME`) |
| 실습 레포 | `~/msa` → https://github.com/klaod-tech/msa_practice |

> 교재는 Spring Boot 3 / Java 17 기준(교재 소스는 Java 24 / Boot 3.5.0)이지만, 이 실습은 **Java 21 / Spring Boot 4.0.9**로 진행한다.
