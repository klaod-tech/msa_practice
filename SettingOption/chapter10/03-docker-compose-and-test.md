# 03. compose에 gateway, httpbin 추가, 테스트, 라우트별 확인

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 9장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4나 외부 사이트 때문에 교재 10장과 다르게 쓰는 부분.
> 교재 "에지 서버 사용해 보기" 절.

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
      rabbitmq:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    # ★ 삭제: ports 8080:8080 (이제 gateway를 거쳐서만 들어온다)
    environment:
      - SPRING_PROFILES_ACTIVE=docker
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

  eureka:
    build: spring-cloud/eureka-server
    mem_limit: 512m
    # ★ 삭제: ports 8761:8761 (웹 화면/API는 gateway의 /eureka/... 로)

  # ★ 추가 (gateway 서비스 전체): 외부에 열린 유일한 입구
  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8080:8080"

  # ★ 추가 (httpbin 서비스 전체) 교재와 다름: 교재는 외부 사이트 httpstat.us (지금 응답 없음)
  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m
```

| 바뀐 곳 | 이유 |
|---|---|
| composite, eureka의 `ports` 삭제 | 밖에서는 게이트웨이로만 들어온다 |
| `gateway` 추가, `8080:8080` | 유일한 입구. 주소가 8080 그대로라 curl 명령은 안 바뀐다 |
| `httpbin` 추가 | 헤더 라우팅 예제의 대상(교재: 외부 사이트 httpstat.us). 밖에 포트를 안 연다 |

파티션, Kafka 파일도 같은 방식이다.
```bash
code docker-compose-partitions.yml
```
아래처럼 맞추기 (전체 파일)
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
    # ★ 삭제: ports 8080:8080 (이제 gateway를 거쳐서만 들어온다)
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

  eureka:
    build: spring-cloud/eureka-server
    mem_limit: 512m
    # ★ 삭제: ports 8761:8761 (웹 화면/API는 gateway의 /eureka/... 로)

  # ★ 추가 (gateway 서비스 전체): 외부에 열린 유일한 입구
  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8080:8080"

  # ★ 추가 (httpbin 서비스 전체) 교재와 다름: 교재는 외부 사이트 httpstat.us (지금 응답 없음)
  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m
```

```bash
code docker-compose-kafka.yml
```
아래처럼 맞추기 (전체 파일)
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
    # ★ 삭제: ports 8080:8080 (이제 gateway를 거쳐서만 들어온다)
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

  eureka:
    build: spring-cloud/eureka-server
    mem_limit: 512m
    # ★ 삭제: ports 8761:8761 (웹 화면/API는 gateway의 /eureka/... 로)

  # ★ 추가 (gateway 서비스 전체): 외부에 열린 유일한 입구
  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8080:8080"

  # ★ 추가 (httpbin 서비스 전체) 교재와 다름: 교재는 외부 사이트 httpstat.us (지금 응답 없음)
  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m
```

## 2. test-em-all.bash (전체)
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

waitForService curl http://$HOST:$PORT/actuator/health

# Verify access to Eureka and that all four microservices are registered in Eureka
# ★ 변경 (전: $HOST:8761/eureka/apps, 4개) → gateway를 거쳐 /eureka/api/apps, gateway까지 5개
assertCurl 200 "curl -H "accept:application/json" $HOST:$PORT/eureka/api/apps -s"
assertEqual 5 $(echo $RESPONSE | jq ".applications.application | length")
setupTestdata

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

Eureka 확인 2줄만 바뀐다: `8761` 대신 **게이트웨이의 `/eureka/api/apps`**, 등록 앱 **5개**(gateway 추가). `waitForService`가 보는 `/actuator/health`도 이제 게이트웨이 것이라, 4개 서비스가 모두 UP이어야 넘어간다.

## 3. 빌드와 구동
```bash
cd ~/msa
./gradlew build && docker compose build && docker compose up -d
./test-em-all.bash
```
마지막에 `End, all tests OK`가 나오면 된다. `docker compose ps`에서 `PORTS`에 `8080`이 보이는 건 **gateway 하나**(DB, RabbitMQ 관리 포트는 이전처럼 열려 있음)인지 본다.

## 4. 라우트별 수동 확인
### 4-1. 라우트 목록
```bash
curl localhost:8080/actuator/gateway/routes -s | jq -r '.[] | "\(.route_id) -> \(.uri)"'
```
`product-composite -> lb://product-composite`, `eureka-api -> http://eureka:8761`, `host_route_200 -> http://httpbin:8080` 등 8줄.

### 4-2. 헬스 체크 (게이트웨이가 모은 것)
```bash
curl localhost:8080/actuator/health -s | jq '.components.healthcheckMicroservices'
```
`product`, `recommendation`, `review`, `product-composite`가 모두 `UP`.

### 4-3. composite API, Swagger
```bash
curl localhost:8080/product-composite/1 -s | jq .
```
브라우저: http://localhost:8080/openapi/swagger-ui.html → 5장의 화면이 게이트웨이 주소로 열린다. **Try it out**으로 GET을 실행했을 때 Request URL이 `http://localhost:8080/product-composite/...`이면 02의 `forward-headers-strategy`가 동작한 것이다.

### 4-4. Eureka
```bash
curl -H "accept:application/json" localhost:8080/eureka/api/apps -s | jq -r '.applications.application[].instance[].instanceId'
```
5줄(gateway 포함). 웹 화면: http://localhost:8080/eureka/web

### 4-5. 헤더(Host) 라우팅
```bash
curl http://localhost:8080/headerrouting -H "Host: i.feel.lucky:8080" -s -o /dev/null -w "%{http_code}\n"
curl http://localhost:8080/headerrouting -H "Host: im.a.teapot:8080" -s -o /dev/null -w "%{http_code}\n"
curl http://localhost:8080/headerrouting -s -o /dev/null -w "%{http_code}\n"
```
차례로 `200`, `418`, `501`. 같은 주소인데 **Host 헤더에 따라 다른 라우트**를 탔다.

### 4-6. 핵심 서비스는 밖에서 못 부른다
```bash
curl localhost:8080/product/1 -s -o /dev/null -w "%{http_code}\n"
```
`404`. 게이트웨이에 `/product/**` 라우트가 없으니 비공개 API는 밖에서 닿지 않는다.

## 5. 끝내기
```bash
docker compose down
```
