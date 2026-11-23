# 17장 트러블슈팅

## test-em-all이 `Wait for: curl -k https://minikube.me:8443/actuator/health...`에서 계속 retry
**원인 1:** `minikube.me`를 찾지 못한다. WSL을 재시작하면 `/etc/hosts`가 새로 만들어져서 넣어 둔 `127.0.0.1 minikube.me ...` 줄이 사라진다(검증 중 실제로 겪었다).
**확인:** `getent hosts minikube.me`가 아무것도 안 나오면 이 경우다.
**해결:** [../README.md](../README.md)의 "쿠버네티스 준비" 3번 두 줄(`generateHosts = false`와 hosts 줄)을 실행한다.

**원인 2:** Ingress 컨트롤러 port-forward를 안 켰거나 끊겼다.
**확인:** `curl -sk -o /dev/null -w "%{http_code}\n" -H "Host: minikube.me" https://localhost:8443/actuator/health`가 `200`이면 Ingress는 정상이다(원인 1). `000`이면 port-forward 터미널을 확인한다.

## `minikube addons enable ingress` 뒤 바로 port-forward가 실패
Ingress 컨트롤러가 아직 뜨는 중이다. 아래가 끝난 뒤 port-forward 한다.
```bash
kubectl -n ingress-nginx wait --for=condition=ready pod -l app.kubernetes.io/component=controller --timeout=300s
```

## 파드가 `ErrImageNeverPull`, 메모리 문제
[../chapter16/troubleshooting.md](../chapter16/troubleshooting.md)와 같다.
