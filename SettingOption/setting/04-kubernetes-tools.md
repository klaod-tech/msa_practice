# 04. kubectl, minikube, istioctl

## kubectl
```bash
curl -LO "https://dl.k8s.io/release/v1.32.0/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
rm kubectl
kubectl version --client
```

## minikube
```bash
curl -LO https://storage.googleapis.com/minikube/releases/v1.35.0/minikube-linux-amd64
sudo install -o root -g root -m 0755 minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64
minikube version
```

## istioctl (버전 1.24.2로 고정)
```bash
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.24.2 TARGET_ARCH=x86_64 sh -
sudo install -o root -g root -m 0755 istio-1.24.2/bin/istioctl /usr/local/bin/istioctl
rm -r istio-1.24.2
istioctl version --remote=false
```
버전을 지정하지 않으면 최신 버전이 설치된다 → [troubleshooting.md](troubleshooting.md)

다음: [05-verify-and-book-source.md](05-verify-and-book-source.md)
