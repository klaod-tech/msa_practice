# 9장 트러블슈팅

## eureka-server 테스트 컴파일 실패: `package org.springframework.boot.test.web.client does not exist`
**원인:** Boot 4에서 `TestRestTemplate`이 `org.springframework.boot.resttestclient` 패키지(별도 모듈)로 옮겨졌다.
**해결:** [01](01-eureka-server.md) 2절 build.gradle의 테스트 의존성 2줄, 5절 테스트의 import 2줄과 `@AutoConfigureTestRestTemplate`.

## eureka-server 테스트: `ClassNotFoundException: org.springframework.boot.restclient.RestTemplateBuilder`
**원인:** `spring-boot-resttestclient`만 넣고 `spring-boot-starter-restclient-test`를 빠뜨렸다.
**해결:** 둘 다 넣는다.
```groovy
	testImplementation 'org.springframework.boot:spring-boot-starter-restclient-test'
	testImplementation 'org.springframework.boot:spring-boot-resttestclient'
```

## eureka-server `healthy()` 실패: `expected: <{"status":"UP"}> but was: <{"groups":["liveness","readiness"],"status":"UP"}>`
**원인:** Boot 4의 health 응답에 그룹 목록이 같이 나온다.
**해결:** [01](01-eureka-server.md) 5절처럼 기대값을 바꾼다.

## Gradle이 바로 실패: `Cannot invoke method include() on null object`
**원인:** settings.gradle의 마지막 줄 뒤에 줄바꿈이 없는데 그 뒤에 이어 붙여서 `include ':util'include ':spring-cloud:eureka-server'`처럼 한 줄이 됐다.
**해결:** 두 `include`를 서로 다른 줄로 나눈다([01](01-eureka-server.md) 1절).

## composite 조회가 500, 헬스 체크에 `503 Service Unavailable from UNKNOWN`
`curl localhost:8080/actuator/health -s | jq .components.coreServices`에서 product 등이 DOWN이고 에러가 `ServiceUnavailable: 503 ... from UNKNOWN`.
**원인:** composite가 부르는 이름(`http://product`)으로 **등록된 인스턴스가 없다**. 둘 중 하나다.
1. 서비스가 아직 등록 전이다(시작 직후 몇 초). 잠시 뒤 다시 본다.
2. 서비스 이름이 다르다. 핵심 서비스 yml의 `spring.application.name`이 아직 `product-service` 같은 옛 값이다([02](02-eureka-clients.md) 2절).
**확인:**
```bash
curl -H "accept:application/json" localhost:8761/eureka/apps -s | jq -r '.applications.application[].name'
```
`PRODUCT`, `RECOMMENDATION`, `REVIEW`, `PRODUCT-COMPOSITE`가 나와야 한다(`PRODUCT-SERVICE`처럼 보이면 2번).
