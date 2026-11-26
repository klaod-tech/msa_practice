# 16장 트러블슈팅

## `HOST=$(minikube ip) ...`로 테스트하면 연결이 안 됨
**원인:** WSL + Docker Desktop에서는 `minikube ip`(192.168.49.x)에 닿지 않는다.
**해결:** `kubectl port-forward -n hands-on service/gateway 30443:443`을 켜 두고 `HOST=localhost PORT=30443`(확인 4).

## 파드가 `ErrImageNeverPull`
**원인:** 이미지를 minikube 안이 아니라 내 PC의 도커에 만들었다. `eval $(minikube docker-env)`를 안 했거나 다른 터미널에서 빌드했다.
**해결:** 같은 터미널에서 `eval $(minikube docker-env)` 후 `docker compose build`. 그다음 `kubectl delete pod --all -n hands-on`(다시 만들어지며 이미지를 찾는다).

## minikube를 띄우다가 Docker Desktop이 멈춤
도커 컴포즈 컨테이너가 떠 있는 채로 minikube(10GB)를 띄우면 메모리가 모자랄 수 있다. 복구는 [../chapter15/troubleshooting.md](../chapter15/troubleshooting.md).

## 파드가 오래 `0/1 Running`
처음에는 이미지 시작과 프로브 통과까지 몇 분 걸린다. `kubectl get pods -n hands-on -w`로 지켜보고, 오래 안 되면 `kubectl logs -n hands-on deploy/<이름>`.

## 파드 몇 개가 `RESTARTS 1`
**원인:** 설정 서버나 MongoDB/MySQL이 준비되기 전에 먼저 뜬 서비스가 시작에 실패하고, 쿠버네티스가 다시 띄운 것이다. 검증 때도 product, recommendation이 1번씩 재시작한 뒤 정상(`1/1 Running`)이 됐다.
**해결:** 정상이다. `kubectl wait ...`가 끝날 때까지 기다린다. 재시작이 계속 늘면 `kubectl logs -n hands-on deploy/<이름> --previous`로 직전 실패 원인을 본다.

## 이 PC에서 메모리
WSL이 쓸 수 있는 메모리가 16GB일 때 minikube(10GB)를 띄우면 Docker Desktop이 멈췄다. `%UserProfile%\.wslconfig`에 아래를 넣고 `wsl --shutdown` 후 Docker Desktop을 다시 켜서 해결했다(PC 메모리 32GB 기준).
```ini
[wsl2]
memory=24GB
swap=8GB
```
