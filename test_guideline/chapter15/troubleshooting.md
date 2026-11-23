# 15장 트러블슈팅

## `curl $(minikube ip):30080`이 응답 없음
**원인:** WSL + Docker Desktop에서는 minikube가 Docker Desktop 안의 네트워크(192.168.49.x)에 있어서 WSL에서 바로 닿지 않는다.
**해결:** `kubectl port-forward service/nginx-service 30080:80`을 켜 두고 `localhost:30080`으로 접속한다(확인 4).

## minikube를 띄우다가 Docker Desktop이 멈춤 (`docker: Bus error`, `command not found`)
**원인:** minikube(10GB)와 도커 컴포즈 컨테이너가 함께 떠서 Docker Desktop 메모리(이 PC는 약 15.5GB)가 모자랐다.
**해결:**
1. 관리자 PowerShell에서 Docker를 완전히 끄고 WSL을 재시작한다.
```powershell
Stop-Service com.docker.service -ErrorAction SilentlyContinue
Get-Process *docker* -ErrorAction SilentlyContinue | Stop-Process -Force
wsl --shutdown
```
2. Docker Desktop을 한 번만 실행하고 **Engine running**까지 기다린다.
3. 망가진 클러스터는 지우고 새로 만든다: `minikube delete` → `minikube start ...`
4. 앞으로는 minikube를 쓸 때 도커 컴포즈를 먼저 내린다(`docker compose down`).

## `kubectl get nodes`가 `NotReady`
막 만든 직후다. 30초쯤 뒤 다시 본다.
