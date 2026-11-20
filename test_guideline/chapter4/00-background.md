# 00. 배경: 3장과 무엇이 다른가 (도커로 배포)

> 코드 없이 읽기만 하는 문서. 4장 전체를 한눈에 보는 요약.

## 1. 3장까지
```
터미널 1: java -jar product-service...jar         → localhost:7002
터미널 2: java -jar recommendation-service...jar  → localhost:7003
터미널 3: java -jar review-service...jar          → localhost:7004
터미널 4: java -jar product-composite...jar       → localhost:7001
```
- 서비스마다 **포트가 달라야** 한다(같은 PC에서 돌리니까).
- 띄우고 끄는 걸 **하나씩 손으로** 한다. 내 PC의 Java 버전, 설정에 따라 동작이 달라질 수 있다.

## 2. 4장이 끝나면
```
docker compose up -d   (명령 하나)
 ┌──────────────── 도커 네트워크 ────────────────┐
 │ product:8080   recommendation:8080   review:8080 │
 │            ▲            ▲               ▲        │
 │            └──── product-composite:8080 ─┘        │
 └──────────────────────┬──────────────────────────┘
                        │ ports "8080:8080"
                 내 PC localhost:8080
```
- 서비스마다 **도커 이미지**를 만들고, **도커 컴포즈**로 4개를 한 번에 띄우고 끈다.
- 컨테이너는 각자 따로라서 **모두 8080 포트**를 써도 충돌하지 않는다.
- 컨테이너끼리는 **서비스 이름이 곧 주소**다(`http://product:8080`). composite만 밖으로 포트를 연다.

## 3. 3장과 달라지는 점
| 항목 | 3장 | 4장 |
|---|---|---|
| 실행 방법 | `java -jar` 4번 | `docker compose up -d` 한 번 |
| 서비스 포트 | 7001~7004 | 컨테이너 안에서 모두 **8080** |
| composite가 부르는 주소 | `localhost:7002` 등 | `product:8080` 등 (서비스 이름) |
| 설정 구분 | 하나 | yml에 **`docker` 프로필** 추가 (`---` 아래, 컴포즈가 `SPRING_PROFILES_ACTIVE=docker`로 켬) |
| 새 파일 | - | 서비스마다 `Dockerfile`, 루트에 `docker-compose.yml` |
| 밖에서 부르는 주소 | `localhost:7001/product-composite/1` | `localhost:8080/product-composite/1` |
| test-em-all.bash | 이미 띄운 서비스를 테스트 | `start`/`stop` 인자로 **컴포즈 구동~종료까지 자동** |

- 로컬(도커 없이) 실행용 설정은 그대로 남아 있다. **기본 프로필 = 로컬(7001~7004), `docker` 프로필 = 컨테이너(8080)**.

## 4. 각 문서에서 생기는 것
| 문서 | 내용 |
|---|---|
| 01 | 핵심 서비스 3개: yml에 `docker` 프로필, `Dockerfile` (레이어로 나눠 이미지 빌드를 빠르게) |
| 02 | composite: `docker` 프로필(핵심 서비스 주소를 서비스 이름으로), `Dockerfile` |
| 03 | `docker-compose.yml`로 4개 한 번에, `test-em-all.bash start stop` 자동화, 빌드·실행 |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 이미지 | 실행에 필요한 것(JRE + jar)을 묶은 틀. `docker build`로 만든다 |
| 컨테이너 | 이미지를 실행한 것. 같은 이미지로 여러 개 띄울 수 있다(9장 `--scale`) |
| 프로필 | 상황별 설정 묶음. `docker` 프로필은 컨테이너에서만 켜진다 |
| 포트 매핑 `"8080:8080"` | `내 PC 포트:컨테이너 포트`. 밖에서 접근할 컨테이너만 연다 |
| `mem_limit: 512m` | 컨테이너가 쓸 수 있는 메모리 상한. 자바가 이 값을 보고 힙 크기를 정한다 |
