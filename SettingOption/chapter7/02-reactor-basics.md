# 02. 프로젝트 리액터 맛보기 (ReactorTests)

> 교재 "프로젝트 리액터 소개" 절. 서비스 코드는 안 바꾸고, util 프로젝트에 테스트 하나만 만든다.

## Mono와 Flux
| 타입 | 뜻 | 6장 코드와 비교 |
|---|---|---|
| `Mono<T>` | 값이 **0개 또는 1개** 나올 스트림 | `Product getProduct()` → `Mono<Product> getProduct()` |
| `Flux<T>` | 값이 **0개 이상** 나올 스트림 | `List<Review> getReviews()` → `Flux<Review> getReviews()` |

중요한 규칙: **스트림은 선언만 해서는 아무 일도 안 일어난다.** 누군가 구독(`subscribe`)해야 처리가 시작된다. 웹 요청에서는 스프링 웹플럭스가 응답을 만들 때 구독해 준다. 테스트나 블로킹 코드에서는 직접 `subscribe()` 하거나 `block()`으로 끝날 때까지 기다린다.

## 1. ReactorTests (새 파일)
```bash
mkdir -p util/src/test/java/se/magnus/util/reactor
code util/src/test/java/se/magnus/util/reactor/ReactorTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.util.reactor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class ReactorTests {

  @Test
  void testFlux() {

    List<Integer> list = new ArrayList<>();

    Flux.just(1, 2, 3, 4)
      .filter(n -> n % 2 == 0)
      .map(n -> n * 2)
      .log()
      .subscribe(n -> list.add(n));

    assertThat(list).containsExactly(4, 8);
  }

  @Test
  void testFluxBlocking() {

    List<Integer> list = Flux.just(1, 2, 3, 4)
      .filter(n -> n % 2 == 0)
      .map(n -> n * 2)
      .log()
      .collectList().block();

    assertThat(list).containsExactly(4, 8);
  }
}
```

| 메서드 | 하는 일 |
|---|---|
| `Flux.just(1, 2, 3, 4)` | 정수 4개로 스트림 시작 |
| `.filter(n -> n % 2 == 0)` | 짝수만 통과 → 2, 4 |
| `.map(n -> n * 2)` | 2배 → 4, 8 |
| `.log()` | 지나가는 신호(onSubscribe, onNext, onComplete)를 로그로 |
| `testFlux`: `.subscribe(n -> list.add(n))` | 구독하면서 받은 값을 리스트에 넣음 |
| `testFluxBlocking`: `.collectList().block()` | 다 모아서 리스트로, `block()`이 끝날 때까지 기다림 (교재 본문 예시) |

## 확인 (도커 필요 없음)
```bash
./gradlew :util:test --tests ReactorTests
```
`BUILD SUCCESSFUL`이면 된다. 교재 그림 7.3 같은 로그를 보려면:
```bash
./gradlew :util:test --tests ReactorTests --rerun -i | grep 'reactor.Flux'
```
```
... INFO reactor.Flux.MapFuseable.1 -- | onSubscribe([Fuseable] FluxMapFuseable.MapFuseableSubscriber)
... INFO reactor.Flux.MapFuseable.1 -- | request(unbounded)
... INFO reactor.Flux.MapFuseable.1 -- | onNext(4)
... INFO reactor.Flux.MapFuseable.1 -- | onNext(8)
... INFO reactor.Flux.MapFuseable.1 -- | onComplete()
```
(테스트가 2개라 같은 묶음이 `.2`로 한 번 더 나온다.)
