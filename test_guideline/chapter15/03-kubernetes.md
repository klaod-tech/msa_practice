# 03. nginx 매니페스트, minikube, kubectl

> 시작: 교수님 `Chapter14` 상태 / 결과: 교수님 `Chapter15`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

쿠버네티스에 처음 올려 볼 nginx 예제다.
- `nginx-deployment.yaml`: nginx 파드 1개(`replicas: 1`)를 유지하는 디플로이먼트. 이미지는 `nginx:latest`.
- `nginx-service.yaml`: 그 파드들 앞에 서비스. `NodePort` 30080으로 밖에서 접근한다.

## 3-1. `kubernetes/first-attempts/nginx-deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/first-attempts
code kubernetes/first-attempts/nginx-deployment.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deploy
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nginx-app
  template:
    metadata:
      labels:
        app: nginx-app
    spec:
      containers:
      - name: nginx-container
        image: nginx:latest
        ports:
        - containerPort: 80
```

## 3-2. `kubernetes/first-attempts/nginx-service.yaml` (새 파일)
```bash
code kubernetes/first-attempts/nginx-service.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  type: NodePort
  selector:
    app: nginx-app
  ports:
    - targetPort: 80
      port: 80
      nodePort: 30080
```

## 확인 1: 마이크로서비스는 14장처럼 도커 컴포즈로
```bash
cd ~/msa
./gradlew build
docker compose build
./test-em-all.bash start stop
```
마지막 줄 `End, all tests OK`. 마이크로서비스를 쿠버네티스에 올리는 것은 16장이다.

## 확인 2: minikube 클러스터 만들기
```bash
minikube start --driver=docker --container-runtime=docker --cpus=4 --memory=10g
kubectl get nodes
```
`minikube   Ready   control-plane ...`. 처음에는 이미지를 받느라 몇 분 걸린다. 메모리 10GB를 쓰므로 **도커 컴포즈로 띄운 것은 먼저 내린다**(`docker compose down`). 둘을 같이 띄우면 Docker Desktop이 멈출 수 있다([troubleshooting.md](troubleshooting.md)).

## 확인 3: nginx 배포
```bash
kubectl create namespace first-attempts
kubectl config set-context $(kubectl config current-context) --namespace=first-attempts
kubectl apply -f kubernetes/first-attempts/nginx-deployment.yaml
kubectl apply -f kubernetes/first-attempts/nginx-service.yaml
kubectl get all
```
파드 `nginx-deploy-...` 1개가 `Running`, 서비스 `nginx-service`가 `NodePort 80:30080/TCP`.

## 확인 4: 접속 (교수님 안내와 다름: WSL)
교재·교수님 안내는 `curl $(minikube ip):30080`인데, **WSL + Docker Desktop에서는 `minikube ip`(192.168.49.2)에 접속되지 않는다**(응답 없음). 대신 `kubectl port-forward`로 내 PC의 포트에 연결한다.
```bash
kubectl port-forward service/nginx-service 30080:80
```
`Forwarding from 127.0.0.1:30080 -> 80`이 나오고 터미널이 그대로 멈춰 있으면 연결 중인 것이다. **새 터미널**을 열어서:
```bash
curl -s localhost:30080 | grep title
```
`<title>Welcome to nginx!</title>`. 브라우저로 http://localhost:30080 을 열어도 된다. 다 보면 port-forward 터미널에서 `Ctrl+C`.

## 확인 5: 파드를 지워도 다시 생긴다
```bash
kubectl delete pod -l app=nginx-app
kubectl get pods
```
지운 파드 대신 이름이 다른 새 파드가 바로 생긴다. 디플로이먼트가 "1개 유지"를 지키기 때문이다.

## 끝내기
```bash
kubectl delete namespace first-attempts
kubectl config set-context $(kubectl config current-context) --namespace=default
minikube stop
```
`minikube stop`은 클러스터를 멈추기만 한다(다시 `minikube start`로 켠다). 완전히 지우려면 `minikube delete`.
