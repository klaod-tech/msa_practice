# 09. settings.gradle, compose, test-em-all, 실행

> 시작: 15장까지 따라 한 상태 / 결과: 16장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

- `settings.gradle`: `eureka-server` 제외.
- compose 3개: `eureka` 서비스 삭제.
- `test-em-all.bash`: `USE_K8S=true`면 쿠버네티스 방식(파드 로그, `kubectl exec`)으로 확인한다.

## 9-1. `docker-compose-kafka.yml`
```bash
code docker-compose-kafka.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    image: hands-on/product-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  product-p1:
    build: microservices/product-service
    image: hands-on/product-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  recommendation-p1:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  review:
    build: microservices/review-service
    image: hands-on/review-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_healthy

  review-p1:
    build: microservices/review-service
    image: hands-on/review-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    image: hands-on/product-composite-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,kafka
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      kafka:
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
    healthcheck:
      test: ["CMD-SHELL", "kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1"]
      interval: 5s
      timeout: 10s
      retries: 30
      start_period: 10s

  zookeeper:
    image: confluentinc/cp-zookeeper:7.9.0
    restart: always
    mem_limit: 512m
    ports:
      - "2181:2181"
    environment:
      - ZOOKEEPER_CLIENT_PORT=2181

  # ★ 삭제 시작 (7줄) — 아래 줄들을 지운다
  # ★ 지움: eureka:
  # ★ 지움: build: spring-cloud/eureka-server
  # ★ 지움: mem_limit: 512m
  # ★ 지움: environment:
  # ★ 지움: - SPRING_PROFILES_ACTIVE=docker
  # ★ 지움: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
  # ★ 지움: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
  # ★ 삭제 끝
  gateway:
    build: spring-cloud/gateway
    image: hands-on/gateway  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  auth-server:
    build: spring-cloud/authorization-server
    image: hands-on/auth-server  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost/actuator/health"]  # ★ 변경 (전: test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator...)
      interval: 5s
      timeout: 2s
      retries: 60

  config-server:
    build: spring-cloud/config-server
    image: hands-on/config-server  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${CONFIG_SERVER_ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo

  zipkin-server:
    image: openzipkin/zipkin:3.5.0
    restart: always
    mem_limit: 1024m
    environment:
      - STORAGE_TYPE=mem
    ports:
      - 9411:9411
```

## 9-2. `docker-compose-partitions.yml`
```bash
code docker-compose-partitions.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    image: hands-on/product-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-p1:
    build: microservices/product-service
    image: hands-on/product-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation-p1:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review:
    build: microservices/review-service
    image: hands-on/review-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review-p1:
    build: microservices/review-service
    image: hands-on/review-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    image: hands-on/product-composite-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
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

  rabbitmq:
    image: rabbitmq:4.0.7-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 2s
      retries: 60

  # ★ 삭제 시작 (7줄) — 아래 줄들을 지운다
  # ★ 지움: eureka:
  # ★ 지움: build: spring-cloud/eureka-server
  # ★ 지움: mem_limit: 512m
  # ★ 지움: environment:
  # ★ 지움: - SPRING_PROFILES_ACTIVE=docker
  # ★ 지움: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
  # ★ 지움: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
  # ★ 삭제 끝
  gateway:
    build: spring-cloud/gateway
    image: hands-on/gateway  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  auth-server:
    build: spring-cloud/authorization-server
    image: hands-on/auth-server  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost/actuator/health"]  # ★ 변경 (전: test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator...)
      interval: 5s
      timeout: 2s
      retries: 60

  config-server:
    build: spring-cloud/config-server
    image: hands-on/config-server  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${CONFIG_SERVER_ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo

  zipkin-server:
    image: openzipkin/zipkin:3.5.0
    restart: always
    mem_limit: 1024m
    environment:
      - STORAGE_TYPE=mem
    ports:
      - 9411:9411
```

## 9-3. `docker-compose.yml`
```bash
code docker-compose.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    image: hands-on/product-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review:
    build: microservices/review-service
    image: hands-on/review-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    image: hands-on/product-composite-service  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
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

  rabbitmq:
    image: rabbitmq:4.0.7-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 2s
      retries: 60

  # ★ 삭제 시작 (7줄) — 아래 줄들을 지운다
  # ★ 지움: eureka:
  # ★ 지움: build: spring-cloud/eureka-server
  # ★ 지움: mem_limit: 512m
  # ★ 지움: environment:
  # ★ 지움: - SPRING_PROFILES_ACTIVE=docker
  # ★ 지움: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
  # ★ 지움: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
  # ★ 삭제 끝
  gateway:
    build: spring-cloud/gateway
    image: hands-on/gateway  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  auth-server:
    build: spring-cloud/authorization-server
    image: hands-on/auth-server  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONFIG_SERVER_USR=${CONFIG_SERVER_USR}
      - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD}
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost/actuator/health"]  # ★ 변경 (전: test: ["CMD", "wget", "-O -", "-q", "http://localhost:9999/actuator...)
      interval: 5s
      timeout: 2s
      retries: 60

  config-server:
    build: spring-cloud/config-server
    image: hands-on/config-server  # ★ 추가
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${CONFIG_SERVER_ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo

  zipkin-server:
    image: openzipkin/zipkin:3.5.0
    restart: always
    mem_limit: 1024m
    environment:
      - STORAGE_TYPE=mem
    ports:
      - 9411:9411
```

## 9-4. `settings.gradle`
```bash
code settings.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include ':api'
include ':util'
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
// ★ 삭제: include ':spring-cloud:eureka-server'
include ':spring-cloud:gateway'
include ':spring-cloud:authorization-server'
include ':spring-cloud:config-server'

// See MIGRATION-BOOT4.md, "2-5. 의존성 보안 패치".
def securityPatches = [
    'com.rabbitmq:amqp-client'                                    : '5.37.1',
    'at.yawk.lz4:lz4-java'                                        : '1.11.4',
    'org.xerial.snappy:snappy-java'                               : '1.1.10.9',
    'com.google.guava:guava'                                      : '33.7.2-jre',// Netflix Eureka (archaius, servo)
    'org.apache.httpcomponents:httpclient'                        : '4.5.14',
    'org.apache.httpcomponents.client5:httpclient5'               : '5.6.3',
    'org.apache.httpcomponents.core5:httpcore5'                   : '5.4.3',
    'org.apache.httpcomponents.core5:httpcore5-h2'                : '5.4.3',
    'org.apache.commons:commons-lang3'                            : '3.20.0',
    'io.opentelemetry:opentelemetry-api'                          : '1.67.0',
    'io.opentelemetry:opentelemetry-common'                       : '1.67.0',
    'io.opentelemetry:opentelemetry-context'                      : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk'                          : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-common'                   : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-trace'                    : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-metrics'                  : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-logs'                     : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi': '1.67.0',
    'io.opentelemetry:opentelemetry-exporter-common'              : '1.67.0',
    'io.opentelemetry:opentelemetry-extension-trace-propagators'  : '1.67.0',
    'io.opentelemetry:opentelemetry-exporter-zipkin'              : '1.64.0',
]
gradle.beforeProject { project ->
    project.pluginManager.withPlugin('io.spring.dependency-management') {
        project.dependencyManagement {
            dependencies {
                securityPatches.each { ga, v -> dependency "${ga}:${v}" }
            }
        }
    }
    project.buildscript.configurations.configureEach {
        resolutionStrategy.eachDependency { details ->
            def v = securityPatches["${details.requested.group}:${details.requested.name}".toString()]
            if (v) details.useVersion(v)
        }
    }
}
```

## 9-5. `test-em-all.bash`
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
: ${HOST=localhost}
: ${PORT=8443}
: ${USE_K8S=false}  # ★ 추가
: ${PROD_ID_REVS_RECS=1}
: ${PROD_ID_NOT_FOUND=13}
: ${PROD_ID_NO_RECS=113}
: ${PROD_ID_NO_REVS=213}
: ${SKIP_CB_TESTS=false}
# ★ 추가 시작 (3줄)
: ${NAMESPACE=hands-on}
: ${CONFIG_SERVER_USR=dev-usr}
: ${CONFIG_SERVER_PWD=dev-pwd}
# ★ 추가 끝

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

    if ! assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
    then
        echo -n "FAIL"
        return 1
    fi
    if ! assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
    then
        echo -n "FAIL"
        return 1
    fi
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

function testCircuitBreaker() {

    echo "Start Circuit Breaker tests!"

    # ★ 변경 시작 (전 1줄 → 7줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: assertEqual "CLOSED" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"
    if [[ $USE_K8S == "false" ]]
    then
        EXEC="docker compose exec -T product-composite"
    else
        EXEC="kubectl -n $NAMESPACE exec deploy/product-composite -- "
    fi

    assertEqual "CLOSED" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"
    # ★ 변경 끝

    for ((n=0; n<3; n++))
    do
        assertCurl 500 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
        message=$(echo $RESPONSE | jq -r .message)
        assertEqual "Did not observe any item or terminal signal within 2000ms" "${message:0:57}"
    done

    assertEqual "OPEN" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"  # ★ 변경 (전: assertEqual "OPEN" "$(docker compose exec -T product-composite wget...)

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 404 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND $AUTH -s"
    assertEqual "Product Id: $PROD_ID_NOT_FOUND not found in fallback cache!" "$(echo $RESPONSE | jq -r .message)"

    echo "Will sleep for 10 sec waiting for the CB to go Half Open..."
    sleep 10

    assertEqual "HALF_OPEN" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"  # ★ 변경 (전: assertEqual "HALF_OPEN" "$(docker compose exec -T product-composite...)

    for ((n=0; n<3; n++))
    do
        assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
        assertEqual "product name C" "$(echo "$RESPONSE" | jq -r .name)"
    done

    assertEqual "CLOSED" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"  # ★ 변경 (전: assertEqual "CLOSED" "$(docker compose exec -T product-composite wg...)

    # ★ 변경 시작 (전 3줄 → 3줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    # ★ 전: assertEqual "CLOSED_TO_OPEN"      "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-3].stateTransition)"
    # ★ 전: assertEqual "OPEN_TO_HALF_OPEN"   "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-2].stateTransition)"
    # ★ 전: assertEqual "HALF_OPEN_TO_CLOSED" "$(docker compose exec -T product-composite wget -qO - http://product-composite:8080/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-1].stateTransition)"
    assertEqual "CLOSED_TO_OPEN"      "$($EXEC wget -qO - http://localhost/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-3].stateTransition)"
    assertEqual "OPEN_TO_HALF_OPEN"   "$($EXEC wget -qO - http://localhost/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-2].stateTransition)"
    assertEqual "HALF_OPEN_TO_CLOSED" "$($EXEC wget -qO - http://localhost/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-1].stateTransition)"
    # ★ 변경 끝
}

set -e

echo "Start Tests:" `date`

echo "HOST=${HOST}"
echo "PORT=${PORT}"
echo "USE_K8S=${USE_K8S}"  # ★ 추가
echo "SKIP_CB_TESTS=${SKIP_CB_TESTS}"

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

# ★ 변경 시작 (전 7줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: for i in $(seq 1 30); do
# ★ 전: [ "$(curl -H 'accept:application/json' -k https://u:p@$HOST:$PORT/eureka/api/apps -s | jq ".applications.application | length")" = "6" ] && break
# ★ 전: sleep 2
# ★ 전: done
# ★ 전: assertCurl 200 "curl -H "accept:application/json" -k https://u:p@$HOST:$PORT/eureka/api/apps -s"
# ★ 전: assertEqual 6 $(echo $RESPONSE | jq ".applications.application | length")
# ★ 전: assertCurl 200 "curl -H "accept:application/json" -k https://dev-usr:dev-pwd@$HOST:$PORT/config/product/docker -s"
assertCurl 200 "curl -H "accept:application/json" -k https://$CONFIG_SERVER_USR:$CONFIG_SERVER_PWD@$HOST:$PORT/config/product/docker -s"
# ★ 변경 끝
TEST_VALUE="hello-world"
# ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: ENCRYPTED_VALUE=$(curl -k https://dev-usr:dev-pwd@$HOST:$PORT/config/encrypt --data-urlencode "$TEST_VALUE" -s)
# ★ 전: DECRYPTED_VALUE=$(curl -k https://dev-usr:dev-pwd@$HOST:$PORT/config/decrypt -d $ENCRYPTED_VALUE -s)
ENCRYPTED_VALUE=$(curl -k https://$CONFIG_SERVER_USR:$CONFIG_SERVER_PWD@$HOST:$PORT/config/encrypt --data-urlencode "$TEST_VALUE" -s)
DECRYPTED_VALUE=$(curl -k https://$CONFIG_SERVER_USR:$CONFIG_SERVER_PWD@$HOST:$PORT/config/decrypt -d $ENCRYPTED_VALUE -s)
# ★ 변경 끝
assertEqual "$TEST_VALUE" "$DECRYPTED_VALUE"

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

assertCurl 401 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

READER_ACCESS_TOKEN=$(curl -k https://reader:secret-reader@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read" -s | jq .access_token -r)
echo READER_ACCESS_TOKEN=$READER_ACCESS_TOKEN
READER_AUTH="-H \"Authorization: Bearer $READER_ACCESS_TOKEN\""

assertCurl 200 "curl $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertCurl 403 "curl -X DELETE $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

echo "Swagger/OpenAPI tests"
assertCurl 302 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ksL https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/index.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/oauth2-redirect.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs"
assertEqual "3.1.0" "$(echo $RESPONSE | jq -r .openapi)"
assertEqual "https://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.servers[0].url')"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs.yaml"

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

## 확인 1: 도커 컴포즈 (Eureka 없이)
```bash
cd ~/msa
./gradlew build
docker compose build
./test-em-all.bash start stop
```
마지막 줄 `End, all tests OK`. 끝나면 컨테이너가 내려간다(`stop`). 다음 단계의 minikube가 메모리를 많이 쓰므로 **컴포즈를 꼭 내린 상태에서** 진행한다.

## 확인 2: minikube에 이미지 만들기
```bash
minikube start --driver=docker --container-runtime=docker --cpus=4 --memory=10g
eval $(minikube docker-env)
docker compose build
```
`eval $(minikube docker-env)`: 이 터미널의 `docker` 명령이 내 PC의 도커가 아니라 **minikube 안의 도커**를 쓰게 한다. 그래서 이미지가 minikube 안에 바로 생기고, 쿠버네티스가 따로 받지 않고 쓴다(차트의 `imagePullPolicy: Never`). 이 설정은 이 터미널에서만 유효하다.

## 확인 3: 헬름으로 설치
```bash
for f in kubernetes/helm/components/*; do helm dep up $f; done
for f in kubernetes/helm/environments/*; do helm dep up $f; done
helm install hands-on-dev-env kubernetes/helm/environments/dev-env -n hands-on --create-namespace
kubectl wait --timeout=900s --for=condition=ready pod --all -n hands-on
kubectl get pods -n hands-on
```
- `helm dep up`: 차트마다 `common` 차트를 `charts/` 폴더로 가져온다(`Chart.lock`, `charts/*.tgz`가 생긴다. 직접 만드는 파일이 아니다).
- 파드 11개(auth-server, config-server, gateway, mongodb, mysql, product, product-composite, rabbitmq, recommendation, review, zipkin-server)가 모두 `1/1 Running`이 될 때까지 기다린다(처음엔 몇 분).

## 확인 4: 테스트 (교수님 안내와 다름: WSL)
교수님 안내는 `HOST=$(minikube ip) PORT=30443 USE_K8S=true ./test-em-all.bash`인데, WSL에서는 `minikube ip`에 닿지 않는다(15장). 게이트웨이 서비스를 내 PC의 30443 포트로 연결한다.
```bash
kubectl port-forward -n hands-on service/gateway 30443:443
```
`Forwarding from 127.0.0.1:30443 -> 8443`이 나오고 멈춰 있으면 된다. **새 터미널**을 열어서:
```bash
cd ~/msa
HOST=localhost PORT=30443 USE_K8S=true ./test-em-all.bash
```
마지막 줄 `End, all tests OK`. 다 끝나면 port-forward 터미널에서 `Ctrl+C`.

## 끝내기
```bash
helm uninstall hands-on-dev-env -n hands-on
kubectl delete namespace hands-on
eval $(minikube docker-env -u)
minikube stop
```
`eval $(minikube docker-env -u)`: `docker` 명령을 다시 내 PC의 도커로 돌린다(새 터미널을 열어도 된다).
