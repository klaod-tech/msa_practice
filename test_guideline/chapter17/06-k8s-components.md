# 06. 헬름 구성 요소 차트

> 시작: 교수님 `Chapter16` 상태 / 결과: 교수님 `Chapter17`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

config-server, gateway 차트 삭제. 서비스 차트마다 `config-repo` 파일을 ConfigMap으로 넣고 `SPRING_CONFIG_LOCATION`으로 가리킨다.

## 6-1. `kubernetes/helm/components/auth-server/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/auth-server/config-repo
code kubernetes/helm/components/auth-server/config-repo/application.yml
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

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://zipkin-server:9411/api/v2/spans
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

## 6-2. `kubernetes/helm/components/auth-server/config-repo/auth-server.yml` (새 파일)
```bash
code kubernetes/helm/components/auth-server/config-repo/auth-server.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 9999
spring.application.name: auth-server
server.forward-headers-strategy: framework

logging:
  level:
    root: INFO

---
spring.config.activate.on-profile: docker

server.port: 80

---
spring.config.activate.on-profile: prod

logging.level.root: WARN
```

## 6-3. `kubernetes/helm/components/auth-server/templates/configmap_from_file.yaml` (새 파일)
```bash
code kubernetes/helm/components/auth-server/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 6-4. `kubernetes/helm/components/auth-server/values.yaml`
```bash
code kubernetes/helm/components/auth-server/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: auth-server

image:
  name: auth-server

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/auth-server.yml  # ★ 추가

livenessProbe_enabled: true

readinessProbe_enabled: true
# ★ 추가 (4줄)

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 6-5. `kubernetes/helm/components/mysql/values.yaml`
```bash
code kubernetes/helm/components/mysql/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: mysql

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
    - port: 3306
```

## 6-6. `kubernetes/helm/components/product-composite/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product-composite/config-repo
code kubernetes/helm/components/product-composite/config-repo/application.yml
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

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://zipkin-server:9411/api/v2/spans
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

## 6-7. `kubernetes/helm/components/product-composite/config-repo/product-composite.yml` (새 파일)
```bash
code kubernetes/helm/components/product-composite/config-repo/product-composite.yml
```
붙여넣기 (새 파일)
```yaml
springdoc:
  swagger-ui.path: /openapi/swagger-ui.html
  api-docs.path: /openapi/v3/api-docs
  packagesToScan: se.magnus.microservices.composite.product
  pathsToMatch: /**
  cache.disabled: true

  swagger-ui:
    oauth2-redirect-url: /swagger-ui/oauth2-redirect.html
    oauth:
      clientId: writer
      clientSecret: secret-writer
      useBasicAuthenticationWithAccessCodeGrant: true
  oAuthFlow:
    authorizationUrl: https://localhost:8443/oauth2/authorize
    tokenUrl: https://localhost:8443/oauth2/token

server.forward-headers-strategy: framework

api:

  common:
    version: 1.0.0
    title: Sample API
    description: Description of the API...
    termsOfService: MY TERMS OF SERVICE
    license: MY LICENSE
    licenseUrl: MY LICENSE URL

    externalDocDesc: MY WIKI PAGE
    externalDocUrl: MY WIKI URL
    contact:
      name: NAME OF CONTACT
      url: URL TO CONTACT
      email: contact@mail.com

  responseCodes:
    ok.description: OK
    badRequest.description: Bad Request, invalid format of the request. See response message for more information
    notFound.description: Not found, the specified id does not exist
    unprocessableEntity.description: Unprocessable entity, input parameters caused the processing to fail. See response message for more information

  product-composite:

    get-composite-product:
      description: Returns a composite view of the specified product id
      notes: |
        # Normal response
        If the requested product id is found the method will return information regarding:
        1. Product information
        1. Reviews
        1. Recommendations
        1. Service Addresses\n(technical information regarding the addresses of the microservices that created the response)

        # Expected partial and error responses
        1. If no product information is found, a **404 - Not Found** error will be returned
        1. In no recommendations or reviews are found for a product, a partial response will be returned

    create-composite-product:
      description: Creates a composite product
      notes: |
        # Normal response
        The composite product information posted to the API will be split up and stored as separate product-info, recommendation and review entities.

        # Expected error responses
        1. If a product with the same productId as specified in the posted information already exists,
           an **422 - Unprocessable Entity** error with a "duplicate key" error message will be returned

    delete-composite-product:
      description: Deletes a product composite
      notes: |
        # Normal response
        Entities for product information, recommendations and reviews related to the specified productId will be deleted.
        The implementation of the delete method is idempotent, i.e. it can be called several times with the same response.

        This means that a delete request of a non-existing product will return **200 Ok**.

server.port: 7001
spring.web.error.include-message: always

spring.application.name: product-composite

management.endpoint.health.group.readiness.include: readinessState, rabbit

spring.cloud.stream.output-bindings: products-out-0;recommendations-out-0;reviews-out-0

spring.cloud.stream:
  default.contentType: application/json
  bindings:
    products-out-0:
      destination: products
      producer:
        required-groups: auditGroup
    recommendations-out-0:
      destination: recommendations
      producer:
        required-groups: auditGroup
    reviews-out-0:
      destination: reviews
      producer:
        required-groups: auditGroup

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}

resilience4j.timelimiter:
  instances:
    product:
      timeoutDuration: 2s

resilience4j.retry:
  instances:
    product:
      maxAttempts: 3
      waitDuration: 1000
      retryExceptions:
      - org.springframework.web.reactive.function.client.WebClientResponseException$InternalServerError

management.health.circuitbreakers.enabled: true

spring.jackson.datatype.enum.write-enums-using-to-string: false

resilience4j.circuitbreaker:
  instances:
    product:
      allowHealthIndicatorToFail: false
      registerHealthIndicator: true
      slidingWindowType: COUNT_BASED
      slidingWindowSize: 5
      failureRateThreshold: 50
      waitDurationInOpenState: 10000
      permittedNumberOfCallsInHalfOpenState: 3
      automaticTransitionFromOpenToHalfOpenEnabled: true
      ignoreExceptions:
        - se.magnus.api.exceptions.InvalidInputException
        - se.magnus.api.exceptions.NotFoundException

logging:
  level:
    root: INFO
    se.magnus: INFO
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: INFO

---
spring.config.activate.on-profile: docker

server.port: 80

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.products-out-0.producer:
  partition-key-expression: headers['partitionKey']
  partition-count: 2

spring.cloud.stream.bindings.recommendations-out-0.producer:
  partition-key-expression: headers['partitionKey']
  partition-count: 2

spring.cloud.stream.bindings.reviews-out-0.producer:
  partition-key-expression: headers['partitionKey']
  partition-count: 2

---
spring.config.activate.on-profile: kafka

management.endpoint.health.group.readiness.include: readinessState

---
spring.config.activate.on-profile: prod

logging:
  level:
    root: WARN
    se.magnus: WARN
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: WARN
```

## 6-8. `kubernetes/helm/components/product-composite/templates/configmap_from_file.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-composite/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 6-9. `kubernetes/helm/components/product-composite/values.yaml`
```bash
code kubernetes/helm/components/product-composite/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: product-composite

image:
  name: product-composite-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/product-composite.yml  # ★ 추가

livenessProbe_enabled: true

readinessProbe_enabled: true
# ★ 추가 (4줄)

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 6-10. `kubernetes/helm/components/product/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product/config-repo
code kubernetes/helm/components/product/config-repo/application.yml
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

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://zipkin-server:9411/api/v2/spans
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

## 6-11. `kubernetes/helm/components/product/config-repo/product.yml` (새 파일)
```bash
code kubernetes/helm/components/product/config-repo/product.yml
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

## 6-12. `kubernetes/helm/components/product/templates/configmap_from_file.yaml` (새 파일)
```bash
code kubernetes/helm/components/product/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 6-13. `kubernetes/helm/components/product/values.yaml`
```bash
code kubernetes/helm/components/product/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: product

image:
  name: product-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/product.yml  # ★ 추가

livenessProbe_enabled: true

readinessProbe_enabled: true
# ★ 추가 (4줄)

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 6-14. `kubernetes/helm/components/rabbitmq/values.yaml`
```bash
code kubernetes/helm/components/rabbitmq/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: rabbitmq

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
        value: Basic cmFiYml0LXVzZXItZGV2OnJhYmJpdC1wd2QtZGV2  # ★ 변경 (전: value: Basic Z3Vlc3Q6Z3Vlc3Q=)
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

## 6-15. `kubernetes/helm/components/recommendation/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/recommendation/config-repo
code kubernetes/helm/components/recommendation/config-repo/application.yml
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

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://zipkin-server:9411/api/v2/spans
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

## 6-16. `kubernetes/helm/components/recommendation/config-repo/recommendation.yml` (새 파일)
```bash
code kubernetes/helm/components/recommendation/config-repo/recommendation.yml
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

## 6-17. `kubernetes/helm/components/recommendation/templates/configmap_from_file.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 6-18. `kubernetes/helm/components/recommendation/values.yaml`
```bash
code kubernetes/helm/components/recommendation/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: recommendation

image:
  name: recommendation-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/recommendation.yml  # ★ 추가

livenessProbe_enabled: true

readinessProbe_enabled: true
# ★ 추가 (4줄)

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 6-19. `kubernetes/helm/components/review/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/review/config-repo
code kubernetes/helm/components/review/config-repo/application.yml
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

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false

management.tracing.export.zipkin.endpoint: http://zipkin-server:9411/api/v2/spans
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

## 6-20. `kubernetes/helm/components/review/config-repo/review.yml` (새 파일)
```bash
code kubernetes/helm/components/review/config-repo/review.yml
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

## 6-21. `kubernetes/helm/components/review/templates/configmap_from_file.yaml` (새 파일)
```bash
code kubernetes/helm/components/review/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 6-22. `kubernetes/helm/components/review/values.yaml`
```bash
code kubernetes/helm/components/review/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: review

image:
  name: review-service

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/review.yml  # ★ 추가

livenessProbe_enabled: true

readinessProbe_enabled: true
# ★ 추가 (4줄)

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 6-23. 지울 파일
```bash
cd ~/msa
rm -rf kubernetes/helm/components/config-server
rm -rf kubernetes/helm/components/gateway
```

`rm -rf 폴더`는 이 장에서 폴더째 없어지는 것이다. 빌드나 `helm dep up`이 만든 파일(`build/`, `Chart.lock`, `charts/`)까지 함께 지운다.

다음: [07-k8s-environments.md](07-k8s-environments.md)
