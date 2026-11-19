# 04. compose에 설정 서버 추가, 테스트

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 11장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Cloud 2025.1 때문에 교재 12장과 다르게 쓰는 부분.
> 교재 "스프링 클라우드 컨피그 서버 사용해보기" 절.

## 1. docker-compose.yml
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    depends_on:
      rabbitmq:
        condition: service_healthy
      auth-server:
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
    environment:  # ★ 추가 (environment 블록)
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}

  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m

  auth-server:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60

  # ★ 추가 (config-server 서비스 전체): 설정 서버
  config-server:
    build: spring-cloud/config-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${CONFIG_SERVER_ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
```

| 바뀐 곳 | 이유 |
|---|---|
| 서비스마다 `CONFIG_SERVER_USR`, `CONFIG_SERVER_PWD` | bootstrap.yml의 `${CONFIG_SERVER_USR}` 자리에 들어갈 값. `.env`에서 온다 |
| eureka에 `environment` 블록 | 11장까지 eureka는 프로필이 필요 없었다. 이제 docker 프로필이어야 설정 서버 주소가 `config-server:8888`이 된다 |
| `config-server` 서비스 | `docker,native` 프로필(로컬 폴더 모드), 암호화 키와 로그인 계정은 `.env`에서 |
| `volumes: $PWD/config-repo:/config-repo` | 내 PC의 `~/msa/config-repo` 폴더를 컨테이너 안 `/config-repo`로 붙인다. 그래서 **`~/msa`에서 compose를 실행해야** 한다(`$PWD` = 현재 폴더) |

설정 서버에는 `depends_on`/healthcheck를 걸지 않았다(교재와 같음). 다른 서비스가 bootstrap.yml의 retry로 기다린다.

## 2. docker-compose-partitions.yml
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    depends_on:
      rabbitmq:
        condition: service_healthy
      auth-server:
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
    environment:  # ★ 추가 (environment 블록)
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}

  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m

  auth-server:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60

  # ★ 추가 (config-server 서비스 전체): 설정 서버
  config-server:
    build: spring-cloud/config-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${CONFIG_SERVER_ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
```

## 3. docker-compose-kafka.yml
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
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
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    depends_on:
      kafka:
        condition: service_started
      auth-server:
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
    environment:  # ★ 추가 (environment 블록)
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}

  gateway:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    build: spring-cloud/gateway
    mem_limit: 512m
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m

  auth-server:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}  # ★ 추가
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}  # ★ 추가
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 10s  # 교재와 다름 (교재: 2s, 6장 트러블슈팅)
      retries: 60

  # ★ 추가 (config-server 서비스 전체): 설정 서버
  config-server:
    build: spring-cloud/config-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${CONFIG_SERVER_ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
```

## 4. test-em-all.bash
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

waitForService curl -k https://$HOST:$PORT/actuator/health

ACCESS_TOKEN=$(curl -k https://writer:secret-writer@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq .access_token -r)
echo ACCESS_TOKEN=$ACCESS_TOKEN
AUTH="-H \"Authorization: Bearer $ACCESS_TOKEN\""

# Verify access to Eureka and that all four microservices are registered in Eureka
assertCurl 200 "curl -H "accept:application/json" -k https://u:p@$HOST:$PORT/eureka/api/apps -s"
assertEqual 6 $(echo $RESPONSE | jq ".applications.application | length")

# ★ 추가 (6줄): 설정 서버 접근, 암호화/복호화 확인
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

if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi

echo "End, all tests OK:" `date`
```

추가된 6줄: 게이트웨이를 거쳐 설정 서버에서 product의 docker 설정을 읽고(200), `hello-world`를 암호화했다가 다시 풀어서 같은지 본다. Eureka 앱 수는 그대로 6개다(설정 서버는 Eureka에 등록하지 않는다).

## 5. 빌드와 구동
```bash
cd ~/msa
./gradlew build && docker compose build && docker compose up -d
./test-em-all.bash
```
처음에 서비스들이 설정 서버를 기다리느라 11장보다 조금 늦게 뜬다. 마지막에 `End, all tests OK`가 나오면 된다.

## 6. 직접 해 보기
### 6-1. 서비스가 설정 서버에서 설정을 받았는지
```bash
docker compose logs product | grep -E "Fetching config|Located environment"
```
`Fetching config from server at : http://config-server:8888`, `Located environment: name=product, profiles=[docker]`.

### 6-2. 설정 서버가 내주는 설정 보기 (게이트웨이 경유)
```bash
curl -k https://dev-usr:dev-pwd@localhost:8443/config/product/docker -s | jq -r '.propertySources[].name'
```
`product.yml`(docker 부분, 기본 부분)과 `application.yml`(docker 부분, 기본 부분)이 나온다. 위에 있을수록 우선한다.
```bash
curl -k https://localhost:8443/config/product/docker -s -o /dev/null -w "%{http_code}\n"
```
계정 없이 부르면 `401`.

### 6-3. 암호화 / 복호화
```bash
curl -k https://dev-usr:dev-pwd@localhost:8443/config/encrypt --data-urlencode "hello world"
```
긴 16진수 문자열(암호문)이 나온다. 그 값을 그대로 넣어 푼다:
```bash
curl -k https://dev-usr:dev-pwd@localhost:8443/config/decrypt -d 위에서_나온_암호문
```
`hello world`. 설정 파일의 `'{cipher}...'`도 설정 서버가 이렇게 풀어서 서비스에 넘긴다.
```bash
curl -k https://dev-usr:dev-pwd@localhost:8443/config/review/docker -s | jq -r '.propertySources[].source["spring.datasource.password"] // empty'
```
`pwd`. review.yml에는 암호문으로 적었지만 받는 쪽에는 평문으로 간다.

## 7. 끝내기
```bash
docker compose down
```

다음 장: [../chapter13/README.md](../chapter13/README.md)
