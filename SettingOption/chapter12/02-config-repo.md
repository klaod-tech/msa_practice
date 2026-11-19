# 02. 설정 저장소 만들기 (config-repo)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 11장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Cloud 2025.1 때문에 교재 12장과 다르게 쓰는 부분.
> 교재 "구성 저장소 설정"과 "민감한 정보 암호화" 절. 새 파일만 만든다(★ 없음). 각 서비스의 `application.yml`은 [03](03-config-clients.md)에서 지운다. **지우기 전에 이 문서부터** 한다.

## 무엇을 하나
11장까지 서비스 7개가 각자 들고 있던 `application.yml`을 루트의 `config-repo/` 폴더 한 곳으로 모은다.

| config-repo 파일 | 누가 받나 | 내용 |
|---|---|---|
| `application.yml` | **모든 서비스** | 여러 파일에 똑같이 있던 것: Eureka 주소/계정, RabbitMQ, Kafka, actuator, docker/kafka 프로필 |
| `product.yml`, `recommendation.yml`, `review.yml` | 각 핵심 서비스 | 포트, DB, 메시지 바인딩, 로그 |
| `product-composite.yml` | composite | springdoc, API 설명, 바인딩, JWT 발급자 |
| `gateway.yml` | gateway | HTTPS 인증서, 라우트(★ 설정 서버 라우트 추가), JWT 발급자 |
| `auth-server.yml`, `eureka-server.yml` | 인가 서버, Eureka | 포트 등 |

서비스는 `spring.application.name`과 같은 이름의 파일 + `application.yml`을 **합쳐서** 받는다(이름이 같은 값은 서비스 파일이 이긴다).

비밀번호는 **암호문**(`'{cipher}...'`)으로 적는다. 설정 서버가 `ENCRYPT_KEY`로 풀어서 서비스에 평문으로 건넨다. 파일(GitHub)에는 평문 비밀번호가 남지 않는다.

## 1. .env (새 파일, 루트)
```bash
code .env
```
붙여넣기 (새 파일)
```bash
CONFIG_SERVER_ENCRYPT_KEY=my-very-secure-encrypt-key
CONFIG_SERVER_USR=dev-usr
CONFIG_SERVER_PWD=dev-pwd
```

`docker compose`가 같은 폴더의 `.env`를 자동으로 읽어서 compose 파일의 `${CONFIG_SERVER_USR}` 같은 자리에 넣는다.
- `CONFIG_SERVER_ENCRYPT_KEY`: 암호화/복호화 키. 아래 암호문들은 교재가 이 키로 만든 값이라 **키를 바꾸면 안 풀린다.**
- `CONFIG_SERVER_USR` / `PWD`: 설정 서버 로그인 계정. 다른 서비스도 이 계정으로 설정을 받아 간다.
- 실습용 값이라 교재처럼 git에 같이 올린다(실제 운영이라면 올리지 않는다).

## 2. application.yml (공통, 새 파일)
```bash
mkdir -p config-repo
code config-repo/application.yml
```
붙여넣기 (새 파일)
```yaml
app:
  eureka-username: u
  eureka-password: '{cipher}bf298f6d5f878b342f9e44bec08cb9ac00b4ce57e98316f030194a225fac89fb'
  eureka-server: localhost
  auth-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

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

---
spring.config.activate.on-profile: docker

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

app:
  eureka-server: eureka
  auth-server: auth-server

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

## 3. eureka-server.yml (새 파일)
```bash
code config-repo/eureka-server.yml
```
붙여넣기 (새 파일)
```yaml
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    waitTimeInMsWhenSyncEmpty: 0
    response-cache-update-interval-ms: 5000
```

## 4. auth-server.yml (새 파일)
```bash
code config-repo/auth-server.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 9999
server.forward-headers-strategy: framework

logging:
  level:
    root: INFO
```

## 5. gateway.yml (새 파일)
```bash
code config-repo/gateway.yml
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
  uri: lb://product-composite
  predicates:
  - Path=/product-composite/**

- id: oauth2-server
  uri: lb://auth-server
  predicates:
  - Path=/oauth2/**

- id: oauth2-login
  uri: lb://auth-server
  predicates:
  - Path=/login/**

- id: oauth2-error
  uri: lb://auth-server
  predicates:
  - Path=/error/**

- id: product-composite-swagger-ui
  uri: lb://product-composite
  predicates:
  - Path=/openapi/**

- id: eureka-api
  uri: http://${app.eureka-server}:8761
  predicates:
  - Path=/eureka/api/{segment}
  filters:
  - SetPath=/eureka/{segment}

- id: eureka-web-start
  uri: http://${app.eureka-server}:8761
  predicates:
  - Path=/eureka/web
  filters:
  - SetPath=/

- id: eureka-web-other
  uri: http://${app.eureka-server}:8761
  predicates:
  - Path=/eureka/**

- id: config-server
  uri: ${spring.cloud.config.uri}
  predicates:
  - Path=/config/**
  filters:
  - RewritePath=/config/(?<segment>.*), /$\{segment}

- id: host_route_200
  uri: http://httpbin:8080  # 교재와 다름 (교재: http://httpstat.us, 10장부터)
  predicates:
  - Host=i.feel.lucky:8080
  - Path=/headerrouting/**
  filters:
  - SetPath=/status/200  # 교재와 다름 (교재: /200)

- id: host_route_418
  uri: http://httpbin:8080  # 교재와 다름 (교재: http://httpstat.us, 10장부터)
  predicates:
  - Host=im.a.teapot:8080
  - Path=/headerrouting/**
  filters:
  - SetPath=/status/418  # 교재와 다름 (교재: /418)

- id: host_route_501
  uri: http://httpbin:8080  # 교재와 다름 (교재: http://httpstat.us, 10장부터)
  predicates:
  - Path=/headerrouting/**
  filters:
  - SetPath=/status/501  # 교재와 다름 (교재: /501)

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}:9999

management.endpoint.gateway.enabled: true

logging:
  level:
    root: INFO
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: INFO
    org.springframework.cloud.gateway: TRACE
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: TRACE
```

11장 gateway `application.yml`과 다른 곳은 하나다. 설정 서버로 가는 라우트 `config-server`가 생겼다.
- `uri: ${spring.cloud.config.uri}`: 게이트웨이 자신이 설정을 받아 온 주소(도커에서는 `http://config-server:8888`, [03](03-config-clients.md)의 bootstrap.yml).
- `RewritePath`: `/config/product/docker` → 설정 서버의 `/product/docker`. 앞의 `/config`만 떼고 넘긴다.
- 설정 서버 포트(8888)는 밖에 열지 않는다. 밖에서는 게이트웨이(8443)의 `/config/**`로만 들어온다.

## 6. product-composite.yml (새 파일)
```bash
code config-repo/product-composite.yml
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
      usePkceWithAuthorizationCodeGrant: true  # 교재와 다름 (교재에는 없음, 11장부터)
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
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message, 3장부터)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

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

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}:9999

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: TRACE

---
spring.config.activate.on-profile: docker

server.port: 8080

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
```

## 7. product.yml (새 파일)
```bash
code config-repo/product.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message, 3장부터)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

spring.mongodb:  # 교재와 다름 (교재: spring.data.mongodb, 6장부터)
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
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb  # 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)

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
```

## 8. recommendation.yml (새 파일)
```bash
code config-repo/recommendation.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message, 3장부터)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

spring.mongodb:  # 교재와 다름 (교재: spring.data.mongodb, 6장부터)
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
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG

---
spring.config.activate.on-profile: docker

server.port: 8080

spring.mongodb.host: mongodb  # 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)

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
```

## 9. review.yml (새 파일)
```bash
code config-repo/review.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message, 3장부터)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

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
    se.magnus: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

---
spring.config.activate.on-profile: docker

server.port: 8080

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
```

## 암호문은 무엇인가
| 파일 | 설정 | 풀면 (11장 평문 값) |
|---|---|---|
| application.yml | `app.eureka-password` | `p` |
| application.yml | `spring.rabbitmq.password` | `guest` |
| review.yml | `spring.datasource.password` | `pwd` |
| gateway.yml | 인증서 `keystore.password` | `password` |
| product-composite.yml | Swagger `clientSecret` | `secret-writer` |

교재 값 그대로이고, 위 키(`my-very-secure-encrypt-key`)로 실제로 풀리는 것을 확인했다. 직접 암호문을 만들고 싶으면 compose를 띄운 뒤([04](04-docker-compose-and-test.md)):
```bash
curl -k https://dev-usr:dev-pwd@localhost:8443/config/encrypt --data-urlencode "p"
```
같은 값도 부를 때마다 암호문이 달라진다(랜덤 salt). 어느 것을 써도 같은 평문으로 풀린다.

## 확인 (선택, 도커 필요 없음)
[01](01-config-server.md)에서 빌드한 설정 서버를 로컬로 띄워 config-repo를 잘 읽는지 본다.
```bash
cd ~/msa
ENCRYPT_KEY=my-very-secure-encrypt-key SPRING_SECURITY_USER_NAME=dev-usr SPRING_SECURITY_USER_PASSWORD=dev-pwd java -jar spring-cloud/config-server/build/libs/config-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=native
```
로그에 `Serving configurations from folder: file:/home/leo/msa/config-repo`가 나오면 다른 터미널에서:
```bash
curl -s localhost:8888/review/docker -o /dev/null -w "%{http_code}\n"
curl -s -u dev-usr:dev-pwd localhost:8888/review/docker | jq -r '.propertySources[].source["spring.datasource.password"] // empty'
curl -s -u dev-usr:dev-pwd localhost:8888/application/default | jq -r '.propertySources[0].source["app.eureka-password"]'
```
첫 줄 `401`(계정 없음), 둘째 `pwd`, 셋째 `p`. 암호문이 평문으로 풀려서 나온다. 끝나면 설정 서버 터미널에서 `Ctrl+C`.

다음: [03-config-clients.md](03-config-clients.md)
