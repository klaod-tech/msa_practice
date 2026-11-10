# 07. 도커 컴포즈에 DB 추가, 전체 테스트, DB 직접 조회

> 표기: `★ 추가` / `★ 변경` = 5장 대비. 이 단계의 파일은 **교재 6장과 똑같다**(Boot 4 때문에 바뀌는 것 없음).

## 1. docker-compose.yml (전체)
```bash
code docker-compose.yml
```
기존 내용을 모두 지우고 아래 내용 전체 입력
```yaml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      mongodb:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      mongodb:
        condition: service_healthy

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      mysql:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  # ★ 추가 (mongodb 서비스 전체)
  mongodb:
    image: mongo:8.0.5
    mem_limit: 512m
    ports:
      - "27017:27017"
    command: mongod
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 5s
      timeout: 10s   # ★ 변경 (교재: 2s)
      retries: 60

  # ★ 추가 (mysql 서비스 전체)
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
      timeout: 10s   # ★ 변경 (교재: 2s)
      retries: 60
```

| 항목 | 뜻 |
|---|---|
| `mongodb`, `mysql` 서비스 | DB 컨테이너 2개. **이 서비스 이름이 05 yml docker 프로필의 호스트 이름**(`spring.mongodb.host: mongodb`, `jdbc:mysql://mysql/...`)이 된다 |
| `ports: 27017`, `3306` | 내 PC에서도 DB에 붙을 수 있게 연다(도구로 들여다보기용). 서비스끼리는 포트 공개 없이도 통신된다 |
| `healthcheck` | DB가 **요청을 받을 준비가 됐는지** 5초마다 확인한다 |
| `timeout: 10s` | 검사 명령 한 번이 끝나야 하는 최대 시간. 교재의 2s는 `mongosh`(Node.js 셸) 시작만으로도 넘기기 쉬워서(특히 WSL2) 실패로 판정되고, 60번 연속 실패하면 `unhealthy`가 되어 서비스가 안 뜬다. 10s로 늘려도 검사가 빨리 끝나면 바로 통과하므로 느려지지 않는다 |
| `depends_on ... condition: service_healthy` | DB가 healthy가 된 **뒤에** 서비스를 띄운다. 그냥 `depends_on`만 쓰면 컨테이너가 "시작"만 하면 바로 띄워서, DB가 아직 준비 안 돼 연결에 실패할 수 있다 |
| `MYSQL_DATABASE=review-db`, `MYSQL_USER=user`, `MYSQL_PASSWORD=pwd` | MySQL이 처음 뜰 때 DB와 계정을 만든다. 05 review yml의 값과 맞아야 한다 |

MongoDB는 DB를 미리 만들 필요가 없다. 처음 저장할 때 `product-db`, `recommendation-db`가 자동으로 생긴다.

## 2. test-em-all.bash (전체)
```bash
code test-em-all.bash
```
기존 내용을 모두 지우고 아래 내용 전체 입력
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

# ★ 추가 (recreateComposite, setupTestdata 함수 두 개)
function recreateComposite() {
  local productId=$1
  local composite=$2

  assertCurl 200 "curl -X DELETE http://$HOST:$PORT/product-composite/${productId} -s"
  curl -X POST http://$HOST:$PORT/product-composite -H "Content-Type: application/json" --data "$composite"
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

# ★ 변경 (교재 5장: waitForService curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS)
waitForService curl -X DELETE http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND

# ★ 추가
setupTestdata

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

- `setupTestdata`: 테스트 전에 composite API로 **진짜 데이터를 넣는다**(5장까지는 서비스가 고정 값을 돌려줘서 필요 없었다).

  | productId | 넣는 것 | 확인하는 것 |
  |---|---|---|
  | 1 | 추천 3 + 리뷰 3 | 정상 응답 |
  | 113 | 리뷰 3만 | 추천 0개 |
  | 213 | 추천 3만 | 리뷰 0개 |
- `recreateComposite`: 먼저 DELETE(멱등이라 없어도 200)로 지우고 POST로 다시 넣는다. 그래서 몇 번을 돌려도 같은 결과다.
- `waitForService`가 DELETE로 바뀐 이유: GET은 이제 데이터가 없으면 404라서 "준비됐다"는 신호로 못 쓴다. 없는 productId(13) DELETE는 서비스가 살아 있으면 항상 200이다.

## 3. 빌드와 구동
```bash
cd ~/msa
./gradlew build && docker compose build && docker compose up -d
```
- 처음에는 `mongo:8.0.5`, `mysql:9.2.0` 이미지를 받아서 오래 걸린다.
- 상태 확인: `docker compose ps` → `mongodb`, `mysql`이 `(healthy)`, 나머지가 `Up`이면 된다. MySQL은 healthy까지 시간이 좀 걸리고, 그동안 review는 시작을 기다린다.
- 어느 DB에 붙었는지(05 Application의 로그 줄):
  ```bash
  docker compose logs product | grep "Connected to"
  docker compose logs recommendation | grep "Connected to"
  docker compose logs review | grep "Connected to"
  ```
  `Connected to MongoDb: mongodb:27017`, `Connected to MySQL: jdbc:mysql://mysql/review-db`가 나오면 된다. (`null:null`이면 05의 `spring.mongodb` 변경이 빠진 것)

## 4. 테스트 실행
```bash
./test-em-all.bash
```
마지막에 `End, all tests OK`가 나오면 된다. 어느 줄이 어느 테스트인지 보려면 5장처럼:
```bash
bash -x ./test-em-all.bash 2>&1 | grep -E "^\+ assert|Test OK|Test FAILED|End,"
```

## 5. DB에 저장된 데이터 직접 보기
테스트 스크립트가 넣은 데이터가 **각자의 DB에 따로** 들어갔는지 확인한다.

**product → MongoDB `product-db`** (교재 그림 6.2)
```bash
docker compose exec mongodb mongosh product-db --quiet --eval "db.products.find()"
```
productId `1`, `113`, `213` 세 개가 나오면 된다. `_id`(02의 `@Id`), `version`(`@Version`)도 같이 보인다.

**recommendation → MongoDB `recommendation-db`**
```bash
docker compose exec mongodb mongosh recommendation-db --quiet --eval "db.recommendations.find()"
```
productId 1과 213에 3개씩, 총 6개. 필드 이름이 `rate`가 아니라 **`rating`**(엔티티 이름)인 것도 확인(04의 매퍼).

**review → MySQL `review-db`**
```bash
docker compose exec mysql mysql -uuser -ppwd review-db -e "select * from reviews"
```
productId 1과 113에 3개씩, 총 6줄. (`Using a password on the command line...` 경고는 무시해도 된다)

**MongoDB 안의 DB 목록** (같은 서버에 DB가 따로 있는지)
```bash
docker compose exec mongodb mongosh --quiet --eval "show dbs"
```
`product-db`, `recommendation-db`가 따로 보인다. 한 MongoDB 서버를 같이 쓰지만 **DB는 서비스마다 분리**되어 있다.

## 6. 끝낼 때
```bash
docker compose down
```
DB 컨테이너도 같이 지워지므로 **넣은 데이터도 사라진다**(볼륨을 따로 안 만들었기 때문). 다시 `up` 후 `./test-em-all.bash`를 돌리면 다시 들어간다.
