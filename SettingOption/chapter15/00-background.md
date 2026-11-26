# 00. 배경: 쿠버네티스는 무엇이고, 14장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서.

## 이 장 앞부분: 3부 기준으로 맞추기
1~14장(SettingOption)은 우리 코드로 진행했다. 15~20장(3부)은 쿠버네티스, 헬름, 이스티오 파일이 compose, Dockerfile, build.gradle, config-repo, test-em-all과 맞물려 있다. 그래서 15장에서 이 설정 파일들을 3부 기준으로 한 번 맞춘다.

| 그대로 두는 것 (우리 코드) | 이 장에서 맞추는 것 |
|---|---|
| Lombok 데이터 클래스 7개, 엔티티 3개, Lombok 의존성, `allow-final-fields-as-mutators` | build.gradle의 스타터 이름, Dockerfile, compose, config-repo 값, test-em-all, 서버 코드 정리 |

맞추고 나면 14장처럼 `./gradlew build` → `docker compose build` → `./test-em-all.bash start stop`이 그대로 통과한다(07의 확인 1).

## 1. 14장까지
```
docker compose up -d  ─▶  컨테이너 13개 (gateway, auth-server, config-server, eureka, product ..., mongodb, mysql, rabbitmq, zipkin)
```
- 도커 컴포즈는 **내 PC 한 대**에서 컨테이너를 띄우고 끈다.
- 컨테이너가 죽으면 그대로 죽어 있다. 여러 대의 서버에 나눠 띄우거나, 부하에 맞춰 늘리고 줄이는 일은 직접 해야 한다.

## 2. 쿠버네티스가 해 주는 것
```
 kubectl apply -f nginx-deployment.yaml   ("nginx 파드 1개를 늘 유지해 줘" = 원하는 상태)
        │
        ▼
 쿠버네티스(컨트롤 플레인) ── 지금 상태를 계속 보고 원하는 상태로 맞춘다
        │                    (파드가 죽으면 새로 띄움, 개수가 모자라면 더 띄움)
        ▼
 노드(minikube 한 대) ── 파드 nginx-deploy-xxxx
        ▲
 서비스 nginx-service (NodePort 30080) ── 파드들에 요청을 나눠 줌
```
- **원하는 상태를 선언**하면 쿠버네티스가 그 상태를 계속 유지한다(파드를 지우면 바로 다시 생긴다).
- 이 실습은 **minikube**로 내 PC에 노드 하나짜리 클러스터를 만든다.

## 3. 14장과 달라지는 점
| 항목 | 14장 | 15장 |
|---|---|---|
| 마이크로서비스 코드 | - | 거의 그대로 (composite `WebClient` 빈 접근 제한자, 테스트 정리) |
| 새 파일 | - | `kubernetes/first-attempts/` nginx 디플로이먼트·서비스 매니페스트 |
| 실습 | 도커 컴포즈 | **minikube 클러스터 만들기, kubectl로 nginx 배포** (마이크로서비스 배포는 16장) |

## 4. 문서별로 생기는 것
| 문서 | 내용 |
|---|---|
| 01 | composite 정리 |
| 02 | 인가 서버·Eureka 테스트와 설정 정리 |
| 03 | nginx 매니페스트, minikube 시작, kubectl로 배포·확인 |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 클러스터 / 노드 | 쿠버네티스 전체 / 그 안의 서버 한 대(minikube는 노드 1개) |
| 파드(Pod) | 컨테이너를 감싼 가장 작은 실행 단위 |
| 디플로이먼트(Deployment) | "이 파드를 n개 유지해" 하는 선언. 파드가 죽으면 다시 만든다 |
| 레플리카셋(ReplicaSet) | 디플로이먼트가 만드는, 파드 개수를 실제로 맞추는 객체 |
| 서비스(Service) | 파드들 앞의 고정 이름·주소. 요청을 파드들에 나눠 준다. `NodePort`는 노드의 포트(30000~32767)로 밖에서 접근 |
| 네임스페이스 | 리소스를 묶는 이름 공간 (`first-attempts`, 16장부터 `hands-on`) |
| kubectl | 쿠버네티스에 명령하는 CLI |
| minikube | 내 PC에 연습용 쿠버네티스 클러스터를 만드는 도구 |
