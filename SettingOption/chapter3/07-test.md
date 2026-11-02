# 07. 테스트 (수동 81쪽 + 자동화 84쪽 + test-em-all.bash)

| 방법 | 서비스를 띄워야 하나 | 무엇을 확인하나 |
|---|---|---|
| 1. 수동 curl | 4개 모두 | 눈으로 확인 |
| 2. 자동화 테스트 (JUnit) | 아니다 (Mock 사용) | composite 하나만 단독으로 |
| 3. `test-em-all.bash` | 4개 모두 | 전체 시스템을 실제로 호출 (curl 확인을 스크립트로 자동화) |

## 1. 수동 테스트: 서비스 4개 띄우고 curl
```bash
cd ~/msa
./gradlew build
java -jar microservices/product-composite-service/build/libs/*.jar &
java -jar microservices/product-service/build/libs/*.jar &
java -jar microservices/recommendation-service/build/libs/*.jar &
java -jar microservices/review-service/build/libs/*.jar &
```
- `Started ...Application`이 **4번** 나올 때까지 기다린다. 로그가 섞여 출력되면 Enter를 눌러 프롬프트를 되찾는다.
- 다 떴는지 포트로 확인: `ss -ltnp | grep 700`

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

파일: `microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java` (교재 소스를 가져와 수정)

### 개념
- `WebTestClient`: 테스트용 curl이다. `jsonPath("$.productId")`는 `jq .productId`와 같은 역할이다.
- `@MockitoBean ProductCompositeIntegration`: Integration을 **가짜(Mock)로 바꿔치기**한다. 그래서 핵심 서비스 3개를 띄우지 않고 composite 하나만 테스트한다.
- `when(...getRecommendations(1)).thenReturn(singletonList(...))`처럼 가짜가 1개만 돌려주도록 정해 두었으므로, 테스트에서 추천·리뷰는 **1개**씩이다(curl에서는 3개).

### Boot 4 수정점
```java
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;    // ENTITY 대신
import static org.springframework.http.MediaType.APPLICATION_JSON;          // 본문의 APPLICATION_JSON_UTF8은 Spring 7에서 삭제됨
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient          // Boot 4: 없으면 WebTestClient가 주입되지 않음
class ProductCompositeServiceApplicationTests {
```
`@AutoConfigureWebTestClient`가 없으면 테스트 4개가 모두 `No qualifying bean of type 'org.springframework.test.web.reactive.server.WebTestClient'`로 실패한다. Boot 4에서 WebTestClient 설정이 별도 모듈(`spring-boot-webtestclient`)로 분리됐기 때문이다.

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
1번 수동 curl 확인을 스크립트로 자동화한 것이다. 레포 루트(`~/msa/test-em-all.bash`)에 둔다.
교재 소스(`$BOOK_HOME/Chapter03/2-basic-rest-services/test-em-all.bash`)를 **그대로** 쓴다. 기본 포트가 7001이고, 상태 코드와 `message`만 비교하므로 Boot 4에서도 고칠 부분이 없다(422의 `error` 문구는 검사하지 않음).

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
./gradlew build
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
