# 07. 도커 컴포즈에 RabbitMQ 추가, 파티션, Kafka, 전체 테스트

> 표기: `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.
> 교재 "리액티브 마이크로서비스 환경 수동 테스트", "리액티브 마이크로서비스 환경 자동 테스트" 절.

compose 파일 3개를 쓴다.

| 파일 | 브로커 | 인스턴스 | 용도 |
|---|---|---|---|
| `docker-compose.yml` | RabbitMQ | 서비스당 1개 | 기본 |
| `docker-compose-partitions.yml` | RabbitMQ | 핵심 서비스당 **2개**(파티션 0, 1) | 같은 productId 이벤트가 항상 같은 인스턴스로 가는지 |
| `docker-compose-kafka.yml` | **Kafka** (+ ZooKeeper) | 핵심 서비스당 2개 | 브로커만 바꿔도 코드 그대로 도는지 |

## 1. docker-compose.yml (전체)
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
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:  # ★ 추가
        condition: service_healthy  # ★ 추가

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:  # ★ 추가
        condition: service_healthy  # ★ 추가

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:  # ★ 추가
        condition: service_healthy  # ★ 추가

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:  # ★ 추가
      rabbitmq:  # ★ 추가
        condition: service_healthy  # ★ 추가

  mongodb:
    image: mongo:8.0.5
    mem_limit: 512m
    ports:
      - "27017:27017"
    command: mongod
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
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
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60

  # ★ 추가 (rabbitmq 서비스 전체)
  rabbitmq:
    image: rabbitmq:4.0.7-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60
```

| 항목 | 뜻 |
|---|---|
| `rabbitmq:4.0.7-management` | 관리 화면이 들어 있는 이미지. `5672` = 메시지, `15672` = 웹 관리 화면 |
| `depends_on: rabbitmq: service_healthy` | 브로커가 준비된 뒤 서비스 시작. composite도 이제 이벤트를 보내므로 기다린다 |
| `timeout: 10s` | 6장 트러블슈팅. RabbitMQ의 `rabbitmqctl status`도 시작이 느린 명령이라 같이 10초 |

## 2. docker-compose-partitions.yml (새 파일)
```bash
code docker-compose-partitions.yml
```
아래처럼 맞추기 (새 파일)
```yaml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-p1:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation-p1:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review-p1:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned
    depends_on:
      rabbitmq:
        condition: service_healthy

  mongodb:
    image: mongo:8.0.5
    mem_limit: 512m
    ports:
      - "27017:27017"
    command: mongod
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
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
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60

  rabbitmq:
    image: rabbitmq:4.0.7-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60
```

`product`와 `product-p1`이 같은 이미지를 쓰고 프로필만 다르다(`streaming_instance_0` / `_1`). 05/06 yml의 `streaming_partitioned` 설정이 여기서 쓰인다.

## 3. docker-compose-kafka.yml (새 파일)
```bash
code docker-compose-kafka.yml
```
아래처럼 맞추기 (새 파일)
```yaml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_started

  product-p1:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_started

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_started

  recommendation-p1:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_started

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_started

  review-p1:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_started

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,kafka
    depends_on:
      kafka:
        condition: service_started

  mongodb:
    image: mongo:8.0.5
    mem_limit: 512m
    ports:
      - "27017:27017"
    command: mongod
    healthcheck:
      test: echo 'db.runCommand("ping").ok' | mongosh --quiet
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
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
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60

  kafka:
    image: confluentinc/cp-kafka:7.9.0
    restart: always
    mem_limit: 1024m
    ports:
      - "9092:9092"
    environment:
      - KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - KAFKA_BROKER_ID=1
      - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.9.0
    restart: always
    mem_limit: 512m
    ports:
      - "2181:2181"
    environment:
      - ZOOKEEPER_CLIENT_PORT=2181
```

모든 서비스에 `kafka` 프로필이 붙어 05/06 yml의 `spring.cloud.stream.defaultBinder: kafka`가 켜진다. 코드는 그대로다.

## 4. test-em-all.bash (전체)
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

# ★ 추가 (testCompositeCreated, waitForMessageProcessing 함수 두 개)
function testCompositeCreated() {

    # Expect that the Product Composite for productId $PROD_ID_REVS_RECS has been created with three recommendations and three reviews
    if ! assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
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

  # ★ 변경 (전: assertCurl 200, POST는 응답 코드 확인 없음) → 이제 202 Accepted
  assertCurl 202 "curl -X DELETE http://$HOST:$PORT/product-composite/${productId} -s"
  assertEqual 202 $(curl -X POST -s http://$HOST:$PORT/product-composite -H "Content-Type: application/json" --data "$composite" -w "%{http_code}")
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

# ★ 변경 (전: waitForService curl -X DELETE http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND)
waitForService curl http://$HOST:$PORT/actuator/health
setupTestdata

# ★ 추가 (이벤트가 처리될 때까지 기다림)
waitForMessageProcessing

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

| 바뀐 곳 | 이유 |
|---|---|
| `waitForService curl .../actuator/health` | 06의 헬스 체크가 세 핵심 서비스까지 UP이어야 200을 준다 |
| `recreateComposite`의 `202` | 생성/삭제가 이제 `202 Accepted` |
| `waitForMessageProcessing` | POST는 바로 202를 주지만 저장은 **이벤트가 처리된 뒤**다. productId 1이 추천 3개, 리뷰 3개로 조회될 때까지 최대 40번(6초 간격) 다시 확인 |

## 5. 빌드와 구동 (RabbitMQ)
```bash
cd ~/msa
./gradlew build && docker compose build && docker compose up -d
```
- 처음에는 `rabbitmq:4.0.7-management` 이미지를 받아서 오래 걸린다.
- `docker compose ps` → `mongodb`, `mysql`, `rabbitmq`가 `(healthy)`, 나머지가 `Up`이면 된다.

## 6. 테스트 실행
```bash
./test-em-all.bash
```
`Wait for messages to be processed...` 뒤에 `All messages are now processed!`, 마지막에 `End, all tests OK`가 나오면 된다.

## 7. 수동 확인
### 7-1. 헬스 체크
```bash
curl localhost:8080/actuator/health -s | jq .
```
`"status": "UP"`과 함께 `coreServices` 아래 `product`, `recommendation`, `review`가 각각 UP으로 보인다. `rabbit`도 UP.

### 7-2. RabbitMQ 관리 화면 (보기만 하는 단계)
브라우저에서 http://localhost:15672 (`guest` / `guest`). 여기서는 **만들어진 것을 눈으로 확인만** 한다.

**① Exchanges 탭**: `products`, `recommendations`, `reviews`가 있고 Type이 `topic`이면 된다. 하나를 클릭해 **Bindings**를 펼치면 연결된 큐 2개가 보인다(예: `reviews` → `reviews.reviewsGroup`, `reviews.auditGroup`). 그래프는 최근 1분간 메시지가 없으면 0이다.

**② Queues and Streams 탭**: 큐 9개(토픽마다 3개)

| 큐 (products 예) | Messages | Consumers | 뜻 |
|---|---|---|---|
| `products.productsGroup` | 0 | 1 | 서비스가 붙어서 이미 처리함 (05의 `group`) |
| `products.auditGroup` | test-em-all이 보낸 수만큼 | 0 | 아무도 안 읽어서 쌓여 있음 (06의 `required-groups`) |
| `products.productsGroup.dlq` | 0 | 0 | 처리 실패한 메시지 보관함 (05의 DLQ). 0이면 정상 |

### 7-3. 터미널에서 직접 POST/DELETE 해 보기
test-em-all이 해 준 "지우고 다시 만들기"를 `curl`로 한 번 직접 한다. 그 사이 RabbitMQ 화면에서 이벤트가 쌓이는 것을 본다.

`auditGroup`은 아무도 안 읽는 보관함이라 **보낸 이벤트가 전부 쌓인다**. test-em-all만 돌린 직후에는 Ready가 products 6 / recommendations 9 / reviews 9다(상품 3개를 지우고 다시 만듦: products는 3×(DELETE+CREATE), 나머지는 DELETE 3 + CREATE 6).

```bash
body='{"productId":1,"name":"product name C","weight":300, "recommendations":[
 {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
 {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
 {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
], "reviews":[
 {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
 {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
 {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
]}'
curl -X DELETE localhost:8080/product-composite/1 -s -w "%{http_code}\n"
curl -X POST localhost:8080/product-composite -H "Content-Type: application/json" --data "$body" -s -w "%{http_code}\n"
curl localhost:8080/product-composite/1 -s | jq '{productId, recommendations: (.recommendations|length), reviews: (.reviews|length)}'
```
DELETE/POST가 `202`, 조회 결과가 추천 3, 리뷰 3이면 된다. (조회가 바로 안 맞으면 1~2초 뒤 다시: 비동기라서)

RabbitMQ **Queues and Streams** 화면(5초마다 새로고침)에서 auditGroup의 Ready가 늘어난다.

| 큐 | 전 | 후 | 늘어난 이유 |
|---|---|---|---|
| `products.auditGroup` | 6 | 8 | DELETE 1 + CREATE 1 |
| `recommendations.auditGroup` | 9 | 13 | DELETE 1 + CREATE 3 |
| `reviews.auditGroup` | 9 | 13 | DELETE 1 + CREATE 3 |

`...Group` 큐(서비스용)는 서비스가 바로 꺼내 처리하므로 계속 0이다.
보낸 이벤트 내용 보기: 관리 화면 **Queues and Streams** 탭 → `products.auditGroup` 클릭 → 아래 **Get messages** 펼치기 → Ack Mode는 기본값(`Nack message requeue true`, 지우지 않고 보기만) 그대로 → **Get Message(s)**. Payload에 `{"eventType":"CREATE","key":1,"data":{...}}` 같은 JSON이 보인다.

### 7-4. 끝내기
```bash
docker compose down
```

## 8. 파티션 (RabbitMQ, 인스턴스 2개씩)
```bash
export COMPOSE_FILE=docker-compose-partitions.yml
docker compose build && docker compose up -d
./test-em-all.bash
```
`End, all tests OK` 후, 어느 인스턴스가 처리했는지 로그로 본다.
```bash
docker compose logs product product-p1 | grep "Create product with ID"
```
같은 productId는 항상 **같은 인스턴스**(`product-1` 또는 `product-p1-1`)에서만 처리된다. 관리 화면 Queues 탭에는 `products.productsGroup-0`, `products.productsGroup-1`처럼 파티션별 큐가 생긴다.
```bash
docker compose down
unset COMPOSE_FILE
```

## 9. Kafka
```bash
export COMPOSE_FILE=docker-compose-kafka.yml
docker compose build && docker compose up -d
./test-em-all.bash
```
`End, all tests OK` 후 토픽 목록과 내용:
```bash
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic products --from-beginning --timeout-ms 1000
```
`products`, `recommendations`, `reviews` 토픽이 있고, `products`에 이벤트 JSON이 보인다.
```bash
docker compose down
unset COMPOSE_FILE
```
> `unset COMPOSE_FILE`을 잊으면 다음 `docker compose ...`가 계속 그 파일을 쓴다.
