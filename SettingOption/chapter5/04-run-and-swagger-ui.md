# 04. 빌드·구동, 테스트, Swagger UI 확인

> 모든 명령은 `~/msa`에서 실행한다.

## 1. 빌드와 구동
```bash
cd ~/msa                                          # 교재: cd $BOOK_HOME/Chapter05
./gradlew build && docker compose build && docker compose up -d
```
- `&&`: 앞 명령이 **성공했을 때만** 다음 명령을 실행한다. 빌드가 실패하면 거기서 멈춘다.
- 교재의 "8080 포트가 이미 할당됐다" 오류는 교재가 **장마다 폴더가 달라서**(`Chapter04`, `Chapter05`) 컴포즈 이름이 `chapter04_...`, `chapter05_...`로 따로 생기기 때문이다. 우리는 **항상 `~/msa`**라서 컴포즈 이름이 `msa` 하나다 → `up -d`가 기존 컨테이너를 새 이미지로 교체하므로 보통 충돌하지 않는다.
- 떠 있는 컨테이너 확인:
  ```bash
  docker ps --format {{.Names}}      # msa-product-1, msa-product-composite-1 ... 4개
  ```
  4장 05에서 따로 띄운 `my-prd-srv` 같은 컨테이너가 남아 있으면 그때는 8080이 겹친다 → `docker rm -f my-prd-srv`

---

## 2. test-em-all.bash (전체)
`code test-em-all.bash` 들어가서 기존 내용을 모두 지우고 아래 내용 전체 입력

**교재 5장 내용 그대로**다(Boot 4 때문에 바꿀 곳 없음). 4장 버전과 다른 곳은 `Type mismatch` 테스트와 `stop` 블록 사이의 **Swagger/OpenAPI 테스트 11줄**뿐이다. 실행 권한은 4장 그대로라 `chmod`는 다시 안 해도 된다.
```bash
#!/usr/bin/env bash
#
# Sample usage:
#
#   HOST=localhost PORT=7001 ./test-em-all.bash
#
: ${HOST=localhost}
: ${PORT=8080}
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

function testUrl() {
  url=$@
  if $url -ks -f -o /dev/null
  then
    return 0
  else
    return 1
  fi;
}

function waitForService() {
  url=$@
  echo -n "Wait for: $url... "
  n=0
  until testUrl $url
  do
    n=$((n + 1))
    if [[ $n == 100 ]]
    then
      echo " Give up"
      exit 1
    else
      sleep 3
      echo -n ", retry #$n "
    fi
  done
  echo "DONE, continues..."
}

set -e

echo "Start Tests:" `date`

echo "HOST=${HOST}"
echo "PORT=${PORT}"

if [[ $@ == *"start"* ]]
then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  docker compose down --remove-orphans
  echo "$ docker compose up -d"
  docker compose up -d
fi

waitForService curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS

# Verify that a normal request works, expect three recommendations and three reviews
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 404 (Not Found) error is returned for a non-existing productId ($PROD_ID_NOT_FOUND)
assertCurl 404 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

# Verify that no recommendations are returned for productId $PROD_ID_NO_RECS
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that no reviews are returned for productId $PROD_ID_NO_REVS
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 422 (Unprocessable Entity) error is returned for a productId that is out of range (-1)
assertCurl 422 "curl http://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

# Verify that a 400 (Bad Request) error error is returned for a productId that is not a number, i.e. invalid format
assertCurl 400 "curl http://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

# Verify access to Swagger and OpenAPI URLs
echo "Swagger/OpenAPI tests"
assertCurl 302 "curl -s  http://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -sL http://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -s  http://$HOST:$PORT/openapi/swagger-ui/index.html"
# Verify the redirect URL
assertCurl 200 "curl -s  http://$HOST:$PORT/openapi/swagger-ui/oauth2-redirect.html"
assertCurl 200 "curl -s  http://$HOST:$PORT/openapi/v3/api-docs"
assertEqual "3.1.0" "$(echo $RESPONSE | jq -r .openapi)"
assertEqual "http://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.servers[0].url')"
assertCurl 200 "curl -s  http://$HOST:$PORT/openapi/v3/api-docs.yaml"

if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi

echo "End, all tests OK:" `date`
```

### 추가된 Swagger/OpenAPI 테스트
| 테스트 | 확인하는 것 |
|---|---|
| `302` swagger-ui.html (`-s`) | 짧은 주소가 실제 Swagger UI 페이지로 **리다이렉트**되는지. 리다이렉트 응답은 본문이 없어서 출력이 `Test OK (HTTP Code: 302, )`처럼 쉼표 뒤가 빈다(정상) |
| `200` swagger-ui.html (`-sL`) | 리다이렉트를 따라가면(`-L`) 페이지가 열리는지 |
| `200` swagger-ui/index.html | Swagger UI 본 페이지 |
| `200` oauth2-redirect.html | 인증 리다이렉트용 페이지 |
| `200` v3/api-docs | OpenAPI 문서(JSON) |
| `3.1.0` | 문서의 OpenAPI 버전 (springdoc 3.0.3 기본값 = 교재 기대값) |
| `http://localhost:8080` | 문서에 적힌 서버 주소 |
| `200` v3/api-docs.yaml | 같은 문서의 YAML 버전 |

## 3. 테스트 실행
```bash
./test-em-all.bash
```
이미 `up -d`로 띄워 놨으므로 `start` 없이 실행한다. 실제 결과: 4장 테스트 + `Swagger/OpenAPI tests` 아래 8줄 모두 `Test OK` → **`End, all tests OK`**

### 어떤 Test OK가 어느 테스트인지 보고 싶을 때
스크립트는 결과만 찍고 무엇을 테스트했는지는 안 보여 준다. 교재 스크립트를 고치지 않고 보려면:
```bash
bash -x ./test-em-all.bash 2>&1 | grep -E "^\+ assert|Test OK|Test FAILED|End,"
```
- `-x`: 실행하는 명령을 `+`를 붙여 먼저 보여 준다 → `+ assertCurl 302 '...swagger-ui.html'` 다음 줄에 그 결과가 나온다.
- `-x` 출력은 stderr로 나와서 `2>&1`로 합쳐야 `grep`으로 걸러진다.
- 스크립트에 `echo` 설명을 넣지 않는 이유: 시험은 교재 기준이라 스크립트는 교재 그대로 둔다.

---

## 4. Swagger UI로 API 확인 (브라우저)
컨테이너가 떠 있는 상태에서 **Windows 브라우저**로 연다(WSL의 Docker 포트가 Windows의 localhost로 연결돼 있다).
```
http://localhost:8080/openapi/swagger-ui.html
```

### Try it out으로 API 호출
1. 연산(`GET /product-composite/{productId}`)을 펼친다.
2. 오른쪽 위 **`Try it out`** 클릭 → 버튼이 `Cancel`로 바뀌고 `productId` 칸이 입력 가능해지며 **`Execute`** 버튼이 생긴다.
   **누르기 전에는 입력칸이 읽기 전용이라 클릭해도 입력이 안 된다**(실제로 여기서 막혔다).
3. `productId`에 **`123`** 입력 (교재 값)
4. **`Execute`** 클릭
5. 아래 Responses에서 확인
   - **Curl**: 같은 요청을 curl로 하면 어떤 명령인지 (`curl -X GET "http://localhost:8080/product-composite/123" -H "accept: application/json"`)
   - **Request URL**
   - **Server response**: `Code 200`과 Response body(JSON). 필드 순서는 교재 그림과 달리 알파벳순이다(Jackson 3, 정상)

`Try it out`이 반응하지 않으면: 새로 고침(`F5`), 또는 다크 모드 같은 브라우저 확장을 끄거나 시크릿 창에서 연다.

---

## 동작 원리 (어떻게 이 화면이 만들어지나)
```
① composite 시작 시 springdoc이 컨트롤러를 스캔
   - api의 ProductCompositeService: @GetMapping(경로·메서드), @PathVariable(입력 인수),
     반환 타입 ProductAggregate(응답 JSON 구조), @Tag/@Operation/@ApiResponses(설명)
   - @Operation 등의 ${...} → composite application.yml의 api: 문구로 치환
   - util의 @RestControllerAdvice(@ExceptionHandler) → 오류 응답 구조
   - Application 클래스의 OpenAPI 빈 → 제목·버전·연락처 등 공통 정보
        ↓
② 이것을 OpenAPI 문서(JSON)로 만들어 /openapi/v3/api-docs 로 제공
        ↓
③ 브라우저가 /openapi/swagger-ui.html 을 열면 Swagger UI(정적 웹페이지 + JavaScript)가 내려오고,
   그 JavaScript가 ②의 JSON을 받아 화면(연산 목록, 입력칸, 응답 설명)을 그린다
        ↓
④ Try it out → Execute 를 누르면 브라우저가 실제로 GET /product-composite/123 을 composite에 보낸다
   → 우리가 curl로 보내던 것과 같은 진짜 요청이고, 응답도 진짜 서비스 응답이다
```
- 화면에 나오는 문서는 손으로 쓴 게 아니라 **코드(애너테이션) + yml 문구로 자동 생성**된다. 코드를 고치면 문서도 같이 바뀐다.
- ②의 JSON은 직접 볼 수도 있다: `curl -s localhost:8080/openapi/v3/api-docs | jq .`

## 끝낼 때
```bash
docker compose down
```
