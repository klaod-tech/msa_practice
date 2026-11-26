# 05. config-repo: 추적을 Jaeger로

> 시작: 17장까지 따라 한 상태 / 결과: 18장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

`management.tracing.export.zipkin.endpoint`가 `http://jaeger-collector.istio-system:9411/...`(이스티오의 Jaeger가 Zipkin 형식으로 받는다).

## 5-1. `config-repo/application.yml`
```bash
code config-repo/application.yml
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

## 5-2. `config-repo/gateway.yml`
```bash
code config-repo/gateway.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 8443
management.server.port: 8443  # ★ 추가
spring.application.name: gateway

spring.cloud.gateway.server.webflux.trusted-proxies: ".*"

spring.ssl.bundle.jks.gateway:
  key:
    alias: localhost
  keystore:
    type: PKCS12
    location: classpath:keystore/edge.p12

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

다음: [06-k8s-common.md](06-k8s-common.md)
