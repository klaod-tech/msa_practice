# 00. 배경: 서비스 메시(이스티오)는 무엇이고, 17장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서.

## 1. 17장까지
- 서비스끼리의 통신은 평문 HTTP, 누가 누구를 얼마나 부르는지 한눈에 볼 방법이 없다.
- 재시도·타임아웃 같은 회복성은 코드(Resilience4j)에 들어 있다. 새 버전을 배포하면 옛 버전이 한 번에 바뀐다.

## 2. 18장이 끝나면
```
 밖 ─HTTPS─▶ istio-ingressgateway (Gateway, VirtualService) ─▶ product-composite ─▶ product ...
                                                              │
 파드마다 옆에 붙는 사이드카 프록시(Envoy) ◀── istiod가 설정을 내려 줌
   · 서비스 사이 통신을 mTLS로 암호화        · JWT 검증(RequestAuthentication)
   · 요청 수, 오류, 지연을 모아 Prometheus/Kiali  · 추적을 Jaeger로
   · 지연/오류 주입(회복성 시험)              · 트래픽을 옛 버전/새 버전에 나눠 보내기(블루/그린)
```
- 코드는 거의 그대로다. **파드마다 붙는 프록시(사이드카)**가 통신을 대신 처리하고, 그 동작을 쿠버네티스 리소스(YAML)로 정한다.
- 관찰 도구: **Kiali**(서비스 그래프), **Jaeger**(추적), **Prometheus**, **Grafana**. `kiali.minikube.me` 같은 주소로 연다.

## 3. 17장과 달라지는 점
| 항목 | 17장 | 18장 |
|---|---|---|
| 밖에서 들어오는 길 | Ingress | **이스티오 Ingress Gateway** (`minikube tunnel`로 IP 받기) |
| 추적 | Zipkin 서버 + OTel 브리지 | 이스티오의 **Jaeger**로. Brave 브리지 + **B3 헤더** 전파(`TracingConfig`) |
| 서비스 사이 | 평문 | **mTLS** (`DestinationRule`) |
| 새 차트·파일 | - | `istio-system` 환경 차트(관찰 도구 노출, 인증서), `-green` 구성 요소(블루/그린), `routing-tests`, `resilience-tests`, `hands-on-namespace.yml`(사이드카 자동 주입) |
| API | - | composite가 받은 `X-group` 헤더를 핵심 서비스로 넘김(그룹별 라우팅 시험용) |
| test-em-all | 기본 compose | **기본이 이스티오 구성**(`HOST=minikube.me`, `PORT=443`). compose로 돌릴 때는 `USE_K8S=false` 등을 준다 |

## 4. 용어
| 용어 | 뜻 |
|---|---|
| 서비스 메시 | 서비스 사이 통신을 프록시들로 감싸 관찰·보안·제어하는 층 |
| 사이드카 / Envoy | 파드에 함께 뜨는 프록시 컨테이너 / 이스티오가 쓰는 프록시 |
| istiod | 이스티오 컨트롤 플레인. 프록시들에 설정과 인증서를 내려 준다 |
| Gateway / VirtualService / DestinationRule | 밖에서 들어오는 입구 / 요청을 어디로 보낼지(가중치, 헤더 조건) / 보낸 뒤의 정책(mTLS, 버전 묶음) |
| mTLS | 양쪽이 인증서로 서로 확인하는 TLS |
| 블루/그린 배포 | 옛 버전(blue)과 새 버전(green)을 같이 띄우고 트래픽 비율을 옮기는 배포 |
| `minikube tunnel` | LoadBalancer 서비스에 IP를 붙여 주는 명령. 켜 둔 터미널을 닫으면 끊긴다 |
