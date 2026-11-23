# 00. 배경: 마이크로서비스를 쿠버네티스에 배포, 15장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서.

## 1. 15장까지
- 마이크로서비스는 도커 컴포즈로만 띄웠다. 쿠버네티스에는 nginx만 올려 봤다.
- 서비스끼리는 **Eureka**에 등록하고 이름으로 찾았다(`lb://product-composite`).

## 2. 16장이 끝나면
```
 helm install hands-on-dev-env  ─▶  네임스페이스 hands-on
     ├─ 디플로이먼트 + 서비스: gateway(NodePort 30443), auth-server, config-server,
     │                       product, recommendation, review, product-composite, zipkin-server
     └─ 디플로이먼트 + 서비스: mongodb, mysql, rabbitmq   (dev-env만. prod-env는 DB를 쿠버네티스 밖에 둠)

 서비스끼리: http://product  ← 쿠버네티스 서비스 이름이 DNS 이름 (Eureka 필요 없음)
```
- **Eureka를 없앤다.** 쿠버네티스 서비스가 이름 → 파드 주소 변환과 로드 밸런싱을 해 준다.
- 배포 정의를 **헬름 차트**로 만든다. 서비스마다 비슷한 디플로이먼트·서비스 YAML을 공통 템플릿 하나로 찍어낸다.
- 쿠버네티스가 파드 상태를 알 수 있게 **프로브**(`/actuator/health/liveness`, `readiness`)를 켜고, 꺼질 때 처리 중인 요청을 마치도록 **그레이스풀 셧다운**을 켠다.

## 3. 헬름 차트 구조
| 차트 | 위치 | 하는 일 |
|---|---|---|
| `common` | `helm/common` | 디플로이먼트, 서비스, ConfigMap 템플릿 (`_deployment.yaml` 등). 혼자 설치하지 않는 라이브러리 차트 |
| 구성 요소 | `helm/components/<서비스>` | 서비스마다 `values.yaml`(이미지, 포트, 프로브, 환경 변수)만 두고 common 템플릿을 쓴다 |
| 환경 | `helm/environments/dev-env`, `prod-env` | 구성 요소 차트들을 묶어 한 번에 설치. 환경별 값(프로필, 리소스)을 덮어쓴다 |

## 4. 15장과 달라지는 점
| 항목 | 15장 | 16장 |
|---|---|---|
| 서비스 검색 | Eureka (`lb://...`) | 쿠버네티스 서비스 (`http://...`). `eureka-server` 프로젝트와 config-repo의 `eureka-server.yml` 삭제 |
| 설정 | - | 프로브, `server.shutdown: graceful`, `prod` 프로필 |
| 새 폴더 | - | `kubernetes/helm/` (common, components, environments) |
| 실행 | 도커 컴포즈 | 도커 컴포즈 + **minikube에 helm install** |
| test-em-all | - | `USE_K8S=true`면 쿠버네티스 방식으로 확인 |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 헬름(Helm) / 차트 | 쿠버네티스용 패키지 관리자 / 템플릿 + 값으로 된 배포 묶음 |
| `values.yaml` | 차트 템플릿에 넣을 값 |
| `helm dep up` | 차트가 쓰는 다른 차트(의존성)를 `charts/`에 받아 오기 |
| 프로브(liveness/readiness) | 살아 있는가(아니면 재시작) / 요청 받을 준비가 됐는가(아니면 트래픽 안 보냄) |
| 그레이스풀 셧다운 | 종료 신호를 받으면 새 요청은 막고 처리 중인 요청은 끝낸 뒤 종료 |
| ConfigMap | 설정 파일을 담는 쿠버네티스 객체. 16장은 config-repo 파일을 ConfigMap으로 config-server에 넣는다 |
