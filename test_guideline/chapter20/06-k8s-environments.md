# 06. 헬름 환경 차트

> 시작: 교수님 `Chapter19` 상태 / 결과: 교수님 `Chapter20`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

환경 차트 정리.

## 6-1. `kubernetes/helm/environments/istio-system/templates/expose-mail.yml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/templates/expose-mail.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: mail-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "mail.minikube.me"
    port:
      number: 443
      name: https-mail
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: mail-vs
  namespace: istio-system
spec:
  hosts:
  - "mail.minikube.me"
  gateways:
  - mail-gateway
  http:
  - route:
    - destination:
        host: mail-server
        port:
          number: 1080
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: mail
  namespace: istio-system
spec:
  host: mail-server
  trafficPolicy:
    tls:
      mode: DISABLE
```

다음: [07-kubernetes.md](07-kubernetes.md)
