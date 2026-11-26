# 07. 헬름 환경 차트

> 시작: 16장까지 따라 한 상태 / 결과: 17장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

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

  # ★ 삭제 시작 (6줄) — 아래 줄들을 지운다
  # ★ 지움: - name: config-server
  # ★ 지움: version: 1.0.0
  # ★ 지움: repository: file://../../components/config-server
  # ★ 지움: - name: gateway
  # ★ 지움: version: 1.0.0
  # ★ 지움: repository: file://../../components/gateway
  # ★ 삭제 끝
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
# ★ 추가 시작 (15줄)
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
# ★ 추가 끝

secrets:

  # ★ 변경 시작 (전 4줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  # ★ 전: config-server-secrets:
  # ★ 전: ENCRYPT_KEY: my-very-secure-encrypt-key
  # ★ 전: SPRING_SECURITY_USER_NAME: dev-usr
  # ★ 전: SPRING_SECURITY_USER_PASSWORD: dev-pwd
  rabbitmq-server-credentials:
    RABBITMQ_DEFAULT_USER: rabbit-user-dev
    RABBITMQ_DEFAULT_PASS: rabbit-pwd-dev
  # ★ 변경 끝

  # ★ 변경 시작 (전 4줄 → 24줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  # ★ 전: config-client-credentials:
  # ★ 전: CONFIG_SERVER_USR: dev-usr
  # ★ 전: CONFIG_SERVER_PWD: dev-pwd
  # ★ 전: SPRING_CLOUD_CONFIG_URI: http://config-server:8888
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
  # ★ 변경 끝

mysql:
  # ★ 삭제 시작 (6줄) — 아래 줄들을 지운다
  # ★ 지움: env:
  # ★ 지움: MYSQL_ROOT_PASSWORD: rootpwd
  # ★ 지움: MYSQL_DATABASE: review-db
  # ★ 지움: MYSQL_USER: user
  # ★ 지움: MYSQL_PASSWORD: pwd
  # ★ 지움: config-server:
  # ★ 삭제 끝
  envFromSecretRefs:
    # ★ 변경 시작 (전 4줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-server-secrets
    # ★ 전: gateway:
    # ★ 전: envFromSecretRefs:
    # ★ 전: - config-client-credentials
    - mysql-server-credentials
    # ★ 변경 끝

auth-server:
  envFromSecretRefs:
    - rabbitmq-credentials  # ★ 변경 (전: - config-client-credentials)

product:
  envFromSecretRefs:
    # ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-client-credentials
    - rabbitmq-credentials
    - mongodb-credentials
    # ★ 변경 끝

recommendation:
  envFromSecretRefs:
    # ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-client-credentials
    - rabbitmq-credentials
    - mongodb-credentials
    # ★ 변경 끝

review:
  envFromSecretRefs:
    # ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-client-credentials
    - rabbitmq-credentials
    - mysql-credentials
    # ★ 변경 끝

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

  # ★ 삭제 시작 (6줄) — 아래 줄들을 지운다
  # ★ 지움: - name: config-server
  # ★ 지움: version: 1.0.0
  # ★ 지움: repository: file://../../components/config-server
  # ★ 지움: - name: gateway
  # ★ 지움: version: 1.0.0
  # ★ 지움: repository: file://../../components/gateway
  # ★ 삭제 끝
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
# ★ 추가 시작 (15줄)
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
# ★ 추가 끝

secrets:

  # ★ 변경 시작 (전 4줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  # ★ 전: config-server-secrets:
  # ★ 전: ENCRYPT_KEY: my-very-secure-encrypt-key
  # ★ 전: SPRING_SECURITY_USER_NAME: prod-usr
  # ★ 전: SPRING_SECURITY_USER_PASSWORD: prod-pwd
  rabbitmq-credentials:
    SPRING_RABBITMQ_USERNAME: rabbit-user-prod
    SPRING_RABBITMQ_PASSWORD: rabbit-pwd-prod
  # ★ 변경 끝

  # ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  # ★ 전: config-client-credentials:
  # ★ 전: CONFIG_SERVER_USR: prod-usr
  # ★ 전: CONFIG_SERVER_PWD: prod-pwd
  # ★ 전: SPRING_CLOUD_CONFIG_URI: http://config-server:8888
  mongodb-credentials:
    SPRING_MONGODB_AUTHENTICATION_DATABASE: admin
    SPRING_MONGODB_USERNAME: mongodb-user-prod
    SPRING_MONGODB_PASSWORD: mongodb-pwd-prod
  # ★ 변경 끝

  # ★ 변경 시작 (전 25줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  # ★ 전: config-server:
  # ★ 전: image:
  # ★ 전: tag: v1
  # ★ 전: env:
  # ★ 전: LOGGING_LEVEL_ROOT: WARN
  # ★ 전: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info"
  # ★ 전: envFromSecretRefs:
  # ★ 전: - config-server-secrets
  # ★ 전: resources:
  # ★ 전: requests:
  # ★ 전: memory: 200Mi
  # ★ 전: limits:
  # ★ 전: ... (13줄 더)
  mysql-credentials:
    SPRING_DATASOURCE_USERNAME: mysql-user-prod
    SPRING_DATASOURCE_PASSWORD: mysql-pwd-prod
  # ★ 변경 끝

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
    # ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-client-credentials
    - rabbitmq-credentials
    - mongodb-credentials
    # ★ 변경 끝
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
    # ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-client-credentials
    - rabbitmq-credentials
    - mongodb-credentials
    # ★ 변경 끝
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
    # ★ 변경 시작 (전 1줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: - config-client-credentials
    - rabbitmq-credentials
    - mysql-credentials
    # ★ 변경 끝
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
