# 08. 헬름 환경 차트 (istio-system 추가)

> 시작: 교수님 `Chapter17` 상태 / 결과: 교수님 `Chapter18`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`istio-system` 환경 차트: Kiali·Jaeger·Prometheus·Grafana를 `*.minikube.me`로 노출하고, 자체 서명 인증서를 만든다. dev-env/prod-env는 이스티오 리소스를 함께 설치한다.

## 8-1. `kubernetes/helm/environments/dev-env/Chart.yaml`
```bash
code kubernetes/helm/environments/dev-env/Chart.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for HANDS-ON MICROSERVICES WITH SPRING BOOT AND SPRING CLOUD running in a Dev and Test Environment
name: dev-env
version: 1.0.0

dependencies:

  - name: common
    version: 1.0.0
    repository: file://../../common

  - name: rabbitmq
    version: 1.0.0
    repository: file://../../components/rabbitmq

  - name: mongodb
    version: 1.0.0
    repository: file://../../components/mongodb

  - name: mysql
    version: 1.0.0
    repository: file://../../components/mysql

  - name: auth-server
    version: 1.0.0
    repository: file://../../components/auth-server

  - name: product
    version: 1.0.0
    repository: file://../../components/product

  - name: recommendation
    version: 1.0.0
    repository: file://../../components/recommendation

  - name: review
    version: 1.0.0
    repository: file://../../components/review

  - name: product-composite
    version: 1.0.0
    repository: file://../../components/product-composite
    # ★ 삭제 (3줄, 첫 줄: - name: zipkin-server)
```

## 8-2. `kubernetes/helm/environments/dev-env/templates/istio_base.yaml` (새 파일)
```bash
code kubernetes/helm/environments/dev-env/templates/istio_base.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.istio_base" . -}}
```

## 8-3. `kubernetes/helm/environments/dev-env/templates/istio_dr_mutual_tls.yaml` (새 파일)
```bash
code kubernetes/helm/environments/dev-env/templates/istio_dr_mutual_tls.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.istio_dr_mutual_tls" . -}}
```

## 8-4. `kubernetes/helm/environments/dev-env/values.yaml`
```bash
code kubernetes/helm/environments/dev-env/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 삭제 (15줄, 첫 줄: ingress:)
secrets:

  rabbitmq-server-credentials:
    RABBITMQ_DEFAULT_USER: rabbit-user-dev
    RABBITMQ_DEFAULT_PASS: rabbit-pwd-dev

  rabbitmq-credentials:
    SPRING_RABBITMQ_USERNAME: rabbit-user-dev
    SPRING_RABBITMQ_PASSWORD: rabbit-pwd-dev

  mongodb-server-credentials:
    MONGO_INITDB_ROOT_USERNAME: mongodb-user-dev
    MONGO_INITDB_ROOT_PASSWORD: mongodb-pwd-dev

  mongodb-credentials:
    SPRING_MONGODB_AUTHENTICATION_DATABASE: admin
    SPRING_MONGODB_USERNAME: mongodb-user-dev
    SPRING_MONGODB_PASSWORD: mongodb-pwd-dev

  mysql-server-credentials:
    MYSQL_ROOT_PASSWORD: rootpwd
    MYSQL_DATABASE: review-db
    MYSQL_USER: mysql-user-dev
    MYSQL_PASSWORD: mysql-pwd-dev

  mysql-credentials:
    SPRING_DATASOURCE_USERNAME: mysql-user-dev
    SPRING_DATASOURCE_PASSWORD: mysql-pwd-dev

rabbitmq:
  envFromSecretRefs:
    - rabbitmq-server-credentials

mongodb:
  envFromSecretRefs:
    - mongodb-server-credentials

mysql:
  envFromSecretRefs:
    - mysql-server-credentials

auth-server:
  envFromSecretRefs:
    - rabbitmq-credentials

product:
  envFromSecretRefs:
    - rabbitmq-credentials
    - mongodb-credentials

recommendation:
  envFromSecretRefs:
    - rabbitmq-credentials
    - mongodb-credentials

review:
  envFromSecretRefs:
    - rabbitmq-credentials
    - mysql-credentials

product-composite:
  envFromSecretRefs:
    - rabbitmq-credentials
# ★ 추가 (6줄)

destinationRules:
  - name: product-composite
  - name: auth-server
  - name: product
  - name: recommendation
  - name: review
```

## 8-5. `kubernetes/helm/environments/istio-system/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/istio-system
code kubernetes/helm/environments/istio-system/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for added definitions in the istio-system namespace
name: istio-system
version: 1.0.0
```

## 8-6. `kubernetes/helm/environments/istio-system/templates/expose-grafana.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/istio-system/templates
code kubernetes/helm/environments/istio-system/templates/expose-grafana.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: grafana-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "grafana.minikube.me"
    port:
      number: 443
      name: https-grafana
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: grafana-vs
  namespace: istio-system
spec:
  hosts:
  - "grafana.minikube.me"
  gateways:
  - grafana-gateway
  http:
  - route:
    - destination:
        host: grafana
        port:
          number: 3000
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: grafana
  namespace: istio-system
spec:
  host: grafana
  trafficPolicy:
    tls:
      mode: DISABLE
```

## 8-7. `kubernetes/helm/environments/istio-system/templates/expose-kiali.yml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/templates/expose-kiali.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: kiali-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "kiali.minikube.me"
    port:
      number: 443
      name: https-kiali
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: kiali-vs
  namespace: istio-system
spec:
  hosts:
  - "kiali.minikube.me"
  gateways:
  - kiali-gateway
  http:
  - route:
    - destination:
        host: kiali
        port:
          number: 20001
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: kiali
  namespace: istio-system
spec:
  host: kiali
  trafficPolicy:
    tls:
      mode: DISABLE
```

## 8-8. `kubernetes/helm/environments/istio-system/templates/expose-prometheus.yml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/templates/expose-prometheus.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: prometheus-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "prometheus.minikube.me"
    port:
      number: 443
      name: https-prom
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: prometheus-vs
  namespace: istio-system
spec:
  hosts:
  - "prometheus.minikube.me"
  gateways:
  - prometheus-gateway
  http:
  - route:
    - destination:
        host: prometheus
        port:
          number: 9090
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: prometheus
  namespace: istio-system
spec:
  host: prometheus
  trafficPolicy:
    tls:
      mode: DISABLE
```

## 8-9. `kubernetes/helm/environments/istio-system/templates/expose-tracing.yml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/templates/expose-tracing.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: tracing-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "tracing.minikube.me"
    port:
      number: 443
      name: https-tracing
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: tracing-vs
  namespace: istio-system
spec:
  hosts:
  - "tracing.minikube.me"
  gateways:
  - tracing-gateway
  http:
  - route:
    - destination:
        host: tracing
        port:
          number: 80
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: tracing
  namespace: istio-system
spec:
  host: tracing
  trafficPolicy:
    tls:
      mode: DISABLE
```

## 8-10. `kubernetes/helm/environments/istio-system/templates/hands-on-certificate.yaml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/templates/hands-on-certificate.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: hands-on-certificate
spec:
  commonName: minikube.me
  subject:
    countries:
    - my-country
    localities:
    - my-locality
    organizationalUnits:
    - my-ou
    organizations:
    - my-org
    postalCodes:
    - my-pc
    provinces:
    - my-province
    serialNumber: my-sn
    streetAddresses:
    - my-address
  dnsNames:
  - minikube.me
  - health.minikube.me
  - dashboard.minikube.me
  - kiali.minikube.me
  - tracing.minikube.me
  - prometheus.minikube.me
  - grafana.minikube.me
  - kibana.minikube.me
  - elasticsearch.minikube.me
  - mail.minikube.me
  issuerRef:
    name: ca-issuer
  secretName: hands-on-certificate
```

## 8-11. `kubernetes/helm/environments/istio-system/templates/selfsigned-issuer.yaml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/templates/selfsigned-issuer.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: ca-cert
spec:
  isCA: true
  commonName: hands-on-ca
  secretName: ca-secret
  issuerRef:
    name: selfsigned-issuer
---
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: ca-issuer
spec:
  ca:
    secretName: ca-secret
```

## 8-12. `kubernetes/helm/environments/istio-system/values.yaml` (새 파일)
```bash
code kubernetes/helm/environments/istio-system/values.yaml
```
붙여넣기 (새 파일)
```yaml

```

## 8-13. `kubernetes/helm/environments/prod-env/Chart.yaml`
```bash
code kubernetes/helm/environments/prod-env/Chart.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for HANDS-ON MICROSERVICES WITH SPRING BOOT AND SPRING CLOUD running in a QA or Production environment
name: prod-env
version: 1.0.0

dependencies:

  - name: common
    version: 1.0.0
    repository: file://../../common

  - name: auth-server
    version: 1.0.0
    repository: file://../../components/auth-server

  - name: product
    version: 1.0.0
    repository: file://../../components/product

  - name: recommendation
    version: 1.0.0
    repository: file://../../components/recommendation

  - name: review
    version: 1.0.0
    repository: file://../../components/review

  - name: product-composite
    version: 1.0.0
    repository: file://../../components/product-composite

  - name: product-green  # ★ 변경 (전: - name: zipkin-server)
    version: 1.0.0
    # ★ 변경 (7줄, 전: 1줄)
    repository: file://../../components/product-green

  - name: recommendation-green
    version: 1.0.0
    repository: file://../../components/recommendation-green

  - name: review-green
    version: 1.0.0
    repository: file://../../components/review-green
```

## 8-14. `kubernetes/helm/environments/prod-env/templates/istio_base.yaml` (새 파일)
```bash
code kubernetes/helm/environments/prod-env/templates/istio_base.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.istio_base" . -}}
```

## 8-15. `kubernetes/helm/environments/prod-env/templates/istio_dr_mutual_tls.yaml` (새 파일)
```bash
code kubernetes/helm/environments/prod-env/templates/istio_dr_mutual_tls.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.istio_dr_mutual_tls" . -}}
```

## 8-16. `kubernetes/helm/environments/prod-env/templates/istio_vs_green_blue_deploy.yaml` (새 파일)
```bash
code kubernetes/helm/environments/prod-env/templates/istio_vs_green_blue_deploy.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.istio_vs_green_blue_deploy" . -}}
```

## 8-17. `kubernetes/helm/environments/prod-env/values.yaml`
```bash
code kubernetes/helm/environments/prod-env/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 삭제 (15줄, 첫 줄: ingress:)
secrets:

  rabbitmq-credentials:
    SPRING_RABBITMQ_USERNAME: rabbit-user-prod
    SPRING_RABBITMQ_PASSWORD: rabbit-pwd-prod

  mongodb-credentials:
    SPRING_MONGODB_AUTHENTICATION_DATABASE: admin
    SPRING_MONGODB_USERNAME: mongodb-user-prod
    SPRING_MONGODB_PASSWORD: mongodb-pwd-prod

  mysql-credentials:
    SPRING_DATASOURCE_USERNAME: mysql-user-prod
    SPRING_DATASOURCE_PASSWORD: mysql-pwd-prod

auth-server:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

product:
  fullnameOverride: product-v1  # ★ 추가
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
    - mongodb-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

recommendation:
  fullnameOverride: recommendation-v1  # ★ 추가
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
    - mongodb-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

review:
  fullnameOverride: review-v1  # ★ 추가
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
    - mysql-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

product-composite:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

product-green:  # ★ 변경 (전: zipkin-server:)
  fullnameOverride: product-v2  # ★ 추가
  image:  # ★ 추가
    tag: v2  # ★ 추가
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"  # ★ 변경 (전: LOGGING_LEVEL_ROOT: WARN)
  envFromSecretRefs:  # ★ 추가
    - rabbitmq-credentials  # ★ 추가
    - mongodb-credentials  # ★ 추가
  resources:
    requests:
      memory: 200Mi  # ★ 변경 (전: memory: 300Mi)
    limits:
      # ★ 변경 (60줄, 전: 1줄)
      memory: 400Mi

recommendation-green:
  fullnameOverride: recommendation-v2
  image:
    tag: v2
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
    - mongodb-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

review-green:
  fullnameOverride: review-v2
  image:
    tag: v2
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials
    - mysql-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

virtualServices:
  - product
  - recommendation
  - review

destinationRules:
  - name: product-composite
  - name: auth-server
  - name: product
    subsets:
    - labels:
        version: v1
      name: old
    - labels:
        version: v2
      name: new
  - name: recommendation
    subsets:
    - labels:
        version: v1
      name: old
    - labels:
        version: v2
      name: new
  - name: review
    subsets:
    - labels:
        version: v1
      name: old
    - labels:
        version: v2
      name: new
```

## 8-18. 지울 파일
```bash
cd ~/msa
rm kubernetes/helm/environments/dev-env/templates/ingress.yaml
rm kubernetes/helm/environments/dev-env/templates/issuer.yaml
rm kubernetes/helm/environments/prod-env/templates/ingress.yaml
rm kubernetes/helm/environments/prod-env/templates/issuer.yaml
```

다음: [09-kubernetes.md](09-kubernetes.md)
