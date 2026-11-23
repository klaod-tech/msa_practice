# 9장 트러블슈팅

## Gradle이 바로 실패: `Cannot invoke method include() on null object`
**원인:** settings.gradle의 마지막 줄 뒤에 줄바꿈이 없는데 그 뒤에 이어 붙여서 `include ':util'include ':spring-cloud:eureka-server'`처럼 한 줄이 됐다.
**해결:** 두 `include`를 서로 다른 줄로 나눈다([01](01-eureka-server.md) 1절).

## composite 조회가 500, 헬스 체크에 `503 Service Unavailable from UNKNOWN`
`curl localhost:8080/actuator/health -s | jq .components.coreServices`에서 product 등이 DOWN이고 에러가 `ServiceUnavailable: 503 ... from UNKNOWN`.
**원인:** composite가 부르는 이름(`http://product`)으로 **등록된 인스턴스가 없다**. 둘 중 하나다.
1. 서비스가 아직 등록 전이다(시작 직후 몇 초). 잠시 뒤 다시 본다.
2. 서비스 이름이 다르다. 핵심 서비스 yml의 `spring.application.name`이 아직 `product-service` 같은 옛 값이다([02](02-core-services.md)).
**확인:**
```bash
curl -H "accept:application/json" localhost:8761/eureka/apps -s | jq -r '.applications.application[].name'
```
`PRODUCT`, `RECOMMENDATION`, `REVIEW`, `PRODUCT-COMPOSITE`가 나와야 한다(`PRODUCT-SERVICE`처럼 보이면 2번).
