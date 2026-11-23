# 04. config-repo: prometheus 엔드포인트 공개

> 시작: 교수님 `Chapter19` 상태 / 결과: 교수님 `Chapter20`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`management.endpoints.web.exposure.include`에 `prometheus`를 더하고, `management.metrics.tags.application: ${spring.application.name}`으로 지표마다 서비스 이름을 붙인다. `gateway.yml`도 맞춘다.

## 4-1. `config-repo/application.yml`
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

management.server.port: 4004
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"
management.endpoint.health.probes.enabled: true
management.metrics.tags.application: ${spring.application.name}  # ★ 추가
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

management.endpoints.web.exposure.include: health,info,circuitbreakerevents,prometheus  # ★ 변경 (전: management.endpoints.web.exposure.include: health,info,circuitbreak...)
```

다음: [05-k8s-components.md](05-k8s-components.md)
