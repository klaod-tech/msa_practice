# 00. 배경: 설정을 왜 한곳에 모으나, 11장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서. 교재 12장 앞부분 요약.

## 1. 11장까지
```
 product/src/main/resources/application.yml          ── Eureka 주소·계정(u/p), RabbitMQ 계정, 포트, DB ...
 recommendation/src/main/resources/application.yml   ── Eureka 주소·계정(u/p), RabbitMQ 계정, 포트, DB ...
 review/src/main/resources/application.yml           ── Eureka 주소·계정(u/p), RabbitMQ 계정, MySQL 비밀번호 ...
 product-composite / gateway / auth-server / eureka-server ... (7개)
```
- **같은 설정이 7군데에 복사**돼 있다. Eureka 비밀번호 하나를 바꾸려면 7개 파일을 고치고 7개를 다시 빌드해야 한다.
- 설정 파일이 jar 안에 들어가서 **설정만 바꿔도 다시 빌드**해야 한다.
- 비밀번호가 **평문**으로 소스(GitHub)에 그대로 있다.

## 2. 12장이 끝나면
```
                     ~/msa/config-repo/  (설정 파일을 모아 둔 폴더)
                     application.yml (공통) + product.yml + review.yml + gateway.yml ...
                                │ 읽기 (native 프로필 = 로컬 폴더)
                                ▼
                     config-server :8888  ── ENCRYPT_KEY로 '{cipher}...' 복호화
                     (HTTP 기본 인증 dev-usr / dev-pwd)
                                ▲
      ① 시작할 때 "나는 product, 프로필 docker"   │  ② application.yml + product.yml (평문으로 풀어서)
                                │
   product / recommendation / review / product-composite / gateway / auth-server / eureka-server
   (jar 안에는 bootstrap.yml만: 내 이름 + 설정 서버 주소 + 계정)

 밖에서 보기: https://localhost:8443/config/** ─▶ gateway ─▶ config-server
```
- 설정은 **config-repo 한 곳**에 있다. 공통 설정은 `application.yml` 하나에만 적는다.
- 서비스는 **시작할 때** 설정 서버에서 설정을 받아 온다. 그래서 설정 서버가 **가장 먼저** 떠 있어야 한다(늦으면 retry로 기다린다).
- 비밀번호는 `'{cipher}...'` 암호문으로 적고, 설정 서버가 풀어서 건넨다.
- 설정 서버는 Eureka에 등록하지 않는다. Eureka 주소도 설정 서버에서 받아 가기 때문이다(닭과 달걀 문제를 피함).

## 3. 시작 순서
| 순서 | 무슨 일 |
|---|---|
| 1 | 서비스가 `bootstrap.yml`을 읽는다(`spring-cloud-starter-bootstrap` 덕분에 application 설정보다 **먼저**) |
| 2 | `http://config-server:8888/product/docker`를 `dev-usr:dev-pwd`로 부른다 |
| 3 | 설정 서버가 `application.yml` + `product.yml`에서 기본/docker 부분을 골라 암호문을 풀어 돌려준다 |
| 4 | 서비스는 그 설정으로 나머지(DB 연결, Eureka 등록 등)를 시작한다 |
| - | 설정 서버가 아직 없으면 3초 → 최대 10초 간격으로 20번까지 다시 시도, 그래도 안 되면 시작을 포기(`failFast`) |

## 4. 11장과 달라지는 점
| 항목 | 11장 | 12장 |
|---|---|---|
| 새 프로젝트 | - | `spring-cloud/config-server` (포트 8888, 밖에 안 열림) |
| 설정 위치 | 서비스마다 `src/main/resources/application.yml` | 루트 `config-repo/*.yml` 8개. 서비스에는 **`bootstrap.yml`만** |
| 비밀번호 | 평문 (`p`, `guest`, `pwd`, `password`, `secret-writer`) | **암호문** `'{cipher}...'` |
| 새 파일 (루트) | - | `.env`: 암호화 키, 설정 서버 계정 (compose가 자동으로 읽음) |
| 의존성 (서비스 7개) | - | `spring-cloud-starter-config`, `config-client`, `starter-bootstrap`, `starter-aspectj`, `spring-retry` |
| 테스트 | `eureka.client.enabled=false` | + **`spring.cloud.config.enabled=false`**, 테스트용 `src/test/resources/application.yml` |
| 게이트웨이 | 라우트 9개 | + `config-server` 라우트(`/config/**`), SecurityConfig에서 `/config/**` 허용 |
| compose | - | 서비스마다 `CONFIG_SERVER_USR/PWD`, `config-server` 서비스, `config-repo` 폴더를 볼륨으로 |
| test-em-all.bash | - | 설정 조회 200, 암호화 → 복호화 왕복 확인 |

## 5. 문서별로 생기는 것
| 문서 | 이전 상태 → 이후 상태 |
|---|---|
| 01 | 설정 서버 없음 → `config-server` 프로젝트(빌드, 테스트 1개) |
| 02 | 설정이 서비스 안에 → `config-repo/` 8개 파일 + `.env` (아직 서비스는 예전 설정을 씀) |
| 03 | 서비스가 자기 application.yml 사용 → **bootstrap.yml만 남고 설정 서버에서 받음**, 테스트는 설정 서버 없이 |
| 04 | compose에 설정 서버 없음 → 설정 서버 포함 전체 구동, `End, all tests OK` |

## 6. 용어
| 용어 | 뜻 |
|---|---|
| 설정 서버 (Spring Cloud Config Server) | 설정 파일을 HTTP로 나눠 주는 서버. `/{이름}/{프로필}`로 요청 |
| 설정 저장소 (config-repo) | 설정 파일이 실제로 있는 곳. 교재는 로컬 폴더(native), 실무는 주로 Git 저장소 |
| native 프로필 | 설정 서버가 Git 대신 로컬 폴더에서 읽게 하는 프로필 |
| bootstrap.yml | 설정 서버에서 설정을 받기 **전에** 필요한 최소 설정(이름, 서버 주소, 계정) |
| `'{cipher}...'` | 암호문 표시. 설정 서버가 `ENCRYPT_KEY`로 풀어서 건넨다 |
| `/encrypt`, `/decrypt` | 설정 서버가 제공하는 암호화/복호화 엔드포인트 (POST) |
| failFast / retry | 설정을 못 받으면 시작하지 않기 / 그 전에 몇 번 다시 시도하기 |
| `.env` | docker compose가 같은 폴더에서 자동으로 읽는 환경변수 파일 |
