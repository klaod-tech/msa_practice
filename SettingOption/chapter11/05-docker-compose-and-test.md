# 05. compose에 auth-server, 테스트, 토큰으로 직접 호출

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 10장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Security 7 때문에 교재 11장과 다르게 쓰는 부분.
> 교재 "자동화 테스트 변경", "보호된 API 테스트" 절.

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
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      rabbitmq:
        condition: service_healthy
      auth-server:  # ★ 추가
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

  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8443:8443"  # ★ 변경 (전: 8080:8080) HTTPS
    depends_on:  # ★ 추가
      auth-server:  # ★ 추가
        condition: service_healthy  # ★ 추가

  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m

  # ★ 추가 (auth-server 서비스 전체): OAuth 2.0 인가 서버
  auth-server:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60
```

| 바뀐 곳 | 이유 |
|---|---|
| `auth-server` 추가 (healthcheck: `wget .../actuator/health`) | 인가 서버. 밖에 포트를 안 열고 게이트웨이로만 |
| gateway `8443:8443`, `depends_on: auth-server` | HTTPS. 인가 서버가 떠야 토큰 검증 설정을 받아 올 수 있다 |
| composite `depends_on: auth-server` | 같은 이유 |

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
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned
    depends_on:
      rabbitmq:
        condition: service_healthy
      auth-server:  # ★ 추가
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

  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8443:8443"  # ★ 변경 (전: 8080:8080) HTTPS
    depends_on:  # ★ 추가
      auth-server:  # ★ 추가
        condition: service_healthy  # ★ 추가

  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m

  # ★ 추가 (auth-server 서비스 전체): OAuth 2.0 인가 서버
  auth-server:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60
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
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,kafka
    depends_on:
      kafka:
        condition: service_started
      auth-server:  # ★ 추가
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

  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8443:8443"  # ★ 변경 (전: 8080:8080) HTTPS
    depends_on:  # ★ 추가
      auth-server:  # ★ 추가
        condition: service_healthy  # ★ 추가

  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m

  # ★ 추가 (auth-server 서비스 전체): OAuth 2.0 인가 서버
  auth-server:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60
```

## 2. test-em-all.bash (전체)
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
: ${HOST=localhost}
: ${PORT=8443}  # ★ 변경 (전: 8080) 게이트웨이 HTTPS
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

# ★ 변경 (이 파일의 모든 호출): http → https, curl에 -k(자체 서명 인증서 허용)와 $AUTH(토큰) 추가
waitForService curl -k https://$HOST:$PORT/actuator/health

# ★ 추가 (3줄): writer 클라이언트로 토큰을 받아 이후 모든 호출에 Bearer로 붙인다
ACCESS_TOKEN=$(curl -k https://writer:secret-writer@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq .access_token -r)
echo ACCESS_TOKEN=$ACCESS_TOKEN
AUTH="-H \"Authorization: Bearer $ACCESS_TOKEN\""

# ★ 변경: https + Eureka 계정(u:p), 앱 6개(auth-server 추가)
assertCurl 200 "curl -H "accept:application/json" -k https://u:p@$HOST:$PORT/eureka/api/apps -s"
assertEqual 6 $(echo $RESPONSE | jq ".applications.application | length")

setupTestdata

waitForMessageProcessing

assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 404 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 422 "curl $AUTH -k https://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

assertCurl 400 "curl $AUTH -k https://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

# ★ 추가 (여기부터 403 확인까지): 토큰 없으면 401, 읽기 권한만 있으면 삭제 403
assertCurl 401 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

READER_ACCESS_TOKEN=$(curl -k https://reader:secret-reader@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read" -s | jq .access_token -r)
echo READER_ACCESS_TOKEN=$READER_ACCESS_TOKEN
READER_AUTH="-H \"Authorization: Bearer $READER_ACCESS_TOKEN\""

assertCurl 200 "curl $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertCurl 403 "curl -X DELETE $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

# ★ 변경 (아래 Swagger 확인들): http → https, curl에 -k
echo "Swagger/OpenAPI tests"
assertCurl 302 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ksL https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/index.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/oauth2-redirect.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs"
assertEqual "3.1.0" "$(echo $RESPONSE | jq -r .openapi)"
assertEqual "https://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.servers[0].url')"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs.yaml"

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
| `PORT=8443`, 모든 주소 `https://`, curl에 `-k` | 게이트웨이가 HTTPS(자체 서명) |
| `ACCESS_TOKEN=$(curl ... writer:secret-writer ... /oauth2/token ...)` | 시작할 때 writer 토큰을 받아 `$AUTH`로 모든 호출에 붙임 |
| Eureka 확인 `https://u:p@...`, 앱 **6개** | 기본 인증, auth-server 추가 |
| 401, 403 확인 | 토큰 없으면 401, reader로 DELETE면 403 |

## 3. 빌드와 구동
```bash
cd ~/msa
./gradlew build && docker compose build && docker compose up -d
./test-em-all.bash
```
처음에 `ACCESS_TOKEN=eyJ...`가 찍히고 마지막에 `End, all tests OK`가 나오면 된다.

## 4. 직접 해 보기
### 4-1. 토큰 받기 (client credentials)
```bash
ACCESS_TOKEN=$(curl -k https://writer:secret-writer@localhost:8443/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq -r .access_token)
echo $ACCESS_TOKEN
```
`eyJ`로 시작하는 긴 문자열이면 된다. 가운데 부분(내용)을 풀어 보면:
```bash
echo $ACCESS_TOKEN | cut -d. -f2 | tr '_-' '/+' | base64 -d 2>/dev/null | jq '{sub, scope, iss, exp}'
```
`"sub": "writer"`, `"scope": ["product:write", "product:read"]`, `"iss": "http://auth-server:9999"`.

### 4-2. 토큰 있이 / 없이
```bash
curl -k https://localhost:8443/product-composite/1 -s -o /dev/null -w "%{http_code}\n"
curl -k https://localhost:8443/product-composite/1 -H "Authorization: Bearer $ACCESS_TOKEN" -s | jq .productId
```
첫 줄 `401`, 둘째 줄 `1`.

### 4-3. reader는 읽기만
```bash
READER_TOKEN=$(curl -k https://reader:secret-reader@localhost:8443/oauth2/token -d grant_type=client_credentials -d scope="product:read" -s | jq -r .access_token)
curl -k https://localhost:8443/product-composite/1 -H "Authorization: Bearer $READER_TOKEN" -s -o /dev/null -w "%{http_code}\n"
curl -k -X DELETE https://localhost:8443/product-composite/1 -H "Authorization: Bearer $READER_TOKEN" -s -o /dev/null -w "%{http_code}\n"
```
`200`, `403`.

### 4-4. 누가 불렀는지 로그로
```bash
docker compose logs product-composite | grep "Authorization info" | tail -2
```

### 4-5. Eureka
```bash
curl -k https://localhost:8443/eureka/api/apps -H "accept:application/json" -s -o /dev/null -w "%{http_code}\n"
curl -k https://u:p@localhost:8443/eureka/api/apps -H "accept:application/json" -s | jq -r '.applications.application[].name'
```
첫 줄 `401`, 둘째는 앱 6개(AUTH-SERVER 포함). 브라우저로는 https://localhost:8443/eureka/web → 인증서 경고에서 진행 → 아이디 `u`, 비밀번호 `p`.

### 4-6. Swagger UI에서 로그인해서 호출 (authorization code)
1. 브라우저에서 https://localhost:8443/openapi/swagger-ui.html (인증서 경고 → 고급 → 계속)
2. 오른쪽 위 **Authorize** → 창에서 scope `product:read`, `product:write` 체크 → **Authorize**
3. 인가 서버 로그인 화면: `u` / `p` → 동의 화면에서 scope 체크 후 **Submit Consent**
4. 다시 Swagger로 돌아와 **Close** → GET을 **Try it out**으로 실행 → 200

## 5. 끝내기
```bash
docker compose down
```
인가 서버는 시작할 때마다 서명 키를 새로 만든다. 다시 올리면 **예전 토큰은 못 쓴다**(새로 받는다).

다음: [06-auth0.md](06-auth0.md)
