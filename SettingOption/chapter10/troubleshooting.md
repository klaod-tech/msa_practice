# 10장 트러블슈팅

## gateway 컴파일 실패: `package org.springframework.boot.actuate.health does not exist`
**원인:** Boot 4에서 헬스 클래스 패키지가 바뀌었다.
**해결:** [01](01-gateway-project.md) 4절처럼 import를 바꾼다.
```java
import org.springframework.boot.health.contributor.*;
```

## composite 컴파일 실패: `cannot find symbol ... getProductHealth`
**원인:** Integration에서 헬스 메서드를 지웠는데 composite의 `HealthCheckConfiguration.java`가 남아 있다.
**해결:** [02](02-composite-changes.md) 1절의 `rm` 명령으로 지운다.

## 헤더 라우팅이 응답 없음 / 타임아웃 (교재 그대로 httpstat.us를 쓴 경우)
**원인:** `httpstat.us`가 지금 응답하지 않는다(이름은 찾아지지만 연결이 안 됨).
**해결:** [01](01-gateway-project.md) 5절처럼 `uri: http://httpbin:8080`, `SetPath=/status/200` 등으로 바꾸고, [03](03-docker-compose-and-test.md) 1절의 `httpbin` 컨테이너를 compose에 넣는다.

## 로컬에서 gateway만 띄우면 health가 DOWN
**원인:** 로컬에는 핵심 서비스와 RabbitMQ가 없다. composite의 `binders`(RabbitMQ 연결)도 DOWN이라 composite 자체가 DOWN으로 보인다.
**해결:** compose로 전체를 올려서 확인한다.
