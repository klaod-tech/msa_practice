# 4장 오류와 해결 모음

## Docker 연동
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| `docker: command not found` (Ubuntu 터미널) | WSL integration에서 Ubuntu가 꺼져 있음 | Docker Desktop → Settings → Resources → WSL integration → Ubuntu-24.04 켜기 → Apply & restart | 01 |
| `Cannot connect to the Docker daemon at unix:///var/run/docker.sock` | Docker Desktop이 꺼져 있음 | Windows에서 Docker Desktop 실행, `Engine running` 확인 후 다시 시도 | 01 |
| `permission denied ... docker.sock` | 사용자가 docker 그룹에 없음 | `groups`에 `docker`가 있는지 확인. 없으면 Docker Desktop 재시작 또는 `sudo usermod -aG docker $USER` 후 터미널 재시작 | 01 |
| 명령은 쳤는데 Windows 쪽에서 실행됨 | PowerShell이나 Docker Desktop의 `>_` 터미널에서 입력 | 프롬프트가 `leo@301B-03:~$`인 Ubuntu 터미널에서 입력 | 01 |

## 컨테이너
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| Docker Desktop Logs 탭에서 입력이 안 됨 | Logs는 읽기 전용 | **Exec** 탭 사용, 또는 `docker exec -it <이름> bash` | 01 |
| `docker rm -f $(docker ps -aq)` → `requires at least 1 argument` | 지울 컨테이너가 없음 | 무시 | 01 |
| 컨테이너에서 나왔는데 `docker ps -a`에 남아 있음 | `--rm` 없이 실행 | `docker rm <이름>` | 01 |

## 이미지 빌드·실행
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| `docker build`에서 `eclipse-temurin:21_36-jre-noble: not found` | 존재하지 않는 태그 (`_36`은 Java 24의 빌드 번호) | `eclipse-temurin:21-jre-noble` | 03 |
| `docker build`의 COPY에서 `"/extracted/dependencies": not found` | `tools extract --destination layers`로 바꾸고 COPY 경로를 안 바꿈 | COPY 경로에 `layers/` 추가 | 03 |
| `unable to prepare context` / `Dockerfile: no such file` | `~/msa`에서 `docker build ... .` 실행 | `cd microservices/product-service` 후 실행 | 05 |
| docker 프로파일을 켰는데 7002로 뜸 | jar가 yml 수정 전에 빌드됨 | `./gradlew :microservices:product-service:build` → jar 안 yml 확인 → 이미지 다시 빌드 | 04 |
| `Bind for 0.0.0.0:8080 failed: port is already allocated` | 앞 컨테이너가 8080을 쓰고 있음 | 앞 컨테이너를 `Ctrl+C` 또는 `docker rm -f <이름>`으로 정리 | 05 |
| `docker run -d` 후 로그가 안 보임 | 분리 모드라 정상 (컨테이너 ID만 출력) | `docker logs <이름>` | 05 |
| `docker logs -f`에서 `Ctrl+C` 후에도 컨테이너가 남아 있음 | `Ctrl+C`는 로그 보기만 멈춤 | `docker rm -f my-prd-srv` | 05 |

## 도커 컴포즈
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| composite가 docker 프로파일로 시작하자마자 실패 / 핵심 서비스를 못 찾음 | yml의 `host:product`처럼 **콜론 뒤 공백 누락** → 문자열 한 덩어리로 해석됨 | `host: product`, `port: 8080` | 06 |
| `docker images \| grep chapter04`가 비어 있음 | 이미지 이름이 `<폴더>-<서비스>` = `msa-...` | `grep msa` | 06 |
| `curl localhost: 8080/...`이 아무 응답 없음 | 콜론 뒤 공백 + `-s`가 오류를 숨김 | 공백 제거, 확인할 땐 `curl -i` | 06 |
| `[1]+ Stopped` | `Ctrl+Z`(일시 정지)를 누름 | `kill %1`, `jobs`로 확인. 종료는 `Ctrl+C` | 06 |
| VS Code yml 주의 `Unknown property 'spring.jackson.mapper...'` / `'app'` / `special characters ... '[]'` | 맵 키·사용자 정의 설정이라 확장이 모름 | 무시 (동작함). 마지막 것은 `"[se.magnus]"`로 없앨 수 있음 | 06 |

## 테스트 스크립트
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| `Test FAILED` 후 컨테이너가 남아 있음 | 실패하면 `stop`까지 가지 않음 (의도된 동작) | `docker compose logs`로 원인 확인 → `docker compose down` | 07 |
| `Wait for: ... Give up` | 약 5분 동안 서비스가 응답하지 않음 | `docker compose logs`로 시작 실패 원인 확인 | 07 |
| `no configuration file provided` | `~/msa`가 아닌 곳에서 실행 | `cd ~/msa` 후 실행 | 07 |
