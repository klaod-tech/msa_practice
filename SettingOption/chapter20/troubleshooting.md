# 20장 트러블슈팅

## test-em-all의 `Prometheus metrics tests`만 실패 (`000`)
**원인:** 이 줄은 `https://health.minikube.me/actuator/prometheus`로 443 포트를 고정해 쓴다. port-forward를 8443으로 했다.
**해결:** `kubectl port-forward -n istio-system service/istio-ingressgateway 443:443`(확인 3).

## `kubectl port-forward ... 443:443`이 `permission denied`
**원인:** 1024보다 낮은 포트는 관리자만 열 수 있다.
**해결:** 한 번만 설정한다([1장 4-5절](../chapter1/01-ubuntu-environment.md) ③ 443 포트).
```bash
echo "net.ipv4.ip_unprivileged_port_start=443" | sudo tee /etc/sysctl.d/99-unprivileged-443.conf
sudo sysctl -w net.ipv4.ip_unprivileged_port_start=443
```

## Grafana 알림(Alert)이 교재 화면과 다르다
교수님 안내: 이스티오 1.31.4의 Grafana는 12.x라 교재의 예전 알림(패널의 Alert 탭)이 없다. **Alerting → Contact points**에서 Email 연락처를 만들고(메일 서버 `mail-server.istio-system:1025`), **Alerting → Alert rules**에서 `resilience4j_circuitbreaker_state{state="closed"}` 같은 조건으로 규칙을 만든다.

## WSL 접속, 메모리
[../chapter18/troubleshooting.md](../chapter18/troubleshooting.md)와 같다.

## `helm upgrade` 직후 `kubectl wait`가 `Error from server (NotFound): pods "..." not found`
**원인:** 업그레이드로 파드가 새 것으로 바뀌는 중에, 기다리던 옛 파드가 지워졌다(검증 때 실제로 나왔다).
**해결:** 정상이다. `kubectl wait --timeout=900s --for=condition=ready pod --all -n hands-on`을 한 번 더 실행하면 새 파드들로 통과한다.
