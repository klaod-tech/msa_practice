# 07. 테스트 (수동 81쪽 + 자동화 84쪽 + test-em-all.bash)

| 방법 | 서비스를 띄워야 하나 | 무엇을 확인하나 |
|---|---|---|
| 1. 수동 curl | 4개 모두 | 눈으로 확인 |
| 2. 자동화 테스트 (JUnit) | 아니다 (Mock 사용) | composite 하나만 단독으로 |
| 3. `test-em-all.bash` | 4개 모두 | 전체 시스템을 실제로 호출 (curl 확인을 스크립트로 자동화) |

## 1. 수동 테스트: 서비스 4개 띄우고 curl
먼저 `clean`으로 예전 빌드 결과를 지우고 빌드한 뒤, 서비스마다 jar가 **하나씩만** 있는지 확인한다.
```bash
cd ~/msa
./gradlew clean build
ls microservices/*/build/libs/     # 서비스마다 ...-1.0.0-SNAPSHOT.jar 하나씩 (…-plain.jar가 있으면 안 됨)
```
- `jar { enabled = false }`를 넣기 **전에** 한 번이라도 빌드했다면 `...-plain.jar`가 남아 있다. 이 설정은 앞으로 만들지 않을 뿐 이미 있는 파일은 지우지 않는다. 그러면 아래 `*.jar`가 plain jar를 잡아서 `no main manifest attribute, in ...-plain.jar`를 내고 그 서비스만 바로 꺼진다(`Exit 1`). `clean`이 이 파일을 지운다.

```bash
java -jar microservices/product-composite-service/build/libs/*.jar &
java -jar microservices/product-service/build/libs/*.jar &
java -jar microservices/recommendation-service/build/libs/*.jar &
java -jar microservices/review-service/build/libs/*.jar &
```
- `Started ...Application`이 **4번** 나올 때까지 기다린다. 로그가 섞여 출력되면 Enter를 눌러 프롬프트를 되찾는다.
- 다 떴는지 포트로 확인: `ss -ltnp | grep 700`

### 브라우저로 서비스별 확인
서비스가 다 뜨면 브라우저 주소창에 붙여 넣어 각자 화면에서 확인한다. 주소 끝까지(`/product/1` 등) 넣어야 한다. `http://localhost:7002/`처럼 뒤를 비우면 그 주소를 처리하는 API가 없어서 **Whitelabel Error Page(404)**가 나온다(정상).

| 서비스 | 포트 | 주소 | 나와야 하는 것 |
|---|---|---|---|
| product-composite | 7001 | http://localhost:7001/product-composite/1 | 상품 + 추천 3개 + 리뷰 3개 + `serviceAddresses` 4개(`cmp`, `pro`, `rec`, `rev`) |
| product | 7002 | http://localhost:7002/product/1 | `productId` 1, `name` `name-1`, `weight` 123 |
| recommendation | 7003 | http://localhost:7003/recommendation?productId=1 | 추천 3개 (`Author 1`~`3`) |
| review | 7004 | http://localhost:7004/review?productId=1 | 리뷰 3개 (`Subject 1`~`3`) |

- composite에서 추천이나 리뷰가 `[]`이고 `rec`/`rev`가 `""`이면, 그 서비스 주소를 바로 열어 본다. 거기서 404(`No static resource ...`)가 나면 그 서비스의 `ServiceImpl`에 `@RestController`가 빠졌거나 저장이 안 된 것이다.
- Chrome에서 `pretty print 적용`을 체크하면 JSON이 줄바꿈되어 보기 쉽다.

같은 방식으로 composite의 특수 케이스도 확인할 수 있다.

| 주소 | 나와야 하는 것 |
|---|---|
| http://localhost:7001/product-composite/13 | **404**, `"message":"No product found for productId: 13"` |
| http://localhost:7001/product-composite/113 | 추천 `[]`, `rec` `""`, 리뷰 3개 |
| http://localhost:7001/product-composite/213 | 리뷰 `[]`, `rev` `""`, 추천 3개 |
| http://localhost:7001/product-composite/-1 | **422**, `"message":"Invalid productId: -1"` |
| http://localhost:7001/product-composite/abc | **400**, `"message":"Type mismatch."` |

### 호출과 결과
| 명령 | 결과 |
|---|---|
| `curl localhost:7001/product-composite/1 -s \| jq .` | 200, 추천 3개, 리뷰 3개, `serviceAddresses` 4개 |
| `curl localhost:7001/product-composite/13 -i` | **404**, `"message":"No product found for productId: 13"` |
| `curl localhost:7001/product-composite/113 -s \| jq .` | 200, `recommendations: []`, `rec: ""` |
| `curl localhost:7001/product-composite/213 -s \| jq .` | 200, `reviews: []`, `rev: ""` |
| `curl localhost:7001/product-composite/-1 -i` | **422**, `"message":"Invalid productId: -1"` |
| `curl localhost:7001/product-composite/abc -i` | **400**, `"message":"Type mismatch."` |

- `-s | jq .`: 본문 JSON만 보기 좋게 출력한다.
- `-i`: 응답 헤더(상태 코드)까지 출력한다. 오류 케이스는 상태 코드를 봐야 하므로 `-i`를 쓴다.

교재와 다른 점(모두 정상):
- 422의 `error`가 `"Unprocessable Content"`다(교재: `"Unprocessable Entity"`). Spring 7에서 422의 공식 이름이 바뀌었다.
- 400 응답만 모양이 다르다(`requestId`가 있음). 우리 핸들러가 아니라 Spring 기본 오류 처리기가 만든 응답이라서다.

### 종료
```bash
kill $(jobs -p)          # 서비스를 띄운 그 터미널에서
```
한 개만 다시 띄울 때는 PID로 종료한다. 포트가 비었는지 확인한 뒤 다시 실행한다.
```bash
kill <PID>               # 시작 로그의 "with PID 124097" 값
ss -ltnp | grep 7001     # 아무것도 안 나오면 포트가 빈 것
```
옛 프로세스가 포트를 잡고 있는데 새로 띄우면 `Error starting ApplicationContext`(`Port 7001 was already in use`)가 난다. `Exit 143`은 kill로 종료됐다는 정상 메시지다.

### 일부 서비스만 떠 있을 때
composite만 띄우고 호출하면 404가 아니라 **500**이 나온다.
```
"message":"I/O error on GET request for \"http://localhost:7002/product/1\": Connection refused"
```
composite는 정상이고, 호출 대상 서비스가 안 떠 있다는 뜻이다.

---

## 2. 자동화 테스트 (84쪽)
curl로 손으로 확인하던 것을 코드로 자동 확인한다. `./gradlew build` 때마다 실행되어, 코드가 망가지면 바로 알려 준다.

파일: `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java`

### 개념
- `WebTestClient`: 테스트용 curl이다. `jsonPath("$.productId")`는 `jq .productId`와 같은 역할이다.
- `@MockitoBean ProductCompositeIntegration`: Integration을 **가짜(Mock)로 바꿔치기**한다. 그래서 핵심 서비스 3개를 띄우지 않고 composite 하나만 테스트한다.
- `when(...getRecommendations(1)).thenReturn(singletonList(...))`처럼 가짜가 1개만 돌려주도록 정해 두었으므로, 테스트에서 추천·리뷰는 **1개**씩이다(curl에서는 3개).

### 파일 작성
### `product-composite-service/src/test/.../ProductCompositeServiceApplicationTests.java` (전체)
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
`spring init`이 만든 내용(`contextLoads()`만 있음)을 **모두 지우고** 아래 내용 전체 입력

```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;     // ★ 참고: 교재: UNPROCESSABLE_ENTITY
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;   // ★ 참고: 교재에 없음 (Boot 4)
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient                                                                // ★ 참고: 교재에 없음 (Boot 4)
class ProductCompositeServiceApplicationTests {

    private static final int PRODUCT_ID_OK = 1;
    private static final int PRODUCT_ID_NOT_FOUND = 2;
    private static final int PRODUCT_ID_INVALID = 3;

    @Autowired private WebTestClient client;

    @MockitoBean private ProductCompositeIntegration compositeIntegration;

    @BeforeEach
    void setUp() {

        when(compositeIntegration.getProduct(PRODUCT_ID_OK))
            .thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));
        when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
            .thenReturn(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
        when(compositeIntegration.getReviews(PRODUCT_ID_OK))
            .thenReturn(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));

        when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND))
            .thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

        when(compositeIntegration.getProduct(PRODUCT_ID_INVALID))
            .thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
    }

    @Test
    void contextLoads() {}

    @Test
    void getProductById() {

        client.get()
            .uri("/product-composite/" + PRODUCT_ID_OK)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
                .jsonPath("$.recommendations.length()").isEqualTo(1)
                .jsonPath("$.reviews.length()").isEqualTo(1);
    }

    @Test
    void getProductNotFound() {

        client.get()
            .uri("/product-composite/" + PRODUCT_ID_NOT_FOUND)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
                .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
    }

    @Test
    void getProductInvalidInput() {

        client.get()
            .uri("/product-composite/" + PRODUCT_ID_INVALID)
            .accept(APPLICATION_JSON)
            .exchange()
            .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT)        // ★ 참고: 교재: UNPROCESSABLE_ENTITY
            .expectHeader().contentType(APPLICATION_JSON)
            .expectBody()
                .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
                .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
    }
}
```
- `★ 참고` 줄이 교재와 다른 곳이다(줄 끝 `// ★ ...`는 입력하지 않는다).
- `UNPROCESSABLE_ENTITY` → `UNPROCESSABLE_CONTENT` (import와 마지막 테스트 두 곳). Spring 7에서 ENTITY는 deprecated.
- `@AutoConfigureWebTestClient`와 그 import: 없으면 테스트 4개가 모두 `No qualifying bean of type 'org.springframework.test.web.reactive.server.WebTestClient'`로 실패한다. Boot 4에서 WebTestClient 설정이 별도 모듈(`spring-boot-webtestclient`)로 분리됐기 때문이다.
- `APPLICATION_JSON`을 쓴다. **교재 본문의 `APPLICATION_JSON_UTF8`은 Spring 7에서 삭제된 상수**라 쓰면 안 된다.

### 실행
```bash
./gradlew :microservices:product-composite-service:test
```
결과: `BUILD SUCCESSFUL`, 4개 통과 (`build/test-results/test/*.xml`에서 `tests="4" failures="0" errors="0"` 확인)

| 테스트 | 확인하는 것 |
|---|---|
| `contextLoads()` | 애플리케이션이 정상적으로 뜨는지 |
| `getProductById()` | 200 + productId, 추천 1개, 리뷰 1개 |
| `getProductNotFound()` | 404 |
| `getProductInvalidInput()` | 422 |

출력의 `Graceful shutdown` 로그는 테스트용 서버(`RANDOM_PORT`)를 종료한 기록이라 정상이다.

---

## 3. `test-em-all.bash` (전체 시스템 테스트)
1번 수동 curl 확인을 스크립트로 자동화한 것이다. 레포 루트(`~/msa/test-em-all.bash`)에 둔다. 교재 소스와 같은 내용이다(영어 설명 주석만 뺐다).
```bash
code test-em-all.bash
```
아래 내용 전체 입력

```bash
#!/usr/bin/env bash
: ${HOST=localhost}
: ${PORT=7001}
: ${PROD_ID_REVS_RECS=1}
: ${PROD_ID_NOT_FOUND=13}
: ${PROD_ID_NO_RECS=113}
: ${PROD_ID_NO_REVS=213}

function assertCurl() {

  local expectedHttpCode=$1
  local curlCmd="$2 -w \"%{http_code}\""
  local result=$(eval $curlCmd)
  local httpCode="${result:(-3)}"
  RESPONSE='' && (( ${#result} > 3 )) && RESPONSE="${result%???}"

  if [ "$httpCode" = "$expectedHttpCode" ]
  then
    if [ "$httpCode" = "200" ]
    then
      echo "Test OK (HTTP Code: $httpCode)"
    else
      echo "Test OK (HTTP Code: $httpCode, $RESPONSE)"
    fi
  else
    echo  "Test FAILED, EXPECTED HTTP Code: $expectedHttpCode, GOT: $httpCode, WILL ABORT!"
    echo  "- Failing command: $curlCmd"
    echo  "- Response Body: $RESPONSE"
    exit 1
  fi
}

function assertEqual() {

  local expected=$1
  local actual=$2

  if [ "$actual" = "$expected" ]
  then
    echo "Test OK (actual value: $actual)"
  else
    echo "Test FAILED, EXPECTED VALUE: $expected, ACTUAL VALUE: $actual, WILL ABORT"
    exit 1
  fi
}

set -e

echo "HOST=${HOST}"
echo "PORT=${PORT}"

assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 404 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 422 "curl http://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

assertCurl 400 "curl http://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

echo "End, all tests OK:" `date`
```
실행 권한을 준다.
```bash
chmod +x test-em-all.bash
```
기본 포트가 7001이고, 상태 코드와 `message`만 비교하므로 Boot 4에서도 고칠 부분이 없다(422의 `error` 문구는 검사하지 않음).

### 구조
- `assertCurl 기대코드 "curl ..."`: curl을 실행해 HTTP 상태 코드를 비교하고, 본문을 `$RESPONSE`에 저장한다.
- `assertEqual 기대값 실제값`: `jq`로 꺼낸 값을 비교한다.
- 하나라도 틀리면 `Test FAILED ...`를 출력하고 바로 멈춘다(`exit 1`).

| 검사 | 기대 결과 |
|---|---|
| `/product-composite/1` | 200, productId 1, 추천 3개, 리뷰 3개 |
| `/product-composite/13` | 404, `No product found for productId: 13` |
| `/product-composite/113` | 200, 추천 0개, 리뷰 3개 |
| `/product-composite/213` | 200, 추천 3개, 리뷰 0개 |
| `/product-composite/-1` | 422, `"Invalid productId: -1"` |
| `/product-composite/invalidProductId` | 400, `"Type mismatch."` |

### 실행
```bash
cd ~/msa
chmod +x test-em-all.bash        # 처음 한 번 (없으면 Permission denied)
./gradlew clean build
java -jar microservices/product-composite-service/build/libs/*.jar &
java -jar microservices/product-service/build/libs/*.jar &
java -jar microservices/recommendation-service/build/libs/*.jar &
java -jar microservices/review-service/build/libs/*.jar &
# Started ... 4번 확인 후
./test-em-all.bash
kill $(jobs -p)
```
다른 호스트·포트로 실행하려면 `HOST=localhost PORT=7001 ./test-em-all.bash`처럼 앞에 붙인다.

결과: 마지막 줄 `End, all tests OK: Mon Nov 2 14:04:47 KST 2026` → **3장 완료**
