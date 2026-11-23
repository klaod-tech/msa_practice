# 07. 헬름 환경 차트

> 시작: 교수님 `Chapter16` 상태 / 결과: 교수님 `Chapter17`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

환경 차트에 Ingress(호스트 `minikube.me`, 443)와 인증서 발급 설정.

## 7-1. `kubernetes/helm/environments/dev-env/Chart.yaml`
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

  # ★ 삭제 (6줄, 첫 줄: - name: config-server)
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

  - name: zipkin-server
    version: 1.0.0
    repository: file://../../components/zipkin-server
```

## 7-2. `kubernetes/helm/environments/dev-env/templates/ingress.yaml` (새 파일)
```bash
code kubernetes/helm/environments/dev-env/templates/ingress.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.ingress" . -}}
```

## 7-3. `kubernetes/helm/environments/dev-env/templates/issuer.yaml` (새 파일)
```bash
code kubernetes/helm/environments/dev-env/templates/issuer.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.issuer" . -}}
```

## 7-4. `kubernetes/helm/environments/dev-env/values.yaml`
```bash
code kubernetes/helm/environments/dev-env/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 추가 (15줄)
ingress:
  host: minikube.me
  paths:
    - path: /oauth2
      service: auth-server
    - path: /login
      service: auth-server
    - path: /error
      service: auth-server
    - path: /product-composite
      service: product-composite
    - path: /actuator/health
      service: product-composite
    - path: /openapi
      service: product-composite

secrets:

  # ★ 변경 (3줄, 전: 4줄)
  rabbitmq-server-credentials:
    RABBITMQ_DEFAULT_USER: rabbit-user-dev
    RABBITMQ_DEFAULT_PASS: rabbit-pwd-dev

  # ★ 변경 (24줄, 전: 4줄)
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
  # ★ 삭제 (6줄, 첫 줄: env:)
  envFromSecretRefs:
    # ★ 변경 (1줄, 전: 4줄)
    - mysql-server-credentials

auth-server:
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)

product:
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
    - mongodb-credentials  # ★ 추가

recommendation:
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
    - mongodb-credentials  # ★ 추가

review:
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
    - mysql-credentials  # ★ 추가

product-composite:
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
```

## 7-5. `kubernetes/helm/environments/prod-env/Chart.yaml`
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
# ★ 삭제 (6줄, 첫 줄: - name: config-server)

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

  - name: zipkin-server
    version: 1.0.0
    repository: file://../../components/zipkin-server
```

## 7-6. `kubernetes/helm/environments/prod-env/templates/ingress.yaml` (새 파일)
```bash
code kubernetes/helm/environments/prod-env/templates/ingress.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.ingress" . -}}
```

## 7-7. `kubernetes/helm/environments/prod-env/templates/issuer.yaml` (새 파일)
```bash
code kubernetes/helm/environments/prod-env/templates/issuer.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.issuer" . -}}
```

## 7-8. `kubernetes/helm/environments/prod-env/values.yaml`
```bash
code kubernetes/helm/environments/prod-env/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
# ★ 추가 (15줄)
ingress:
  host: minikube.me
  paths:
    - path: /oauth2
      service: auth-server
    - path: /login
      service: auth-server
    - path: /error
      service: auth-server
    - path: /product-composite
      service: product-composite
    - path: /actuator/health
      service: product-composite
    - path: /openapi
      service: product-composite

secrets:

  # ★ 변경 (3줄, 전: 4줄)
  rabbitmq-credentials:
    SPRING_RABBITMQ_USERNAME: rabbit-user-prod
    SPRING_RABBITMQ_PASSWORD: rabbit-pwd-prod

  # ★ 변경 (4줄, 전: 4줄)
  mongodb-credentials:
    SPRING_MONGODB_AUTHENTICATION_DATABASE: admin
    SPRING_MONGODB_USERNAME: mongodb-user-prod
    SPRING_MONGODB_PASSWORD: mongodb-pwd-prod

  # ★ 변경 (3줄, 전: 25줄)
  mysql-credentials:
    SPRING_DATASOURCE_USERNAME: mysql-user-prod
    SPRING_DATASOURCE_PASSWORD: mysql-pwd-prod

auth-server:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

product:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
    - mongodb-credentials  # ★ 추가
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

recommendation:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
    - mongodb-credentials  # ★ 추가
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

review:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
    - mysql-credentials  # ★ 추가
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
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

zipkin-server:
  env:
    LOGGING_LEVEL_ROOT: WARN
  resources:
    requests:
      memory: 300Mi
    limits:
      memory: 600Mi
```

다음: [08-build-compose-test.md](08-build-compose-test.md)
