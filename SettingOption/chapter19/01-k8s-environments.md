# 01. logging 환경 차트 (Elasticsearch, Kibana)

> 시작: 18장까지 따라 한 상태 / 결과: 19장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

`logging` 환경 차트(새 폴더): Elasticsearch와 Kibana 디플로이먼트·서비스, 그리고 이스티오 Gateway로 `elasticsearch.minikube.me`, `kibana.minikube.me`에 노출.

## 1-1. `kubernetes/helm/environments/logging/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/logging
code kubernetes/helm/environments/logging/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for added definitions in the logging namespace
name: logging
version: 1.0.0
```

## 1-2. `kubernetes/helm/environments/logging/templates/elasticsearch.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/logging/templates
code kubernetes/helm/environments/logging/templates/elasticsearch.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: elasticsearch
spec:
  selector:
    matchLabels:
      component: elasticsearch
  template:
    metadata:
      labels:
        component: elasticsearch
    spec:
      containers:
      - name: elasticsearch
        image: docker.elastic.co/elasticsearch/elasticsearch:7.17.28
        env:
        - name: discovery.type
          value: single-node
        ports:
        - containerPort: 9200
          name: http
          protocol: TCP
        resources:
          limits:
            cpu: 500m
            memory: 2Gi
          requests:
            cpu: 500m
            memory: 2Gi

---

apiVersion: v1
kind: Service
metadata:
  name: elasticsearch
  labels:
    service: elasticsearch
spec:
  type: ClusterIP
  selector:
    component: elasticsearch
  ports:
  - port: 9200
    targetPort: 9200
```

## 1-3. `kubernetes/helm/environments/logging/templates/expose-elasticsearch.yml` (새 파일)
```bash
code kubernetes/helm/environments/logging/templates/expose-elasticsearch.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: elasticsearch-gateway
  namespace: logging
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "elasticsearch.minikube.me"
    port:
      number: 443
      name: https-tracing
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: elasticsearch-vs
  namespace: logging
spec:
  hosts:
  - "elasticsearch.minikube.me"
  gateways:
  - elasticsearch-gateway
  http:
  - route:
    - destination:
        host: elasticsearch
        port:
          number: 9200
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: elasticsearch
  namespace: logging
spec:
  host: elasticsearch
  trafficPolicy:
    tls:
      mode: DISABLE
```

## 1-4. `kubernetes/helm/environments/logging/templates/expose-kibana.yml` (새 파일)
```bash
code kubernetes/helm/environments/logging/templates/expose-kibana.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: kibana-gateway
  namespace: logging
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "kibana.minikube.me"
    port:
      number: 443
      name: https-tracing
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: kibana-vs
  namespace: logging
spec:
  hosts:
  - "kibana.minikube.me"
  gateways:
  - kibana-gateway
  http:
  - route:
    - destination:
        host: kibana
        port:
          number: 5601
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: kibana
  namespace: logging
spec:
  host: kibana
  trafficPolicy:
    tls:
      mode: DISABLE
```

## 1-5. `kubernetes/helm/environments/logging/templates/kibana.yml` (새 파일)
```bash
code kubernetes/helm/environments/logging/templates/kibana.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kibana
spec:
  selector:
    matchLabels:
      run: kibana
  template:
    metadata:
      labels:
        run: kibana
    spec:
      containers:
      - name: kibana
        image: docker.elastic.co/kibana/kibana:7.17.28
        env:
        - name: ELASTICSEARCH_URL
          value: http://elasticsearch:9200
        ports:
        - containerPort: 5601
          name: http
          protocol: TCP

---

apiVersion: v1
kind: Service
metadata:
  name: kibana
  labels:
    service: kibana
spec:
  type: ClusterIP
  selector:
    run: kibana
  ports:
  - port: 5601
    targetPort: 5601
```

## 1-6. `kubernetes/helm/environments/logging/values.yaml` (새 파일)
```bash
code kubernetes/helm/environments/logging/values.yaml
```
붙여넣기 (새 파일)
```yaml

```

다음: [02-kubernetes.md](02-kubernetes.md)
