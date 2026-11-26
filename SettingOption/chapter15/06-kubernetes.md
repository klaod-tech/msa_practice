# 06. nginx 매니페스트, minikube, kubectl

> 시작: 14장까지 따라 한 상태 / 결과: 15장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

쿠버네티스에 처음 올려 볼 nginx 예제다.
- `nginx-deployment.yaml`: nginx 파드 1개(`replicas: 1`)를 유지하는 디플로이먼트. 이미지는 `nginx:latest`.
- `nginx-service.yaml`: 그 파드들 앞에 서비스. `NodePort` 30080으로 밖에서 접근한다.

## 6-1. `kubernetes/first-attempts/nginx-deployment.yaml` (새 파일)
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

## 6-2. `kubernetes/first-attempts/nginx-service.yaml` (새 파일)
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

다음: [07-build-compose-test.md](07-build-compose-test.md)
