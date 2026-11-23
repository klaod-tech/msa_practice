# 07. 헬름 환경 차트 (dev-env, prod-env)

> 시작: 교수님 `Chapter15` 상태 / 결과: 교수님 `Chapter16`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`dev-env`: 모든 서비스 + mongodb·mysql·rabbitmq를 쿠버네티스 안에 함께. `prod-env`: DB·브로커는 쿠버네티스 밖에 두고 서비스만, `prod` 프로필.

## 7-1. `kubernetes/helm/environments/dev-env/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/dev-env
code kubernetes/helm/environments/dev-env/Chart.yaml
```
붙여넣기 (새 파일)
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

  - name: config-server
    version: 1.0.0
    repository: file://../../components/config-server

  - name: gateway
    version: 1.0.0
    repository: file://../../components/gateway

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

## 7-2. `kubernetes/helm/environments/dev-env/templates/secrets.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/dev-env/templates
code kubernetes/helm/environments/dev-env/templates/secrets.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.secrets" . -}}
```

## 7-3. `kubernetes/helm/environments/dev-env/values.yaml` (새 파일)
```bash
code kubernetes/helm/environments/dev-env/values.yaml
```
붙여넣기 (새 파일)
```yaml
secrets:

  config-server-secrets:
    ENCRYPT_KEY: my-very-secure-encrypt-key
    SPRING_SECURITY_USER_NAME: dev-usr
    SPRING_SECURITY_USER_PASSWORD: dev-pwd

  config-client-credentials:
    CONFIG_SERVER_USR: dev-usr
    CONFIG_SERVER_PWD: dev-pwd
    SPRING_CLOUD_CONFIG_URI: http://config-server:8888

mysql:
  env:
    MYSQL_ROOT_PASSWORD: rootpwd
    MYSQL_DATABASE: review-db
    MYSQL_USER: user
    MYSQL_PASSWORD: pwd

config-server:
  envFromSecretRefs:
    - config-server-secrets

gateway:
  envFromSecretRefs:
    - config-client-credentials

auth-server:
  envFromSecretRefs:
    - config-client-credentials

product:
  envFromSecretRefs:
    - config-client-credentials

recommendation:
  envFromSecretRefs:
    - config-client-credentials

review:
  envFromSecretRefs:
    - config-client-credentials

product-composite:
  envFromSecretRefs:
    - config-client-credentials
```

## 7-4. `kubernetes/helm/environments/prod-env/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/prod-env
code kubernetes/helm/environments/prod-env/Chart.yaml
```
붙여넣기 (새 파일)
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

  - name: config-server
    version: 1.0.0
    repository: file://../../components/config-server

  - name: gateway
    version: 1.0.0
    repository: file://../../components/gateway

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

## 7-5. `kubernetes/helm/environments/prod-env/templates/secrets.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/environments/prod-env/templates
code kubernetes/helm/environments/prod-env/templates/secrets.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.secrets" . -}}
```

## 7-6. `kubernetes/helm/environments/prod-env/values.yaml` (새 파일)
```bash
code kubernetes/helm/environments/prod-env/values.yaml
```
붙여넣기 (새 파일)
```yaml
secrets:

  config-server-secrets:
    ENCRYPT_KEY: my-very-secure-encrypt-key
    SPRING_SECURITY_USER_NAME: prod-usr
    SPRING_SECURITY_USER_PASSWORD: prod-pwd

  config-client-credentials:
    CONFIG_SERVER_USR: prod-usr
    CONFIG_SERVER_PWD: prod-pwd
    SPRING_CLOUD_CONFIG_URI: http://config-server:8888

config-server:
  image:
    tag: v1
  env:
    LOGGING_LEVEL_ROOT: WARN
    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info"
  envFromSecretRefs:
    - config-server-secrets
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

gateway:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - config-client-credentials
  resources:
    requests:
      memory: 200Mi
    limits:
      memory: 400Mi

auth-server:
  image:
    tag: v1
  env:
    SPRING_PROFILES_ACTIVE: "docker,prod"
  envFromSecretRefs:
    - config-client-credentials
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
    - config-client-credentials
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
    - config-client-credentials
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
    - config-client-credentials
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
    - config-client-credentials
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

다음: [08-kubernetes.md](08-kubernetes.md)
