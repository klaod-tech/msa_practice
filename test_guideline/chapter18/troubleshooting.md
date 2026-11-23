# 18장 트러블슈팅

## `minikube tunnel` / 게이트웨이 IP로 접속이 안 됨 (WSL)
**원인:** WSL + Docker Desktop에서는 minikube가 만든 IP에 닿지 않는다.
**해결:** `kubectl port-forward -n istio-system service/istio-ingressgateway 8443:443`을 켜 두고 `PORT=8443 HEALTH_URL=https://health.minikube.me:8443`으로 테스트한다(확인 5). `/etc/hosts`에 `127.0.0.1 minikube.me health.minikube.me ...`가 있어야 한다([../README.md](../README.md)).

## 파드 READY가 `1/1` (프록시가 안 붙음)
**원인:** `hands-on` 네임스페이스를 `kubectl apply -f kubernetes/hands-on-namespace.yml` 없이 만들었다(`helm install ... --create-namespace`). 그러면 `istio-injection: enabled` 라벨이 없다.
**해결:** `helm uninstall hands-on-dev-env -n hands-on` → `kubectl delete namespace hands-on` → 확인 4를 순서대로. MongoDB, MySQL, RabbitMQ는 원래 `1/1`이다.

## Kiali / Jaeger 화면이 404
경로가 있다. Kiali는 `https://kiali.minikube.me:8443/kiali/`, Jaeger는 `https://tracing.minikube.me:8443/jaeger/`(검증 때 둘 다 200, 루트 `/`는 302·404).
**Windows 브라우저로 열려면** WSL의 `/etc/hosts`가 아니라 Windows의 `C:\Windows\System32\drivers\etc\hosts`에도 같은 줄(`127.0.0.1 minikube.me kiali.minikube.me ...`)이 있어야 한다(관리자 메모장으로 편집). port-forward는 WSL 터미널에서 켜 둔다.

## `helm dep up`에서 `Error: Chart.yaml file is missing`
17장의 `zipkin-server` 차트 폴더가 남아 있다(`Chart.lock`, `charts/`만 남은 경우). [07](07-k8s-components.md) 맨 아래의 `rm -rf kubernetes/helm/components/zipkin-server`를 한다.

## 메모리
18장부터 minikube를 12GB로 만든다. WSL 메모리를 24GB로 늘려 두어야 한다([../README.md](../README.md) 준비 2번). 파드 몇 개가 처음에 `RESTARTS 1`인 것은 정상이다([../chapter16/troubleshooting.md](../chapter16/troubleshooting.md)).
