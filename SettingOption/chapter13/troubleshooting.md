# 13장 트러블슈팅

## 01까지 하고 빌드하면 composite가 컴파일 오류
**원인:** API의 `getProduct`에 파라미터가 2개 늘었는데 composite 쪽(`ProductCompositeIntegration`, `ProductCompositeServiceImpl`, 테스트)은 아직 예전 모양이다.
**해결:** 정상이다. [02](02-composite-resilience4j.md)까지 하고 빌드한다.

## 느린 호출 3번을 했는데 회로가 `CLOSED` 그대로
**원인:** 서킷 브레이커는 **최근 5번** 호출을 보고 판단한다. compose를 막 올려서 호출이 5번이 안 되면 아직 판단하지 않는다. 예를 들어 정상 1번 + 느린 3번 = 4번이라 그대로 `CLOSED`이고, 한 번 더 실패하면(5번 중 4번 실패) `OPEN`이 된다.
**해결:** 느린 호출을 한두 번 더 한다. test-em-all 안에서는 앞에서 정상 호출을 많이 한 뒤라 3번이면 열린다. 반대로 test-em-all이 **끝난 직후**에는 회로가 `CLOSED`로 돌아오면서 기록이 초기화돼서 3번으로는 안 열린다.

이 상태에서 3-2를 하면 대체 응답 대신 `null`(2초 타임아웃의 500)과 `No product found for productId: 13`(product가 직접 답한 404)이 나온다. `not found in fallback cache!`는 회로가 **열려 있을 때만** 나온다.

## 테스트: `InvalidUseOfMatchersException` (Mockito)
**원인:** `when(compositeIntegration.getProduct(PRODUCT_ID_OK, anyInt(), anyInt()))`처럼 값과 매처를 섞었다. Mockito는 한 파라미터라도 매처를 쓰면 전부 매처여야 한다.
**해결:** 값도 `eq(...)`로 감싼다([02](02-composite-resilience4j.md) 4절).

## test-em-all: 회로 상태 확인이 실패 (기대값 `CLOSED`, 실제 값이 비어 있음)
**원인:** health를 `docker compose exec`로 composite 컨테이너 안에서 보는데, `~/msa`가 아닌 곳에서 실행하면 compose 파일을 못 찾는다.
**해결:** `cd ~/msa` 후 `./test-em-all.bash`.

## test-em-all: `EXPECTED VALUE: CLOSED_TO_OPEN, ACTUAL VALUE: State transition from CLOSED to OPEN`
**원인:** Resilience4j 2.4.0(Boot 4용 모듈)부터 `/actuator/circuitbreakerevents`의 `stateTransition` 값이 `CLOSED_TO_OPEN` 같은 이름이 아니라 `State transition from CLOSED to OPEN` 같은 문장으로 나온다. 교재(2.3.0) 기대값을 그대로 두면 마지막 3줄에서 실패한다. 서킷 브레이커 동작 자체는 정상이다.
**해결:** `testCircuitBreaker` 마지막 `assertEqual` 3줄의 기대값을 문장으로 바꾼다([03](03-docker-compose-and-test.md) 1절).
```bash
    assertEqual "State transition from CLOSED to OPEN"      ...
    assertEqual "State transition from OPEN to HALF_OPEN"   ...
    assertEqual "State transition from HALF_OPEN to CLOSED" ...
```
