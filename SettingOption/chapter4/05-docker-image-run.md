# 05. 도커 이미지 빌드와 서비스 구동

> [04](04-jar-build.md)에서 jar를 새로 빌드하고 확인한 다음 진행한다.

---

## 1. 도커 이미지 빌드
```bash
cd ~/msa/microservices/product-service
docker build -t product-service .
```
| 부분 | 뜻 |
|---|---|
| `cd microservices/product-service` | Dockerfile이 있는 폴더로 이동. jar 빌드(04)는 `~/msa`에서, 이미지 빌드는 여기서 한다 |
| `-t product-service` | 만들 이미지의 이름(태그). 버전을 안 붙이면 `product-service:latest` |
| `.` | **빌드 컨텍스트** = 현재 폴더. 이 폴더의 Dockerfile과 `build/libs/*.jar`를 사용한다. `~/msa`에서 실행하면 Dockerfile을 찾지 못한다 |

- 처음에는 베이스 이미지(`eclipse-temurin:21-jre-noble`)를 내려받느라 오래 걸린다.
- 로그에 Dockerfile 각 줄이 `[builder 1/4]`, `[stage-1 2/6]`처럼 단계별로 찍힌다. **실패하면 어느 단계에서 멈췄는지**가 원인을 찾는 핵심이다.
- 이 폴더에서 다시 `./gradlew`를 쓰려면 `cd ~/msa`로 돌아가야 한다.

## 2. 이미지 확인
```bash
docker images | grep product-service
```
`product-service   latest   <이미지 ID>   ...   <크기>` 한 줄이 나오면 된다. 크기는 교재(289MB)와 다를 수 있다(베이스 이미지·jar 크기가 다름).

---

## 3. 서비스 구동 (터미널 하나를 점유하는 방식)
**터미널 1**
```bash
docker run --rm -p8080:8080 -e "SPRING_PROFILES_ACTIVE=docker" product-service
```
| 옵션 | 뜻 |
|---|---|
| `--rm` | 컨테이너가 멈추면 자동 삭제 |
| `-p8080:8080` | **내 PC의 8080 → 컨테이너의 8080**으로 연결. 그래서 `localhost:8080`으로 호출할 수 있다 |
| `-e "SPRING_PROFILES_ACTIVE=docker"` | 환경 변수로 스프링 프로파일을 지정 → yml의 docker 부분(8080) 적용 |

로그에서 이 두 줄을 확인한다.
```
The following 1 profile is active: "docker"     ← -e로 넘긴 프로파일이 켜졌는지
Netty started on port 8080                      ← jar 안 yml의 docker 설정(8080)이 적용됐는지
```
이 명령은 컨테이너가 실행되는 동안 로그를 계속 출력하면서 **터미널 하나를 점유한다.** 프롬프트(`leo@301B-03:...$`)가 돌아오지 않아서 이 터미널에는 다른 명령을 입력할 수 없다. 그래서 호출은 **다른 터미널(터미널 2)**에서 한다.

**터미널 2**
```bash
curl localhost:8080/product/3
```
교재와 같은 모양의 응답(필드 순서만 알파벳순):
```json
{"name":"name-3","productId":3,"serviceAddress":"<컨테이너ID>/172.17.0.2:8080","weight":123}
```
- 호스트 이름이 내 PC(`301B-03`)가 아니라 **컨테이너 ID**다 → 컨테이너 안에서 응답했다는 증거
- `172.17.0.x`는 Docker 내부 네트워크에서 컨테이너가 받은 IP, 포트는 **8080**(docker 프로파일)

## 4. 호스트 이름 = 컨테이너 ID 확인
**터미널 2** (curl을 친 곳에서 이어서)
```bash
docker ps
```
| 열 | 볼 것 |
|---|---|
| `CONTAINER ID` | curl 응답의 `serviceAddress` 앞부분과 **같은 값** → 여러 컨테이너 중 어느 것이 응답했는지 알 수 있다 |
| `PORTS` | `0.0.0.0:8080->8080/tcp` = 내 PC 8080 → 컨테이너 8080 (`-p8080:8080`) |
| `NAMES` | Docker가 자동으로 붙인 이름이라 교재(`interesting_mahavira`)와 다르다. 정상 |

끝낼 때: **터미널 1**에서 `Ctrl+C` → `--rm`이라 컨테이너가 자동으로 정리된다.

---

## 5. 분리(detached) 모드로 실행
**앞 컨테이너를 먼저 멈춘다.** 두 컨테이너 모두 내 PC의 8080을 쓰려고 해서, 앞 컨테이너가 떠 있으면 `Bind for 0.0.0.0:8080 failed: port is already allocated` 오류가 난다.
```bash
docker ps        # 비어 있으면 OK
docker run -d -p8080:8080 -e "SPRING_PROFILES_ACTIVE=docker" --name my-prd-srv product-service
```
출력은 **긴 문자열 한 줄**뿐이고 프롬프트가 바로 돌아온다. 정상이다.
```
8c01ccb40d21896eafc0d3fa07fea9fbe57395d7555a557e8d6d929b57cef46a
```
이 문자열은 컨테이너의 **전체 ID**다. `docker ps`에는 앞 12자리(`8c01ccb40d21`)로 줄여서 나온다.

| | 3번 (터미널 하나를 점유하는 방식) | 5번 (분리 모드) |
|---|---|---|
| 명령 | `docker run --rm -p8080:8080 ...` | `docker run -d -p8080:8080 ... --name my-prd-srv ...` |
| 터미널 | 컨테이너가 끝날 때까지 로그를 출력하며 터미널을 점유 → 프롬프트가 안 돌아와 다른 명령 불가 | 컨테이너 ID만 출력하고 프롬프트가 바로 돌아옴 → 같은 터미널에서 계속 명령 가능 |
| 로그 보기 | 그 터미널에 바로 보임 | `docker logs`로 따로 봄 |
| 이름 | Docker가 자동으로 붙임 | 내가 정한 `my-prd-srv` |
| 끝낼 때 | `Ctrl+C` → `--rm`으로 자동 삭제 | **직접** 멈추고 지움 (`docker rm -f`) |

- `-d` (detached): 컨테이너를 뒤에서 실행하고 터미널을 돌려준다. 3장의 `java -jar ... &`와 비슷하다.
- `--name`: 이후 명령에서 컨테이너 ID 대신 이름으로 가리킨다.
- `--rm`을 뺀 이유: 나중에 직접 멈추고 지울 것이기 때문. 그래서 **정리(7번)까지가 한 세트**다.

## 6. 로그 보기
**터미널 1**
```bash
docker logs my-prd-srv -f
```
`-f`(follow)는 로그를 **계속 기다리면서** 보여 준다 → 이 터미널도 로그 출력에 **점유되어** 프롬프트가 돌아오지 않는다. 그래서 curl은 터미널 2에서 친다.

**터미널 2**
```bash
curl localhost:8080/product/3
```
curl을 칠 때마다 **터미널 1에 새 로그 줄이 실시간으로** 찍힌다(예: `/product return the found product for productId=3`). 3장에서 yml에 `se.magnus: DEBUG`를 넣어 둬서 DEBUG 로그가 보인다.

| 옵션 | 뜻 |
|---|---|
| `-f` | 새 로그를 계속 기다리며 출력 |
| `--tail 0` | 이전 로그는 빼고 **지금부터** 나오는 로그만 (`docker logs my-prd-srv -f --tail 0`) |
| `--since 5m` | 최근 5분 이내 로그만 |

터미널 1에서 `Ctrl+C` → **로그 보기만** 멈춘다. 컨테이너는 분리 모드라 계속 떠 있다(3번에서 `docker run`에 대고 `Ctrl+C`한 것과 다르다).

## 7. 정리
```bash
docker rm -f my-prd-srv
docker ps          # 비어 있으면 정리 끝
```
`-f`: 실행 중이어도 먼저 멈춘 다음 지운다. `--rm` 없이 띄웠으니 직접 지워야 한다.

다음: [06-docker-compose.md](06-docker-compose.md)
