# 04. config-repo: Eureka 설정 제거, 프로브, prod 프로필

> 시작: 15장까지 따라 한 상태 / 결과: 16장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

- `application.yml`: Eureka 설정 삭제. 프로브(`management.endpoint.health.probes.enabled`), `server.shutdown: graceful`, 종료 대기 10초. `prod` 프로필(쿠버네티스 밖의 RabbitMQ·DB 주소, 로그 줄이기).
- `gateway.yml`: 라우트 대상이 `lb://...` → `http://...`. Eureka와 헤더 라우팅 예제 라우트 삭제.
- 각 서비스 yml: docker 프로필 포트 80 등.
- `eureka-server.yml` 삭제.

## 4-1. `config-repo/application.yml`
```bash
code config-repo/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
app:
  # ★ 삭제 시작 (3줄) — 아래 줄들을 지운다
  # ★ 지움: eureka-username: u
  # ★ 지움: eureka-password: '{cipher}bf298f6d5f878b342f9e44bec08cb9ac00b4ce57e98316f030194a225fac89fb'
  # ★ 지움: eureka-server: localhost
  # ★ 삭제 끝
  auth-server: localhost

# ★ 삭제 시작 (9줄) — 아래 줄들을 지운다
# ★ 지움: eureka:
# ★ 지움: client:
# ★ 지움: serviceUrl:
# ★ 지움: defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"
# ★ 지움: initialInstanceInfoReplicationIntervalSeconds: 5
# ★ 지움: registryFetchIntervalSeconds: 5
# ★ 지움: instance:
# ★ 지움: leaseRenewalIntervalInSeconds: 5
# ★ 지움: leaseExpirationDurationInSeconds: 5
# ★ 삭제 끝
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
# ★ 추가 시작 (3줄)
management.endpoint.health.probes.enabled: true
management.health.rabbit.enabled: true
management.health.kafka.enabled: false
# ★ 추가 끝

management.tracing.export.zipkin.endpoint: http://zipkin-server:9411/api/v2/spans
management.tracing.sampling.probability: 1.0

# ★ 추가 시작 (2줄)
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 10s
# ★ 추가 끝

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  # ★ 삭제: eureka-server: eureka
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

# ★ 삭제: management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1

# ★ 추가 시작 (6줄)
management.health.rabbit.enabled: false
management.health.kafka.enabled: true

---
spring.config.activate.on-profile: prod
spring.rabbitmq.host: 172.17.0.1

management.endpoints.web.exposure.include: health,info,circuitbreakerevents
# ★ 추가 끝
```

## 4-2. `config-repo/auth-server.yml`
```bash
code config-repo/auth-server.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 9999
server.forward-headers-strategy: framework

logging:
  level:
    root: INFO

# ★ 추가 시작 (6줄)
---
spring.config.activate.on-profile: docker

server.port: 80

---
spring.config.activate.on-profile: prod

logging.level.root: WARN
# ★ 추가 끝
```

## 4-3. `config-repo/gateway.yml`
```bash
code config-repo/gateway.yml
```
아래처럼 맞추기 (전체 파일)
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
  uri: http://product-composite  # ★ 변경 (전: uri: lb://product-composite)
  predicates:
  - Path=/product-composite/**

- id: oauth2-server
  uri: http://${app.auth-server}  # ★ 변경 (전: uri: lb://auth-server)
  predicates:
  - Path=/oauth2/**

- id: oauth2-login
  uri: http://${app.auth-server}  # ★ 변경 (전: uri: lb://auth-server)
  predicates:
  - Path=/login/**

- id: oauth2-error
  uri: http://${app.auth-server}  # ★ 변경 (전: uri: lb://auth-server)
  predicates:
  - Path=/error/**

- id: product-composite-swagger-ui
  uri: http://product-composite  # ★ 변경 (전: uri: lb://product-composite)
  predicates:
  - Path=/openapi/**

# ★ 삭제 시작 (16줄) — 아래 줄들을 지운다
# ★ 지움: - id: eureka-api
# ★ 지움: uri: http://${app.eureka-server}:8761
# ★ 지움: predicates:
# ★ 지움: - Path=/eureka/api/{segment}
# ★ 지움: filters:
# ★ 지움: - SetPath=/eureka/{segment}
# ★ 지움: - id: eureka-web-start
# ★ 지움: uri: http://${app.eureka-server}:8761
# ★ 지움: predicates:
# ★ 지움: - Path=/eureka/web
# ★ 지움: filters:
# ★ 지움: - SetPath=/
# ★ 지움: ... (4줄 더)
# ★ 삭제 끝
- id: config-server
  uri: ${spring.cloud.config.uri}
  predicates:
  - Path=/config/**
  filters:
  - RewritePath=/config/(?<segment>.*), /$\{segment}

# ★ 변경 시작 (전 21줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: - id: host_route_200
# ★ 전: uri: http://httpstat.us
# ★ 전: predicates:
# ★ 전: - Host=i.feel.lucky:8080
# ★ 전: - Path=/headerrouting/**
# ★ 전: filters:
# ★ 전: - SetPath=/200
# ★ 전: - id: host_route_418
# ★ 전: uri: http://httpstat.us
# ★ 전: predicates:
# ★ 전: - Host=im.a.teapot:8080
# ★ 전: - Path=/headerrouting/**
# ★ 전: ... (9줄 더)
spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}
# ★ 변경 끝

management.endpoint.gateway.access: unrestricted

logging:
  level:
    root: INFO
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: INFO
    org.springframework.cloud.gateway: TRACE
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: TRACE

# ★ 추가 시작 (8줄)
---
spring.config.activate.on-profile: prod

logging:
  level:
    root: WARN
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: WARN
    org.springframework.cloud.gateway: WARN
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: WARN
# ★ 추가 끝
```

## 4-4. `config-repo/product-composite.yml`
```bash
code config-repo/product-composite.yml
```
아래처럼 맞추기 (전체 파일)
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

# ★ 추가 시작 (2줄)
spring.application.name: product-composite

management.endpoint.health.group.readiness.include: readinessState, rabbit
# ★ 추가 끝

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

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}  # ★ 변경 (전: spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app....)

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

server.port: 80  # ★ 변경 (전: server.port: 8080)

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

# ★ 추가 시작 (10줄)
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
# ★ 추가 끝
```

## 4-5. `config-repo/product.yml`
```bash
code config-repo/product.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true

# ★ 추가 시작 (2줄)
spring.application.name: product

management.endpoint.health.group.readiness.include: readinessState, rabbit, mongo
# ★ 추가 끝

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

server.port: 80  # ★ 변경 (전: server.port: 8080)

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

# ★ 추가 시작 (11줄)
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
# ★ 추가 끝
```

## 4-6. `config-repo/recommendation.yml`
```bash
code config-repo/recommendation.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true

# ★ 추가 시작 (2줄)
spring.application.name: recommendation

management.endpoint.health.group.readiness.include: readinessState, rabbit, mongo
# ★ 추가 끝

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

server.port: 80  # ★ 변경 (전: server.port: 8080)

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

# ★ 추가 시작 (11줄)
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
# ★ 추가 끝
```

## 4-7. `config-repo/review.yml`
```bash
code config-repo/review.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always
spring.jackson.mapper.allow-final-fields-as-mutators: true

# ★ 추가 시작 (2줄)
spring.application.name: review

management.endpoint.health.group.readiness.include: readinessState, rabbit, db
# ★ 추가 끝

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

server.port: 80  # ★ 변경 (전: server.port: 8080)

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

# ★ 추가 시작 (12줄)
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
# ★ 추가 끝
```

## 4-8. 지울 파일
```bash
cd ~/msa
rm config-repo/eureka-server.yml
```

다음: [05-k8s-common.md](05-k8s-common.md)
