# 04. compose에 DB 추가, test-em-all, 실행

> 시작: 교수님 `Chapter05` 상태 / 결과: 교수님 `Chapter06`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `docker-compose.yml`: `mongodb`, `mysql` 컨테이너 추가. 핵심 서비스는 DB가 **healthy**(healthcheck 통과)가 된 뒤 시작한다(`depends_on ... condition: service_healthy`).
- `test-em-all.bash`: 테스트 시작 전에 테스트 데이터(상품 1, 113, 213)를 지우고 다시 만든다.

## 4-1. `docker-compose.yml`
```bash
code docker-compose.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      mongodb:  # ★ 추가
        condition: service_healthy  # ★ 추가

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      mongodb:  # ★ 추가
        condition: service_healthy  # ★ 추가

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      mysql:  # ★ 추가
        condition: service_healthy  # ★ 추가

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
  # ★ 추가 (26줄)

  mongodb:
    image: mongo:8.0.5
    mem_limit: 512m
    ports:
      - "27017:27017"
    command: mongod
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 5s
      timeout: 2s
      retries: 60

  mysql:
    image: mysql:9.2.0
    mem_limit: 512m
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=rootpwd
      - MYSQL_DATABASE=review-db
      - MYSQL_USER=user
      - MYSQL_PASSWORD=pwd
    healthcheck:
      test: "/usr/bin/mysql --user=user --password=pwd --execute \"SHOW DATABASES;\""
      interval: 5s
      timeout: 2s
      retries: 60
```

## 4-2. `test-em-all.bash`
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
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

function recreateComposite() {  # ★ 추가
  local productId=$1  # ★ 추가
  local composite=$2  # ★ 추가

  assertCurl 200 "curl -X DELETE http://$HOST:$PORT/product-composite/${productId} -s"  # ★ 추가
  curl -X POST http://$HOST:$PORT/product-composite -H "Content-Type: application/json" --data "$composite"  # ★ 추가
}  # ★ 추가

function setupTestdata() {  # ★ 추가

  body="{\"productId\":$PROD_ID_NO_RECS"  # ★ 추가
  body+=\
',"name":"product name A","weight":100, "reviews":[
  {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
  {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
  {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
]}'
  recreateComposite "$PROD_ID_NO_RECS" "$body"  # ★ 추가

  body="{\"productId\":$PROD_ID_NO_REVS"  # ★ 추가
  body+=\
',"name":"product name B","weight":200, "recommendations":[
  {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
  {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
  {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
]}'
  recreateComposite "$PROD_ID_NO_REVS" "$body"  # ★ 추가

  body="{\"productId\":$PROD_ID_REVS_RECS"  # ★ 추가
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
  recreateComposite "$PROD_ID_REVS_RECS" "$body"  # ★ 추가

}  # ★ 추가

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

waitForService curl -X DELETE http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND  # ★ 변경 (전: waitForService curl http://$HOST:$PORT/product-composite/$PROD_ID_R...)

setupTestdata  # ★ 추가

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

echo "Swagger/OpenAPI tests"
assertCurl 302 "curl -s  http://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -sL http://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -s  http://$HOST:$PORT/openapi/swagger-ui/index.html"
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

문자열·블록 안이라 ★를 달지 못한 변경: `body+=\` (★ 추가); `',"name":"product name A","weight":100, "reviews":[` (★ 추가); `{"reviewId":1,"author":"author 1","subject":"subject 1","...` (★ 추가); `{"reviewId":2,"author":"author 2","subject":"subject 2","...` (★ 추가); `{"reviewId":3,"author":"author 3","subject":"subject 3","...` (★ 추가); `]}'` (★ 추가); `body+=\` (★ 추가); `',"name":"product name B","weight":200, "recommendations":[` (★ 추가); `{"recommendationId":1,"author":"author 1","rate":1,"conte...` (★ 추가); `{"recommendationId":2,"author":"author 2","rate":2,"conte...` (★ 추가); `{"recommendationId":3,"author":"author 3","rate":3,"conte...` (★ 추가); `]}'` (★ 추가); `body+=\` (★ 추가); `',"name":"product name C","weight":300, "recommendations":[` (★ 추가); `{"recommendationId":1,"author":"author 1","rate":1,"conte...` (★ 추가); `{"recommendationId":2,"author":"author 2","rate":2,"conte...` (★ 추가); `{"recommendationId":3,"author":"author 3","rate":3,"conte...` (★ 추가); `], "reviews":[` (★ 추가); `{"reviewId":1,"author":"author 1","subject":"subject 1","...` (★ 추가); `{"reviewId":2,"author":"author 2","subject":"subject 2","...` (★ 추가); `{"reviewId":3,"author":"author 3","subject":"subject 3","...` (★ 추가); `]}'` (★ 추가)

## 확인: 빌드(테스트에 도커 필요), 컴포즈, 자동 테스트
```bash
cd ~/msa
./gradlew build
```
핵심 서비스 테스트가 Testcontainers로 MongoDB/MySQL 컨테이너를 잠깐 띄우기 때문에 **도커가 켜져 있어야** 하고, 처음엔 이미지를 받느라 몇 분 걸린다. `BUILD SUCCESSFUL`.
```bash
docker compose build
./test-em-all.bash start
```
마지막 줄 `End, all tests OK`. DB까지 뜨느라 4장보다 오래 걸린다.

## DB에 들어간 데이터 직접 보기
```bash
docker compose exec mongodb mongosh product-db --quiet --eval "db.products.find()"
docker compose exec mysql mysql -uuser -p review-db -e "select * from reviews"
```
- 첫 줄: test-em-all이 만든 상품(1, 113, 213).
- 둘째 줄: 비밀번호를 물으면 `pwd`. 상품 1, 113의 리뷰가 나온다.

끝내기:
```bash
docker compose down
```
