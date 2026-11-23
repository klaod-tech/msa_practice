# 07. 헬름 구성 요소 차트 (green 포함)

> 시작: 교수님 `Chapter17` 상태 / 결과: 교수님 `Chapter18`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

Zipkin 서버 차트 삭제(Jaeger를 쓴다). `product-green`, `recommendation-green`, `review-green`(새 버전 배포 시험용) 추가.

## 7-1. `kubernetes/helm/components/auth-server/config-repo/application.yml`
```bash
code kubernetes/helm/components/auth-server/config-repo/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004  # ★ 추가
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans  # ★ 변경 (전: management.tracing.export.zipkin.endpoint: http://zipkin-server:941...)
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-2. `kubernetes/helm/components/mongodb/values.yaml`
```bash
code kubernetes/helm/components/mongodb/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: mongodb

annotations:  # ★ 추가
  sidecar.istio.io/inject: "false"  # ★ 추가

image:
  repository: registry.hub.docker.com/library
  name: mongo
  tag: 8.0.5
  pullPolicy: IfNotPresent

ports:
  - containerPort: 27017

service:
  ports:
    - name: mongo  # ★ 변경 (전: - port: 27017)
      port: 27017  # ★ 추가
```

## 7-3. `kubernetes/helm/components/mysql/values.yaml`
```bash
code kubernetes/helm/components/mysql/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: mysql

annotations:  # ★ 추가
  sidecar.istio.io/inject: "false"  # ★ 추가

image:
  repository: registry.hub.docker.com/library
  name: mysql
  tag: 9.2.0
  pullPolicy: IfNotPresent

ports:
  - containerPort: 3306

resources:
  limits:
    memory: 512Mi

service:
  ports:
    - name: mysql  # ★ 변경 (전: - port: 3306)
      port: 3306  # ★ 추가
```

## 7-4. `kubernetes/helm/components/product-composite/config-repo/application.yml`
```bash
code kubernetes/helm/components/product-composite/config-repo/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004  # ★ 추가
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans  # ★ 변경 (전: management.tracing.export.zipkin.endpoint: http://zipkin-server:941...)
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-5. `kubernetes/helm/components/product-composite/values.yaml`
```bash
code kubernetes/helm/components/product-composite/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: product-composite

image:
  name: product-composite-service
# ★ 추가 (7줄)

ports:
  - name: http
    containerPort: 80
    protocol: TCP
  - name: http-mgm
    containerPort: 4004
    protocol: TCP

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/product-composite.yml

livenessProbe_enabled: true

readinessProbe_enabled: true

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
# ★ 추가 (8줄)

service:
  ports:
    - port: 80
      name: http
      targetPort: 80
    - port: 4004
      name: http-mgm
      targetPort: 4004
```

## 7-6. `kubernetes/helm/components/product-green/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product-green
code kubernetes/helm/components/product-green/.helmignore
```
붙여넣기 (새 파일)
```
# Patterns to ignore when building packages.
# This supports shell glob matching, relative path matching, and
# negation (prefixed with !). Only one pattern per line.
.DS_Store
# Common VCS dirs
.git/
.gitignore
.bzr/
.bzrignore
.hg/
.hgignore
.svn/
# Common backup files
*.swp
*.bak
*.tmp
*~
# Various IDEs
.project
.idea/
*.tmproj
```

## 7-7. `kubernetes/helm/components/product-green/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-green/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Product microservice
name: product-green
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 7-8. `kubernetes/helm/components/product-green/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product-green/config-repo
code kubernetes/helm/components/product-green/config-repo/application.yml
```
붙여넣기 (새 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-9. `kubernetes/helm/components/product-green/config-repo/product.yml` (새 파일)
```bash
code kubernetes/helm/components/product-green/config-repo/product.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always

spring.application.name: product

management.endpoint.health.group.readiness.include: readinessState, rabbit, mongo

spring.mongodb:
  host: localhost
  port: 27017
  database: product-db

spring.cloud.function.definition: messageProcessor

spring.cloud.stream:
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: products
    group: productsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

logging:
  level:
    root: INFO
    se.magnus: INFO
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG

---
spring.config.activate.on-profile: docker

server.port: 80

spring.mongodb.host: mongodb

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.endpoint.health.group.readiness.include: readinessState, mongo

---
spring.config.activate.on-profile: prod

spring.mongodb.host: 172.17.0.1

logging:
  level:
    root: WARN
    se.magnus: WARN
    org.springframework.data.mongodb.core.MongoTemplate: WARN
```

## 7-10. `kubernetes/helm/components/product-green/templates/configmap_from_file.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product-green/templates
code kubernetes/helm/components/product-green/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 7-11. `kubernetes/helm/components/product-green/templates/deployment.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-green/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 7-12. `kubernetes/helm/components/product-green/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-green/values.yaml
```
붙여넣기 (새 파일)
```yaml
nameOverride: product
fullnameOverride: product-green

image:
  name: product-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/product.yml

livenessProbe_enabled: true

readinessProbe_enabled: true

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 7-13. `kubernetes/helm/components/product/config-repo/application.yml`
```bash
code kubernetes/helm/components/product/config-repo/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004  # ★ 추가
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans  # ★ 변경 (전: management.tracing.export.zipkin.endpoint: http://zipkin-server:941...)
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-14. `kubernetes/helm/components/rabbitmq/values.yaml`
```bash
code kubernetes/helm/components/rabbitmq/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: rabbitmq

annotations:  # ★ 추가
  sidecar.istio.io/inject: "false"  # ★ 추가

image:
  repository: registry.hub.docker.com/library
  name: rabbitmq
  tag: 4.0.7-management
  pullPolicy: IfNotPresent

ports:
  - containerPort: 5671
  - containerPort: 5672
  - containerPort: 15672

readinessProbe_enabled: true
readinessProbe:
  httpGet:
    scheme: HTTP
    path: "/api/aliveness-test/%2F"
    port: 15672
    httpHeaders:
      - name: Authorization
        value: Basic cmFiYml0LXVzZXItZGV2OnJhYmJpdC1wd2QtZGV2
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 20

service:
  ports:
    - name: tcp-secure
      port: 5671
    - name: tcp-not-secure
      port: 5672
    - name: http-mgm-ui
      port: 15672
```

## 7-15. `kubernetes/helm/components/recommendation-green/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/recommendation-green
code kubernetes/helm/components/recommendation-green/.helmignore
```
붙여넣기 (새 파일)
```
# Patterns to ignore when building packages.
# This supports shell glob matching, relative path matching, and
# negation (prefixed with !). Only one pattern per line.
.DS_Store
# Common VCS dirs
.git/
.gitignore
.bzr/
.bzrignore
.hg/
.hgignore
.svn/
# Common backup files
*.swp
*.bak
*.tmp
*~
# Various IDEs
.project
.idea/
*.tmproj
```

## 7-16. `kubernetes/helm/components/recommendation-green/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation-green/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Recommendation microservice
name: recommendation-green
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 7-17. `kubernetes/helm/components/recommendation-green/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/recommendation-green/config-repo
code kubernetes/helm/components/recommendation-green/config-repo/application.yml
```
붙여넣기 (새 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-18. `kubernetes/helm/components/recommendation-green/config-repo/recommendation.yml` (새 파일)
```bash
code kubernetes/helm/components/recommendation-green/config-repo/recommendation.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always

spring.application.name: recommendation

management.endpoint.health.group.readiness.include: readinessState, rabbit, mongo

spring.mongodb:
  host: localhost
  port: 27017
  database: recommendation-db

spring.cloud.function.definition: messageProcessor

spring.cloud.stream:
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: recommendations
    group: recommendationsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

logging:
  level:
    root: INFO
    se.magnus: INFO
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG

---
spring.config.activate.on-profile: docker

server.port: 80

spring.mongodb.host: mongodb

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.endpoint.health.group.readiness.include: readinessState, mongo

---
spring.config.activate.on-profile: prod

spring.mongodb.host: 172.17.0.1

logging:
  level:
    root: WARN
    se.magnus: WARN
    org.springframework.data.mongodb.core.MongoTemplate: WARN
```

## 7-19. `kubernetes/helm/components/recommendation-green/templates/configmap_from_file.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/recommendation-green/templates
code kubernetes/helm/components/recommendation-green/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 7-20. `kubernetes/helm/components/recommendation-green/templates/deployment.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation-green/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 7-21. `kubernetes/helm/components/recommendation-green/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation-green/values.yaml
```
붙여넣기 (새 파일)
```yaml
nameOverride: recommendation
fullnameOverride: recommendation-green

image:
  name: recommendation-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/recommendation.yml

livenessProbe_enabled: true

readinessProbe_enabled: true

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 7-22. `kubernetes/helm/components/recommendation/config-repo/application.yml`
```bash
code kubernetes/helm/components/recommendation/config-repo/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004  # ★ 추가
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans  # ★ 변경 (전: management.tracing.export.zipkin.endpoint: http://zipkin-server:941...)
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-23. `kubernetes/helm/components/review-green/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/review-green
code kubernetes/helm/components/review-green/.helmignore
```
붙여넣기 (새 파일)
```
# Patterns to ignore when building packages.
# This supports shell glob matching, relative path matching, and
# negation (prefixed with !). Only one pattern per line.
.DS_Store
# Common VCS dirs
.git/
.gitignore
.bzr/
.bzrignore
.hg/
.hgignore
.svn/
# Common backup files
*.swp
*.bak
*.tmp
*~
# Various IDEs
.project
.idea/
*.tmproj
```

## 7-24. `kubernetes/helm/components/review-green/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/review-green/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Review microservice
name: review-green
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 7-25. `kubernetes/helm/components/review-green/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/review-green/config-repo
code kubernetes/helm/components/review-green/config-repo/application.yml
```
붙여넣기 (새 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-26. `kubernetes/helm/components/review-green/config-repo/review.yml` (새 파일)
```bash
code kubernetes/helm/components/review-green/config-repo/review.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always

spring.application.name: review

management.endpoint.health.group.readiness.include: readinessState, rabbit, db

spring.jpa.hibernate.ddl-auto: update

spring.datasource:
  url: jdbc:mysql://localhost/review-db

spring.datasource.hikari.initializationFailTimeout: 60000

spring.cloud.function.definition: messageProcessor

spring.cloud.stream:
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: reviews
    group: reviewsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

logging:
  level:
    root: INFO
    se.magnus: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

---
spring.config.activate.on-profile: docker

server.port: 80

spring.datasource.url: jdbc:mysql://mysql/review-db

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.endpoint.health.group.readiness.include: readinessState, db

---
spring.config.activate.on-profile: prod

spring.datasource.url: jdbc:mysql://172.17.0.1:3306/review-db

logging:
  level:
    root: WARN
    se.magnus: WARN
    org.hibernate.SQL: WARN
    org.hibernate.type.descriptor.sql.BasicBinder: WARN
```

## 7-27. `kubernetes/helm/components/review-green/templates/configmap_from_file.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/review-green/templates
code kubernetes/helm/components/review-green/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 7-28. `kubernetes/helm/components/review-green/templates/deployment.yaml` (새 파일)
```bash
code kubernetes/helm/components/review-green/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 7-29. `kubernetes/helm/components/review-green/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/review-green/values.yaml
```
붙여넣기 (새 파일)
```yaml
nameOverride: review
fullnameOverride: review-green

image:
  name: review-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/review.yml

livenessProbe_enabled: true

readinessProbe_enabled: true

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 7-30. `kubernetes/helm/components/review/config-repo/application.yml`
```bash
code kubernetes/helm/components/review/config-repo/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.cloud.stream.defaultBinder: rabbit

management.server.port: 4004  # ★ 추가
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://jaeger-collector.istio-system:9411/api/v2/spans  # ★ 변경 (전: management.tracing.export.zipkin.endpoint: http://zipkin-server:941...)
management.tracing.sampling.probability: 1.0
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
```

## 7-31. 지울 파일
```bash
cd ~/msa
rm -rf kubernetes/helm/components/zipkin-server
```

`rm -rf 폴더`는 이 장에서 폴더째 없어지는 것이다. 빌드나 `helm dep up`이 만든 파일(`build/`, `Chart.lock`, `charts/`)까지 함께 지운다.

다음: [08-k8s-environments.md](08-k8s-environments.md)
