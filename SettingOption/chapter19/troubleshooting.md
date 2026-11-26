# 19장 트러블슈팅

## `helm install logging-hands-on-add-on ... --wait`가 시간 초과
**원인:** Elasticsearch·Kibana 이미지를 받는 데 오래 걸렸다.
**해결:** 확인 3처럼 `eval $(minikube docker-env)` 후 `docker pull`로 먼저 받는다. 이미 실패했으면 `helm uninstall logging-hands-on-add-on -n logging` 후 다시 설치.

## Fluentd 파드가 `ErrImageNeverPull`
`docker build ... -t hands-on/fluentd:v1`을 `eval $(minikube docker-env)` 없이 했다. 같은 터미널에서 다시 빌드하고 `kubectl delete pod -l app=fluentd -n kube-system`.

## Elasticsearch에 `logstash-*` 인덱스가 없다
로그가 모이는 데 1~2분 걸린다. 그래도 없으면 `kubectl logs -n kube-system -l app=fluentd --tail=30`에서 Elasticsearch 연결 오류를 본다.

## WSL 접속, 메모리
[../chapter18/troubleshooting.md](../chapter18/troubleshooting.md)와 같다.
