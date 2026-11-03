# 4장: 도커를 사용한 배포

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 4장
> 실습 환경: **Java 21 / Spring Boot 4.0.9**, Docker 29.0.1 (Docker Desktop + WSL2 Ubuntu 24.04)
> 이전: [../chapter3/README.md](../chapter3/README.md) / 상태: **완료 (2026-11-03)** — 서비스 4개 도커 이미지화, 도커 컴포즈 구동, `./test-em-all.bash start stop` 통과

## 문서 읽는 법
- 명령 블록은 위에서부터 그대로 입력하면 된다.
- 기존 파일 수정은 앞뒤 줄과 함께 `★ 추가` / `★ 변경 (교재: ...)` / `★ 삭제`로 표시한다.

## 진행 순서

| 순서 | 문서 | 내용 | 교재 |
|---|---|---|---|
| 1 | [01-docker-setup.md](01-docker-setup.md) | 도커 기본 세팅: Ubuntu(WSL)와 Docker Desktop 연동, 첫 도커 명령, Docker Desktop 화면 | 93~94쪽 |
| 2 | [02-java-in-docker.md](02-java-in-docker.md) | 컨테이너 안 자바의 CPU·메모리 제한 인식, 메모리 부족 테스트 | 95쪽~ |
| 3 | [03-dockerfile.md](03-dockerfile.md) | docker 프로파일, Dockerfile 작성(Boot 4 대응) | 99쪽~ |
| 4 | [04-jar-build.md](04-jar-build.md) | jar 빌드, jar 확인(ls), (선택) jar 안 내용·프로파일 확인 | |
| 5 | [05-docker-image-run.md](05-docker-image-run.md) | 도커 이미지 빌드, 서비스 구동, 분리 모드, 로그 보기, 정리 | |
| 6 | [06-docker-compose.md](06-docker-compose.md) | 나머지 서비스 Dockerfile·docker 프로파일, docker-compose.yml, 빌드·구동·테스트·종료 | |
| 7 | [07-test-script.md](07-test-script.md) | test-em-all.bash 자동화 (start/stop, 서비스 대기) | |
| - | [troubleshooting.md](troubleshooting.md) | 오류와 해결 | |

## 교재와 다르게 하는 것 (진행하면서 추가)

| 항목 | 교재 | 이 실습 | 문서 |
|---|---|---|---|
| 자바 이미지 | `eclipse-temurin:24` | `eclipse-temurin:21` (서비스가 Java 21) | 02 |
| Dockerfile 베이스 이미지 | `eclipse-temurin:24_36-jre-noble` | `eclipse-temurin:21-jre-noble` | 03 |
| Dockerfile의 레이어 추출 | `-Djarmode=layertools ... extract` | `-Djarmode=tools ... extract --layers --launcher --destination layers` + COPY 경로에 `layers/` | 03 |
| CPU 수 결과 | 8 | 12 (PC마다 다름) | 02 |
| `ubuntu` 이미지 버전 | 교재 시점 | `latest` = Ubuntu 26.04.1 LTS | 01 |
| compose 이미지 이름 확인 | `docker images \| grep chapter04` | `docker images \| grep msa` (이름 = `<폴더>-<서비스>`) | 06 |
| compose 실행 위치 | `cd $BOOK_HOME/Chapter04` | `cd ~/msa` | 06 |
