# 14장 트러블슈팅

## 01까지 하고 빌드하면 composite가 컴파일 오류
**원인:** composite의 변경(02)이 아직 안 됐다.
**해결:** 정상이다. [02](02-composite-observation.md)까지 하고 빌드한다.

## 컴파일 오류: `package org.springframework.boot.actuate.autoconfigure.observation does not exist`
**원인:** 교재의 import다. Boot 4에서 `ObservationRegistryCustomizer`가 다른 패키지로 옮겨졌다.
**해결:** [02](02-composite-observation.md) 3절처럼 바꾼다.
```java
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
```

## composite가 시작하지 못함: `BuildProperties` 빈이 없음
**원인:** `ObservationRegistryConfig`가 빌드 정보(`BuildProperties`)를 받는데, composite `build.gradle`에 `springBoot { buildInfo() }`가 없으면 빌드 정보가 만들어지지 않는다.
**해결:** [01](01-dependencies-and-logging.md) 1-4의 composite `build.gradle`에 `springBoot` 블록이 있는지 확인한다.

## 로그에 traceId 자리가 비어 있음 (`[product,,]`), Zipkin 서비스 목록이 비어 있음
**원인:** 추적이 켜지지 않았다. 교재의 두 줄(`micrometer-tracing-bridge-otel`, `opentelemetry-exporter-zipkin`)만으로는 Boot 4에서 추적 자동 설정이 켜지지 않는다. 자동 설정은 별도 모듈에 있다.
**해결:** 해당 서비스 `build.gradle`에 두 줄이 더 있는지 확인한다([01](01-dependencies-and-logging.md) 1절).
```groovy
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
	implementation 'org.springframework.boot:spring-boot-zipkin'
```

## Zipkin 조회가 `jq: parse error: Invalid numeric literal`
**원인:** 요청 직후라 아직 Zipkin에 trace가 없다. 서비스는 span을 몇 초씩 모았다가 보내서, 바로 부르면 JSON 대신 `<traceId> not found`라는 글자가 온다.
**해결:** 5초쯤 기다렸다 다시 부른다([04](04-docker-compose-and-zipkin.md) 5-3).
```bash
curl -s localhost:9411/api/v2/trace/$TRACE_ID
```
`... not found`가 계속 나오면 `TRACE_ID`가 비어 있지 않은지(`echo $TRACE_ID`)도 본다.
