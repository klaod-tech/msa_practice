# 03. test-em-all에 서킷 브레이커 테스트, 직접 해 보기

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 12장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 13장과 다르게 쓰는 부분.
> 교재 "서킷 브레이커와 재시도 메커니즘 사용해보기" 절. compose 파일은 바뀌지 않는다.

## 1. test-em-all.bash
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
#
# Sample usage:
#
#   HOST=localhost PORT=7001 ./test-em-all.bash
#
: ${HOST=localhost}
: ${PORT=8443}
: ${PROD_ID_REVS_RECS=1}
: ${PROD_ID_NOT_FOUND=13}
: ${PROD_ID_NO_RECS=113}
: ${PROD_ID_NO_REVS=213}
: ${SKIP_CB_TESTS=false}  # ★ 추가

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

function testCompositeCreated() {

    # Expect that the Product Composite for productId $PROD_ID_REVS_RECS has been created with three recommendations and three reviews
    if ! assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
    then
        echo -n "FAIL"
        return 1
    fi

    set +e
    assertEqual "$PROD_ID_REVS_RECS" $(echo $RESPONSE | jq .productId)
    if [ "$?" -eq "1" ] ; then return 1; fi

    assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
    if [ "$?" -eq "1" ] ; then return 1; fi

    assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")
    if [ "$?" -eq "1" ] ; then return 1; fi

    set -e
}

function waitForMessageProcessing() {
    echo "Wait for messages to be processed... "

    # Give background processing some time to complete...
    sleep 1

    n=0
    until (testCompositeCreated)
    do
        n=$((n + 1))
        if [[ $n == 40 ]]
        then
            echo " Give up"
            exit 1
        else
            sleep 6
            echo -n ", retry #$n "
        fi
    done
    echo "All messages are now processed!"
}

function recreateComposite() {
  local productId=$1
  local composite=$2

  assertCurl 202 "curl -X DELETE $AUTH -k https://$HOST:$PORT/product-composite/${productId} -s"
  assertEqual 202 $(curl -X POST -s -k https://$HOST:$PORT/product-composite -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" --data "$composite" -w "%{http_code}")
}

function setupTestdata() {

  body="{\"productId\":$PROD_ID_NO_RECS"
  body+=\
',"name":"product name A","weight":100, "reviews":[
  {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
  {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
  {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
]}'
  recreateComposite "$PROD_ID_NO_RECS" "$body"

  body="{\"productId\":$PROD_ID_NO_REVS"
  body+=\
',"name":"product name B","weight":200, "recommendations":[
  {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
  {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
  {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
]}'
  recreateComposite "$PROD_ID_NO_REVS" "$body"


  body="{\"productId\":$PROD_ID_REVS_RECS"
  body+=\
',"name":"product name C","weight":300, "recommendations":[
      {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
      {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
      {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
  ], "reviews":[
      {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
      {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
      {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
  ]}'
  recreateComposite "$PROD_ID_REVS_RECS" "$body"

}

# ★ 추가 (testCircuitBreaker 함수 전체)
function testCircuitBreaker() {

    echo "Start Circuit Breaker tests!"

    assertEqual "CLOSED" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    for ((n=0; n<3; n++))
    do
        assertCurl 500 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
        message=$(echo $RESPONSE | jq -r .message)
        assertEqual "Did not observe any item or terminal signal within 2000ms" "${message:0:57}"
    done

    assertEqual "OPEN" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 404 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND $AUTH -s"
    assertEqual "Product Id: $PROD_ID_NOT_FOUND not found in fallback cache!" "$(echo $RESPONSE | jq -r .message)"

    echo "Will sleep for 10 sec waiting for the CB to go Half Open..."
    sleep 10

    assertEqual "HALF_OPEN" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    for ((n=0; n<3; n++))
    do
        assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
        assertEqual "product name C" "$(echo "$RESPONSE" | jq -r .name)"
    done

    assertEqual "CLOSED" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    assertEqual "CLOSED_TO_OPEN"      "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-3].stateTransition)"
    assertEqual "OPEN_TO_HALF_OPEN"   "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-2].stateTransition)"
    assertEqual "HALF_OPEN_TO_CLOSED" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-1].stateTransition)"
}

set -e

echo "Start Tests:" `date`

echo "HOST=${HOST}"
echo "PORT=${PORT}"
echo "SKIP_CB_TESTS=${SKIP_CB_TESTS}"  # ★ 추가

if [[ $@ == *"start"* ]]
then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  docker compose down --remove-orphans
  echo "$ docker compose up -d"
  docker compose up -d
fi

waitForService curl -k https://$HOST:$PORT/actuator/health

ACCESS_TOKEN=$(curl -k https://writer:secret-writer@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq .access_token -r)
echo ACCESS_TOKEN=$ACCESS_TOKEN
AUTH="-H \"Authorization: Bearer $ACCESS_TOKEN\""

# Verify access to Eureka and that all four microservices are registered in Eureka
assertCurl 200 "curl -H "accept:application/json" -k https://u:p@$HOST:$PORT/eureka/api/apps -s"
assertEqual 6 $(echo $RESPONSE | jq ".applications.application | length")

assertCurl 200 "curl -H "accept:application/json" -k https://dev-usr:dev-pwd@$HOST:$PORT/config/product/docker -s"
TEST_VALUE="hello-world"
ENCRYPTED_VALUE=$(curl -k https://dev-usr:dev-pwd@$HOST:$PORT/config/encrypt --data-urlencode "$TEST_VALUE" -s)
DECRYPTED_VALUE=$(curl -k https://dev-usr:dev-pwd@$HOST:$PORT/config/decrypt -d $ENCRYPTED_VALUE -s)
assertEqual "$TEST_VALUE" "$DECRYPTED_VALUE"


setupTestdata

waitForMessageProcessing

# Verify that a normal request works, expect three recommendations and three reviews
assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 404 (Not Found) error is returned for a non-existing productId ($PROD_ID_NOT_FOUND)
assertCurl 404 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

# Verify that no recommendations are returned for productId $PROD_ID_NO_RECS
assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that no reviews are returned for productId $PROD_ID_NO_REVS
assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 422 (Unprocessable Entity) error is returned for a productId that is out of range (-1)
assertCurl 422 "curl $AUTH -k https://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

# Verify that a 400 (Bad Request) error error is returned for a productId that is not a number, i.e. invalid format
assertCurl 400 "curl $AUTH -k https://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

# Verify that a request without access token fails on 401, Unauthorized
assertCurl 401 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

# Verify that the reader - client with only read scope can call the read API but not delete API.
READER_ACCESS_TOKEN=$(curl -k https://reader:secret-reader@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read" -s | jq .access_token -r)
echo READER_ACCESS_TOKEN=$READER_ACCESS_TOKEN
READER_AUTH="-H \"Authorization: Bearer $READER_ACCESS_TOKEN\""

assertCurl 200 "curl $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertCurl 403 "curl -X DELETE $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

# Verify access to Swagger and OpenAPI URLs
echo "Swagger/OpenAPI tests"
assertCurl 302 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ksL https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/index.html"
# Verify the redirect URL
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/oauth2-redirect.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs"
assertEqual "3.1.0" "$(echo $RESPONSE | jq -r .openapi)"
assertEqual "https://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.servers[0].url')"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs.yaml"

# ★ 추가 (if 블록): 서킷 브레이커 테스트
if [[ $SKIP_CB_TESTS == "false" ]]
then
    testCircuitBreaker
fi

if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi

echo "End, all tests OK:" `date`
```

`testCircuitBreaker`가 하는 일(맨 끝, 다른 테스트가 다 끝난 뒤):

| 순서 | 호출 | 기대 |
|---|---|---|
| 1 | health | `CLOSED` |
| 2 | `?delay=3` 3번 | 매번 2초 타임아웃 → **500**, 메시지 `Did not observe any item or terminal signal within 2000ms` |
| 3 | health | `OPEN` (최근 5번 중 3번 실패 = 60% ≥ 50%) |
| 4 | `?delay=3`, 그냥 호출 | 둘 다 **200**, 이름 `Fallback product1` (product를 부르지 않고 바로 대체 응답) |
| 5 | productId 13 | **404**, `Product Id: 13 not found in fallback cache!` |
| 6 | 10초 대기 → health | `HALF_OPEN` |
| 7 | 그냥 호출 3번 | 200, 진짜 이름 `product name C` |
| 8 | health, 이벤트 | `CLOSED`, 전환 기록 `CLOSED_TO_OPEN` → `OPEN_TO_HALF_OPEN` → `HALF_OPEN_TO_CLOSED` |

health는 게이트웨이를 거치지 않고 `docker compose exec`로 composite 컨테이너 안에서 직접 본다(밖에서 actuator에 접근할 일이 없게).

서킷 브레이커 테스트를 빼고 돌리려면 `SKIP_CB_TESTS=true ./test-em-all.bash`.

## 2. 빌드와 구동
```bash
cd ~/msa
./gradlew build && docker compose build && docker compose up -d
./test-em-all.bash
```
마지막에 `End, all tests OK`. 서킷 브레이커 테스트에 10초 대기가 있어서 12장보다 15초쯤 더 걸린다.

## 3. 직접 해 보기
토큰 먼저(11장과 같음):
```bash
ACCESS_TOKEN=$(curl -k https://writer:secret-writer@localhost:8443/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq -r .access_token)
```
회로 상태 보는 명령(아래에서 계속 쓴다):
```bash
docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/health | jq -r .components.circuitBreakers.details.product.details.state
```

### 3-1. 타임아웃으로 회로 열기
```bash
for n in 1 2 3; do curl -k "https://localhost:8443/product-composite/1?delay=3" -H "Authorization: Bearer $ACCESS_TOKEN" -s | jq -r .message; done
```
세 번 다 `Did not observe any item or terminal signal within 2000ms ...`(약 2초씩 걸림). 회로 상태를 보면 `OPEN`.

> test-em-all을 방금 돌렸다면 최근 호출이 성공으로 차 있어서 위 3번이면 열린다. compose를 막 올리고 처음 해 본다면 최근 호출 수가 5번이 안 돼서 `CLOSED`일 수 있다. 그때는 한두 번 더 하면 열린다.

### 3-2. 열린 동안: 바로 대체 응답
```bash
time curl -k "https://localhost:8443/product-composite/1?delay=3" -H "Authorization: Bearer $ACCESS_TOKEN" -s | jq -r .name
curl -k https://localhost:8443/product-composite/13 -H "Authorization: Bearer $ACCESS_TOKEN" -s | jq -r .message
```
첫 줄 `Fallback product1`, 3초 지연을 줬는데도 **바로**(1초 미만) 온다. product를 부르지 않기 때문이다. 둘째 줄 `Product Id: 13 not found in fallback cache!`.

### 3-3. 10초 뒤 반쯤 열림 → 닫힘
```bash
sleep 10
```
회로 상태 `HALF_OPEN`. 정상 호출 3번:
```bash
for n in 1 2 3; do curl -k https://localhost:8443/product-composite/1 -H "Authorization: Bearer $ACCESS_TOKEN" -s | jq -r .name; done
```
`product name C` 3번, 회로 상태 `CLOSED`.

### 3-4. 전환 기록
```bash
docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r '.circuitBreakerEvents[].stateTransition'
```

### 3-5. 재시도
```bash
curl -k "https://localhost:8443/product-composite/1?faultPercent=25" -H "Authorization: Bearer $ACCESS_TOKEN" -s -o /dev/null -w "%{http_code}\n"
```
여러 번 해 본다. 4번 중 1번꼴로 product가 500을 내지만 composite가 1초 뒤 다시 부르므로 대부분 `200`이 나온다(가끔 1초쯤 늦다). product 로그에서 실패와 재시도를 볼 수 있다:
```bash
docker compose logs product | grep "Bad luck" | tail -3
```

## 4. 끝내기
```bash
docker compose down
```

다음 장: 14장(작성 예정)
