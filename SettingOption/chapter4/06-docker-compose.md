# 06. 도커 컴포즈로 4개 서비스 관리

> 표기: `★ 추가` = 새로 넣는 줄, 표시 없는 줄 = 이미 있는 줄(위치 확인용)
> 모든 명령은 `~/msa`에서 실행한다.

## 무엇을 만드나
```
~/msa/
├── docker-compose.yml                      ← 1개, 루트에 (4개 서비스를 한 번에 정의)
└── microservices/
    ├── product-service/                    (Dockerfile ✓, docker 프로파일 ✓ — 03에서 함)
    ├── recommendation-service/             Dockerfile + yml docker 프로파일
    ├── review-service/                     Dockerfile + yml docker 프로파일
    └── product-composite-service/          Dockerfile + yml docker 프로파일 (app: 블록 포함)
```
- `docker-compose.yml`은 **루트에 하나**다. 서비스마다 만드는 것이 아니다.
- 서비스마다 만드는 것은 **Dockerfile**과 **yml의 docker 프로파일**이다.

---

## 1. 나머지 서비스 Dockerfile (새 파일 3개)

03에서 만든 product-service의 Dockerfile을 그대로 복사한다.
```bash
cp microservices/product-service/Dockerfile microservices/recommendation-service/
cp microservices/product-service/Dockerfile microservices/review-service/
cp microservices/product-service/Dockerfile microservices/product-composite-service/
```
- 4개 서비스 모두 Dockerfile 내용이 같다. 각 폴더의 `build/libs/*.jar`를 넣기 때문에 서비스 이름을 바꿀 곳이 없다.
- **교재 소스의 Dockerfile을 복사하지 않는다.** 교재 것은 `24_36`, `layertools` 버전이다. 03에서 고친 product-service 것을 기준으로 한다.

---

## 2. recommendation, review의 docker 프로파일 (기존 파일)

> 기본 형태가 궁금하면 참고: `code microservices/product-service/src/main/resources/application.yml` (맨 끝 4줄이 03에서 넣은 docker 프로파일)

```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
**맨 끝에** 4줄 추가
```bash
code microservices/review-service/src/main/resources/application.yml
```
**맨 끝에** 4줄 추가
```yaml
logging:
  level:
    root: INFO
    se.magnus: DEBUG

---                                              # ★ 추가
spring.config.activate.on-profile: docker        # ★ 추가
                                                 # ★ 추가
server.port: 8080                                # ★ 추가
```
컨테이너는 각자 떨어진 환경이라 서비스마다 포트를 다르게 줄 필요가 없다. 셋 다 8080이다.

---

## 3. product-composite-service의 docker 프로파일 (기존 파일)

> 기본 형태가 궁금하면 참고: `code microservices/product-service/src/main/resources/application.yml` (docker 프로파일 형식)
> `app:` 구조는 같은 composite 파일 위쪽의 로컬용 `app:` 블록과 같고 값만 다르다.

```bash
code microservices/product-composite-service/src/main/resources/application.yml
```
**맨 끝에** 추가
```yaml
app:
  product-service:
    host: localhost
    port: 7002
  recommendation-service:
    host: localhost
    port: 7003
  review-service:
    host: localhost
    port: 7004

logging:
  level:
    root: INFO
    se.magnus: DEBUG

---                                              # ★ 추가
spring.config.activate.on-profile: docker        # ★ 추가
server.port: 8080                                # ★ 추가
                                                 # ★ 추가
app:                                             # ★ 추가
  product-service:                               # ★ 추가
    host: product                                # ★ 추가
    port: 8080                                   # ★ 추가
  recommendation-service:                        # ★ 추가
    host: recommendation                         # ★ 추가
    port: 8080                                   # ★ 추가
  review-service:                                # ★ 추가
    host: review                                 # ★ 추가
    port: 8080                                   # ★ 추가
```
- **위쪽의 로컬용 `app:` 블록은 지우지 않는다.** 로컬 실행(`java -jar`) 때는 위쪽 값(localhost:7002~7004), docker 프로파일이 켜지면 아래쪽 값이 위쪽 값을 덮어쓴다.
- 로컬 실행과 컨테이너 실행에서 핵심 서비스 주소가 이렇게 달라진다.
  ```
  로컬 실행:   localhost:7002 / localhost:7003      / localhost:7004
  컨테이너:    product:8080   / recommendation:8080 / review:8080
  ```
- `product`, `recommendation`, `review`는 **docker-compose.yml에 적는 서비스 이름**이다. 컴포즈가 띄운 컨테이너끼리는 이 이름으로 서로를 찾는다. 그래서 여기 `host:` 값과 compose 파일의 서비스 이름이 **똑같아야** 한다.

---

## 4. docker-compose.yml (새 파일, 루트)
위치는 `~/msa/docker-compose.yml`(레포 루트, `settings.gradle`과 같은 위치)이다. `microservices` 안이 아니다.
```bash
code docker-compose.yml
```
아래 내용 전체 입력

**교재 내용 그대로**다. Java·Boot 버전과 상관없는 설정이고, 폴더 이름과 서비스 이름이 교재와 같아서 바꿀 곳이 없다.
```yaml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

### 서비스 이름 = 호스트 이름
`services:` 아래의 `product`, `recommendation`, `review`가 3번 composite yml의 `host:` 값이다. 도커 컴포즈는 서비스들을 **같은 네트워크**에 띄우고, **서비스 이름으로 서로를 찾게** 해 준다. composite 컨테이너 안에서 `http://product:8080/...`으로 요청하면 product 컨테이너로 간다. 두 파일의 이름이 하나라도 다르면 composite가 그 서비스를 찾지 못한다.

| 항목 | 뜻 | 05의 같은 것 |
|---|---|---|
| `build: microservices/product-service` | 이 폴더의 Dockerfile로 이미지 빌드 | `cd` 후 `docker build .` |
| `mem_limit: 512m` | 컨테이너 메모리 512MB 제한 (자바 힙은 그 1/4인 128MB) | `-m` (02 실험) |
| `environment: - SPRING_PROFILES_ACTIVE=docker` | docker 프로파일 켜기 | `-e "SPRING_PROFILES_ACTIVE=docker"` |
| `ports: - "8080:8080"` | **composite에만** 있음. 내 PC 8080 → composite 8080 | `-p8080:8080` |

composite에만 `ports`가 있는 이유: 외부(내 PC)에서 호출하는 건 composite 하나뿐이다. 나머지 셋은 compose 네트워크 안에서 composite만 호출하므로 내 PC 쪽으로 포트를 열 필요가 없다. 셋 다 컨테이너 안에서 8080을 써도 내 PC 포트와 연결하지 않으니 서로 겹치지 않는다.

---

## 5. 빌드
```bash
cd ~/msa                  # 교재: cd $BOOK_HOME/Chapter04
./gradlew build
docker-compose build
```
- 4개 서비스 모두 바뀌었으니 `./gradlew build`로 **전체**를 빌드한다(04의 jar 확인 이유와 같다).
- `docker-compose`(하이픈)와 `docker compose`(띄어쓰기)는 이 PC에서 **같은 프로그램**이다(둘 다 v2.40.3). 교재가 1번은 하이픈, 4번부터는 띄어쓰기를 쓰는데 어느 쪽이든 된다.
- compose 명령은 **현재 폴더의 docker-compose.yml**을 기준으로 동작한다. 그래서 `~/msa`에서 실행한다.

## 6. 이미지 확인 ⚠️ 교재와 다름
```bash
docker images | grep msa          # ★ 변경 (교재: grep chapter04)
```
도커 컴포즈는 이미지 이름을 **`<폴더 이름>-<서비스 이름>`**으로 붙인다. 교재는 `Chapter04` 폴더에서 실행해서 `chapter04-product`, 우리는 `msa` 폴더라서 `msa-product`가 된다.
```
msa-product-composite   latest   ...
msa-review              latest   ...
msa-recommendation      latest   ...
msa-product             latest   ...
```
교재대로 `grep chapter04`를 치면 **아무것도 안 나와서** 빌드가 실패한 걸로 착각하기 쉽다. 05에서 직접 만든 `product-service` 이미지는 이름이 달라서 따로 남아 있다.

## 7. 구동과 로그
```bash
docker ps                          # 05의 my-prd-srv 등이 남아 있지 않은지 (8080 충돌 방지)
docker compose up -d
docker compose logs -f
```
- `up -d`: 4개 컨테이너를 **분리 모드**로 한 번에 띄운다(05의 `docker run -d`).
- `logs -f`: 4개 컨테이너의 로그를 한 화면에 섞어서 보여 준다. 줄 앞의 `product-1 |`, `review-1 |`로 어느 컨테이너 로그인지 구분한다. **터미널 하나를 점유**하므로, 4개 모두 `Started ...Application`이 나오면 다른 터미널로 간다. `Ctrl+C`는 로그 보기만 멈춘다(컨테이너는 계속 떠 있다).

## 8. 테스트
**다른 터미널**에서 (교재 값 그대로)
```bash
curl localhost:8080/product-composite/123 -s | jq .
```
3장 로컬 실행과 비교해서 `serviceAddresses`를 본다.
| | 3장 로컬 (`localhost:7001`) | 컴포즈 (`localhost:8080`) |
|---|---|---|
| 호출 포트 | 7001 | **8080** (composite에만 연 포트) |
| `serviceAddresses`의 호스트 이름 | 4개 모두 `301B-03` (내 PC) | 4개가 **서로 다른 컨테이너 ID** |
| IP | 4개 모두 `127.0.1.1` | `172.18.0.x`로 **서로 다름** |
| 포트 | 7001 / 7002 / 7003 / 7004 | 4개 모두 **8080** |
- `cmp`, `pro`, `rev`, `rec`가 서로 다른 컨테이너 ID·IP로 나오면 **4개 서비스가 각자 다른 컨테이너에서 응답**한 것이다(교재 그림 4.16).
- IP가 05의 `172.17.0.x`가 아니라 `172.18.0.x`인 건, 도커 컴포즈가 이 프로젝트 **전용 네트워크**를 따로 만들기 때문이다.

### curl이 아무 응답 없을 때
- `localhost: 8080`처럼 **콜론 뒤에 공백**이 들어가면 curl이 주소를 두 개(`localhost:`, `8080/...`)로 나눠 받아 둘 다 실패한다. 실제로 이렇게 쳐서 응답이 없었다.
- `-s`(silent)는 **오류 메시지까지 숨긴다.** 그래서 실패해도 아무것도 안 보이고, `jq`도 받은 게 없어 조용하다.
- 확인할 때는 `-s | jq .`를 빼고 `-i`로 친다. 오류가 그대로 보인다.
  ```bash
  curl -i localhost:8080/product-composite/123
  ```
  `Connection refused` → 서비스가 안 떴거나 포트가 틀림 (`docker compose ps`로 확인)
- 명령이 멈춰 있을 때는 `Ctrl+C`(종료)를 누른다. `Ctrl+Z`는 **일시 정지**라 `[1]+ Stopped`로 뒤에 남는다 → `kill %1`로 정리, `jobs`로 확인.

## 9. 종료
```bash
cd ~/msa
docker compose down
```
4개 컨테이너와 전용 네트워크를 한 번에 정리한다. compose 명령이라 docker-compose.yml이 있는 `~/msa`에서 실행한다. `logs -f`를 보던 터미널은 컨테이너가 내려가면 자동으로 끝난다.

다음: [07-test-script.md](07-test-script.md)
