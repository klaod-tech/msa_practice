# 00. 배경: 지표 모니터링 (Prometheus, Grafana), 19장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서.

## 1. 19장까지
- 로그(19장)와 추적(18장)은 있지만, "지금 CPU·메모리·요청 수·오류율이 얼마인가" 같은 **숫자(지표)**를 시간 순으로 보는 곳이 없다.

## 2. 20장이 끝나면
```
 서비스마다 /actuator/prometheus (Micrometer가 지표를 Prometheus 형식으로)
        ▲ 주기적으로 긁어 감(scrape)
 Prometheus(이스티오 애드온) ─▶ Grafana 대시보드 https://grafana.minikube.me
                                  └ 알림(Grafana Alerting) → 메일(시험용 메일 서버)
```
- 서비스에 `micrometer-registry-prometheus`를 넣으면 JVM 메모리, 요청 수, 서킷 브레이커 상태 같은 지표가 `/actuator/prometheus`에 나온다.
- 이스티오의 Prometheus가 이를 모으고, Grafana에서 대시보드로 본다.
- **교재와 다른 점(교수님 문서):** 이스티오 1.31.4의 Grafana는 12.x라 교재의 예전 알림 방식이 없다. 알림은 Grafana Alerting으로 한다.

## 3. 19장과 달라지는 점
| 항목 | 19장 | 20장 |
|---|---|---|
| 의존성 (서비스마다) | - | `micrometer-registry-prometheus` |
| 설정 | - | `management.endpoints.web.exposure.include`에 `prometheus` 추가 |
| 헬름 | - | 서비스 차트에 Prometheus가 긁어 가도록 어노테이션, 환경 차트에 메일 서버 등 |
| 새 파일 | - | `kubernetes/grafana/` 대시보드(JSON) |
| test-em-all | - | 쿠버네티스에서 `/actuator/prometheus` 200 확인 |

## 4. 용어
| 용어 | 뜻 |
|---|---|
| 지표(metric) | 시간에 따라 바뀌는 숫자(요청 수, 응답 시간, 메모리) |
| Micrometer | 스프링이 쓰는 지표 API. Prometheus 형식으로 내보낼 수 있다 |
| Prometheus / scrape | 지표를 모아 저장하는 서버 / 지표를 주기적으로 긁어 가는 것 |
| Grafana | 지표 대시보드와 알림 화면 |
