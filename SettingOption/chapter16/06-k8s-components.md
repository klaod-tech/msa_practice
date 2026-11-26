# 06. 헬름 구성 요소 차트 (서비스별)

> 시작: 15장까지 따라 한 상태 / 결과: 16장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

서비스마다 `Chart.yaml`(common을 의존성으로), `values.yaml`(이미지 이름, 포트, 프로브, 환경 변수), `templates/`(common 템플릿 호출 한두 줄). 게이트웨이 서비스는 `NodePort` 30443으로 밖에 연다.

## 6-1. `kubernetes/helm/components/auth-server/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/auth-server
code kubernetes/helm/components/auth-server/.helmignore
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

## 6-2. `kubernetes/helm/components/auth-server/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/auth-server/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Auth Server
name: auth-server
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-3. `kubernetes/helm/components/auth-server/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/auth-server/templates
code kubernetes/helm/components/auth-server/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-4. `kubernetes/helm/components/auth-server/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/auth-server/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-5. `kubernetes/helm/components/auth-server/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/auth-server/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: auth-server

image:
  name: auth-server

env:
  SPRING_PROFILES_ACTIVE: "docker"

livenessProbe_enabled: true

readinessProbe_enabled: true
```

## 6-6. `kubernetes/helm/components/config-server/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/config-server
code kubernetes/helm/components/config-server/.helmignore
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

## 6-7. `kubernetes/helm/components/config-server/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/config-server/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Config Server
name: config-server
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-8. `kubernetes/helm/components/config-server/config-repo/application.yml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/config-server/config-repo
code kubernetes/helm/components/config-server/config-repo/application.yml
```
붙여넣기 (새 파일)
```yaml
app:
  auth-server: localhost

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: '{cipher}17fcf0ae5b8c5cf87de6875b699be4a1746dd493a99d926c7a26a68c422117ef'

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

## 6-9. `kubernetes/helm/components/config-server/config-repo/auth-server.yml` (새 파일)
```bash
code kubernetes/helm/components/config-server/config-repo/auth-server.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 9999
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

## 6-10. `kubernetes/helm/components/config-server/config-repo/gateway.yml` (새 파일)
```bash
code kubernetes/helm/components/config-server/config-repo/gateway.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 8443

spring.cloud.gateway.server.webflux.trusted-proxies: ".*"

spring.ssl.bundle.jks.gateway:
  key:
    alias: localhost
  keystore:
    type: PKCS12
    location: classpath:keystore/edge.p12
    password: '{cipher}d59274473b196cebe288f849c442eba34aa497d14a13e10a23f824489e1c23ec'

server.ssl.bundle: gateway

spring.cloud.gateway.server.webflux.routes:

- id: product-composite
  uri: http://product-composite
  predicates:
  - Path=/product-composite/**

- id: oauth2-server
  uri: http://${app.auth-server}
  predicates:
  - Path=/oauth2/**

- id: oauth2-login
  uri: http://${app.auth-server}
  predicates:
  - Path=/login/**

- id: oauth2-error
  uri: http://${app.auth-server}
  predicates:
  - Path=/error/**

- id: product-composite-swagger-ui
  uri: http://product-composite
  predicates:
  - Path=/openapi/**

- id: config-server
  uri: ${spring.cloud.config.uri}
  predicates:
  - Path=/config/**
  filters:
  - RewritePath=/config/(?<segment>.*), /$\{segment}

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}

management.endpoint.gateway.access: unrestricted

logging:
  level:
    root: INFO
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: INFO
    org.springframework.cloud.gateway: TRACE
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: TRACE

---
spring.config.activate.on-profile: prod

logging:
  level:
    root: WARN
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: WARN
    org.springframework.cloud.gateway: WARN
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: WARN
```

## 6-11. `kubernetes/helm/components/config-server/config-repo/product-composite.yml` (새 파일)
```bash
code kubernetes/helm/components/config-server/config-repo/product-composite.yml
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
      clientSecret: '{cipher}debb77857798ba70441b229de88624213b0a00b284f95fe31d1378d117cf3ffa'
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
spring.jackson.mapper.allow-final-fields-as-mutators: true

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

## 6-12. `kubernetes/helm/components/config-server/config-repo/product.yml` (새 파일)
```bash
code kubernetes/helm/components/config-server/config-repo/product.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true

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

## 6-13. `kubernetes/helm/components/config-server/config-repo/recommendation.yml` (새 파일)
```bash
code kubernetes/helm/components/config-server/config-repo/recommendation.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true

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

## 6-14. `kubernetes/helm/components/config-server/config-repo/review.yml` (새 파일)
```bash
code kubernetes/helm/components/config-server/config-repo/review.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true

spring.application.name: review

management.endpoint.health.group.readiness.include: readinessState, rabbit, db

spring.jpa.hibernate.ddl-auto: update

spring.datasource:
  url: jdbc:mysql://localhost/review-db
  username: user
  password: '{cipher}24b93c790fa2ed0dc5a6a2b8c39853cf4d6cd6100c248de5f279fb5dd78e7be6'

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

## 6-15. `kubernetes/helm/components/config-server/templates/configmap_from_file.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/config-server/templates
code kubernetes/helm/components/config-server/templates/configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.configmap_from_file" . -}}
```

## 6-16. `kubernetes/helm/components/config-server/templates/deployment.yaml` (새 파일)
```bash
code kubernetes/helm/components/config-server/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-17. `kubernetes/helm/components/config-server/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/config-server/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-18. `kubernetes/helm/components/config-server/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/config-server/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: config-server

image:
  name: config-server

ports:
  - name: http
    containerPort: 8888
    protocol: TCP

env:
  SPRING_PROFILES_ACTIVE: "docker,native"

livenessProbe_enabled: true
livenessProbe:
  httpGet:
    port: 8888
readinessProbe_enabled: true
readinessProbe:
  httpGet:
    port: 8888

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo

service:
  ports:
  - port: 8888
    targetPort: http
    protocol: TCP
    name: http
```

## 6-19. `kubernetes/helm/components/gateway/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/gateway
code kubernetes/helm/components/gateway/.helmignore
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

## 6-20. `kubernetes/helm/components/gateway/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/gateway/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Spring Cloud Gateway
name: gateway
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-21. `kubernetes/helm/components/gateway/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/gateway/templates
code kubernetes/helm/components/gateway/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-22. `kubernetes/helm/components/gateway/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/gateway/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-23. `kubernetes/helm/components/gateway/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/gateway/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: gateway

image:
  name: gateway

ports:
  - containerPort: 8443

env:
  SPRING_PROFILES_ACTIVE: "docker"

livenessProbe_enabled: true
livenessProbe:
  httpGet:
    scheme: HTTPS
    port: 8443
readinessProbe_enabled: true
readinessProbe:
  httpGet:
    scheme: HTTPS
    port: 8443

service:
  type: NodePort
  ports:
  - port: 443
    targetPort: 8443
    nodePort: 30443
```

## 6-24. `kubernetes/helm/components/mongodb/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/mongodb
code kubernetes/helm/components/mongodb/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for MongoDB
name: mongodb
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-25. `kubernetes/helm/components/mongodb/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/mongodb/templates
code kubernetes/helm/components/mongodb/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-26. `kubernetes/helm/components/mongodb/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/mongodb/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-27. `kubernetes/helm/components/mongodb/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/mongodb/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: mongodb

image:
  repository: registry.hub.docker.com/library
  name: mongo
  tag: 8.0.5
  pullPolicy: IfNotPresent

ports:
  - containerPort: 27017

service:
  ports:
    - port: 27017
```

## 6-28. `kubernetes/helm/components/mysql/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/mysql
code kubernetes/helm/components/mysql/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for MySQL
name: mysql
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-29. `kubernetes/helm/components/mysql/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/mysql/templates
code kubernetes/helm/components/mysql/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-30. `kubernetes/helm/components/mysql/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/mysql/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-31. `kubernetes/helm/components/mysql/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/mysql/values.yaml
```
붙여넣기 (새 파일)
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

## 6-32. `kubernetes/helm/components/product-composite/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product-composite
code kubernetes/helm/components/product-composite/.helmignore
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

## 6-33. `kubernetes/helm/components/product-composite/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-composite/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Product Composite microservice
name: product-composite
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-34. `kubernetes/helm/components/product-composite/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product-composite/templates
code kubernetes/helm/components/product-composite/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-35. `kubernetes/helm/components/product-composite/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-composite/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-36. `kubernetes/helm/components/product-composite/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/product-composite/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: product-composite

image:
  name: product-composite-service

env:
  SPRING_PROFILES_ACTIVE: "docker"

livenessProbe_enabled: true

readinessProbe_enabled: true
```

## 6-37. `kubernetes/helm/components/product/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product
code kubernetes/helm/components/product/.helmignore
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

## 6-38. `kubernetes/helm/components/product/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/product/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Product microservice
name: product
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-39. `kubernetes/helm/components/product/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/product/templates
code kubernetes/helm/components/product/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-40. `kubernetes/helm/components/product/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/product/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-41. `kubernetes/helm/components/product/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/product/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: product

image:
  name: product-service

env:
  SPRING_PROFILES_ACTIVE: "docker"

livenessProbe_enabled: true

readinessProbe_enabled: true
```

## 6-42. `kubernetes/helm/components/rabbitmq/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/rabbitmq
code kubernetes/helm/components/rabbitmq/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for RabbitMQ
name: rabbitmq
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-43. `kubernetes/helm/components/rabbitmq/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/rabbitmq/templates
code kubernetes/helm/components/rabbitmq/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-44. `kubernetes/helm/components/rabbitmq/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/rabbitmq/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-45. `kubernetes/helm/components/rabbitmq/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/rabbitmq/values.yaml
```
붙여넣기 (새 파일)
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
        value: Basic Z3Vlc3Q6Z3Vlc3Q=
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

## 6-46. `kubernetes/helm/components/recommendation/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/recommendation
code kubernetes/helm/components/recommendation/.helmignore
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

## 6-47. `kubernetes/helm/components/recommendation/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Recommendation microservice
name: recommendation
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-48. `kubernetes/helm/components/recommendation/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/recommendation/templates
code kubernetes/helm/components/recommendation/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-49. `kubernetes/helm/components/recommendation/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-50. `kubernetes/helm/components/recommendation/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/recommendation/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: recommendation

image:
  name: recommendation-service

env:
  SPRING_PROFILES_ACTIVE: "docker"

livenessProbe_enabled: true

readinessProbe_enabled: true
```

## 6-51. `kubernetes/helm/components/review/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/components/review
code kubernetes/helm/components/review/.helmignore
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

## 6-52. `kubernetes/helm/components/review/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/components/review/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for the Review microservice
name: review
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-53. `kubernetes/helm/components/review/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/review/templates
code kubernetes/helm/components/review/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-54. `kubernetes/helm/components/review/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/review/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-55. `kubernetes/helm/components/review/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/review/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: review

image:
  name: review-service

env:
  SPRING_PROFILES_ACTIVE: "docker"

livenessProbe_enabled: true

readinessProbe_enabled: true
```

## 6-56. `kubernetes/helm/components/zipkin-server/Chart.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/zipkin-server
code kubernetes/helm/components/zipkin-server/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
appVersion: "1.0"
description: A Helm chart for Zipkin
name: zipkin-server
version: 1.0.0

dependencies:
  - name: common
    version: 1.0.0
    repository: file://../../common
```

## 6-57. `kubernetes/helm/components/zipkin-server/templates/deployment.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/components/zipkin-server/templates
code kubernetes/helm/components/zipkin-server/templates/deployment.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.deployment" . -}}
```

## 6-58. `kubernetes/helm/components/zipkin-server/templates/service.yaml` (새 파일)
```bash
code kubernetes/helm/components/zipkin-server/templates/service.yaml
```
붙여넣기 (새 파일)
```yaml
{{- template "common.service" . -}}
```

## 6-59. `kubernetes/helm/components/zipkin-server/values.yaml` (새 파일)
```bash
code kubernetes/helm/components/zipkin-server/values.yaml
```
붙여넣기 (새 파일)
```yaml
fullnameOverride: zipkin-server

image:
  repository: registry.hub.docker.com/openzipkin
  name: zipkin
  tag: 3.5.0
  pullPolicy: IfNotPresent

env:
  STORAGE_TYPE: mem

ports:
  - containerPort: 9411

livenessProbe_enabled: true
livenessProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/info
    port: 9411
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 2
  failureThreshold: 20
  successThreshold: 1

readinessProbe_enabled: true
readinessProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/health
    port: 9411
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 2
  failureThreshold: 3
  successThreshold: 1

resources:
  limits:
    memory: 512Mi

service:
  ports:
    - port: 9411
```

다음: [07-k8s-environments.md](07-k8s-environments.md)
