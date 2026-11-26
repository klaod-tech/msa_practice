# 05. 헬름 구성 요소 차트: 지표 수집 설정

> 시작: 19장까지 따라 한 상태 / 결과: 20장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

서비스 차트의 `values.yaml`에 `prometheus.io/scrape: "true"` 어노테이션(이스티오 Prometheus가 이 파드의 지표를 긁어 가게), 차트 안 `config-repo` 설정 사본에 prometheus 엔드포인트 공개와 `management.metrics.tags.application`(지표에 서비스 이름 태그).

## 5-1. `kubernetes/helm/components/auth-server/config-repo/application.yml`
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

## 5-2. `kubernetes/helm/components/auth-server/values.yaml`
```bash
code kubernetes/helm/components/auth-server/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: auth-server

# ★ 추가 시작 (5줄)
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "4004"
  prometheus.io/scheme: http
  prometheus.io/path: "/actuator/prometheus"
# ★ 추가 끝

image:
  name: auth-server

env:
  SPRING_PROFILES_ACTIVE: "docker"
  SPRING_CONFIG_LOCATION: file:/config-repo/application.yml,file:/config-repo/auth-server.yml

livenessProbe_enabled: true

readinessProbe_enabled: true

configmap:
  enabled: true
  volumeMounts:
    mountPath: /config-repo
```

## 5-3. `kubernetes/helm/components/product-composite/config-repo/application.yml`
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

## 5-4. `kubernetes/helm/components/product-composite/values.yaml`
```bash
code kubernetes/helm/components/product-composite/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: product-composite

# ★ 추가 시작 (5줄)
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "4004"
  prometheus.io/scheme: http
  prometheus.io/path: "/actuator/prometheus"
# ★ 추가 끝

image:
  name: product-composite-service

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

service:
  ports:
    - port: 80
      name: http
      targetPort: 80
    - port: 4004
      name: http-mgm
      targetPort: 4004
```

## 5-5. `kubernetes/helm/components/product-green/config-repo/application.yml`
```bash
code kubernetes/helm/components/product-green/config-repo/application.yml
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

## 5-6. `kubernetes/helm/components/product/config-repo/application.yml`
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

## 5-7. `kubernetes/helm/components/product/values.yaml`
```bash
code kubernetes/helm/components/product/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: product

# ★ 추가 시작 (5줄)
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "4004"
  prometheus.io/scheme: http
  prometheus.io/path: "/actuator/prometheus"
# ★ 추가 끝

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

## 5-8. `kubernetes/helm/components/recommendation-green/config-repo/application.yml`
```bash
code kubernetes/helm/components/recommendation-green/config-repo/application.yml
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

## 5-9. `kubernetes/helm/components/recommendation/config-repo/application.yml`
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

## 5-10. `kubernetes/helm/components/recommendation/values.yaml`
```bash
code kubernetes/helm/components/recommendation/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: recommendation

# ★ 추가 시작 (5줄)
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "4004"
  prometheus.io/scheme: http
  prometheus.io/path: "/actuator/prometheus"
# ★ 추가 끝

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

## 5-11. `kubernetes/helm/components/review-green/config-repo/application.yml`
```bash
code kubernetes/helm/components/review-green/config-repo/application.yml
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

## 5-12. `kubernetes/helm/components/review/config-repo/application.yml`
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

## 5-13. `kubernetes/helm/components/review/values.yaml`
```bash
code kubernetes/helm/components/review/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
fullnameOverride: review

# ★ 추가 시작 (5줄)
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "4004"
  prometheus.io/scheme: http
  prometheus.io/path: "/actuator/prometheus"
# ★ 추가 끝

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

다음: [06-k8s-environments.md](06-k8s-environments.md)
