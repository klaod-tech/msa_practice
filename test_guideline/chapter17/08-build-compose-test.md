# 08. .env, compose, settings.gradle, test-em-all, 실행

> 시작: 교수님 `Chapter16` 상태 / 결과: 교수님 `Chapter17`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `.env`: 설정 서버 계정 대신 RabbitMQ·MongoDB·MySQL 계정과 게이트웨이 인증서 비밀번호(compose가 DB와 브로커를 띄울 때 사용).
- `settings.gradle`: `config-server` 제외.
- compose: `config-server` 삭제. 서비스는 `config-repo`를 볼륨으로 붙이고 `SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/<서비스>.yml`로 직접 읽는다.
- `test-em-all.bash`: 쿠버네티스에서는 Ingress(`HOST=minikube.me`)로 접속.

## 8-1. `.env`
```bash
code .env
```
아래처럼 맞추기 (전체 파일)
```properties
# ★ 변경 (8줄, 전: 3줄)
RABBITMQ_USR=rabbit-user-prod
RABBITMQ_PWD=rabbit-pwd-prod

MONGODB_USR=mongodb-user-prod
MONGODB_PWD=mongodb-pwd-prod

MYSQL_ROOT_PWD=rootpwd
MYSQL_USR=mysql-user-prod
MYSQL_PWD=mysql-pwd-prod

GATEWAY_TLS_PWD=password
```

## 8-2. `docker-compose-kafka.yml`
```bash
code docker-compose-kafka.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    image: hands-on/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
      # ★ 변경 (6줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product.yml
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  product-p1:
    build: microservices/product-service
    image: hands-on/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
      # ★ 변경 (6줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product.yml
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
      # ★ 변경 (6줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/recommendation.yml
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  recommendation-p1:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
      # ★ 변경 (6줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/recommendation.yml
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      kafka:
        condition: service_healthy

  review:
    build: microservices/review-service
    image: hands-on/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0,kafka
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/review.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USR}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PWD}  # ★ 추가
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_healthy

  review-p1:
    build: microservices/review-service
    image: hands-on/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1,kafka
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/review.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USR}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PWD}  # ★ 추가
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    image: hands-on/product-composite-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,kafka
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product-composite.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
    volumes:  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - $PWD/config-repo:/config-repo  # ★ 추가
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
    environment:  # ★ 추가
      - MONGO_INITDB_ROOT_USERNAME=${MONGODB_USR}  # ★ 추가
      - MONGO_INITDB_ROOT_PASSWORD=${MONGODB_PWD}  # ★ 추가
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
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PWD}  # ★ 변경 (전: - MYSQL_ROOT_PASSWORD=rootpwd)
      - MYSQL_DATABASE=review-db
      - MYSQL_USER=${MYSQL_USR}  # ★ 변경 (전: - MYSQL_USER=user)
      - MYSQL_PASSWORD=${MYSQL_PWD}  # ★ 변경 (전: - MYSQL_PASSWORD=pwd)
    healthcheck:
      test: "/usr/bin/mysql --user=${MYSQL_USR} --password=${MYSQL_PWD} --execute \"SHOW DATABASES;\""  # ★ 변경 (전: test: "/usr/bin/mysql --user=user --password=pwd --execute \"SHOW D...)
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

  gateway:
    build: spring-cloud/gateway
    image: hands-on/gateway
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/gateway.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_SSL_BUNDLE_JKS_GATEWAY_KEYSTORE_PASSWORD=${GATEWAY_TLS_PWD}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  auth-server:
    build: spring-cloud/authorization-server
    image: hands-on/auth-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/auth-server.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
    volumes:  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - $PWD/config-repo:/config-repo  # ★ 추가
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost/actuator/health"]
      interval: 5s
      timeout: 2s
      retries: 60
# ★ 삭제 (11줄, 첫 줄: config-server:)

  zipkin-server:
    image: openzipkin/zipkin:3.5.0
    restart: always
    mem_limit: 1024m
    environment:
      - STORAGE_TYPE=mem
    ports:
      - 9411:9411
```

## 8-3. `docker-compose-partitions.yml`
```bash
code docker-compose-partitions.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    image: hands-on/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
      # ★ 변경 (8줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-p1:
    build: microservices/product-service
    image: hands-on/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
      # ★ 변경 (8줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
      # ★ 변경 (8줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/recommendation.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation-p1:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
      # ★ 변경 (8줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/recommendation.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review:
    build: microservices/review-service
    image: hands-on/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0
      # ★ 변경 (7줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/review.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USR}
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review-p1:
    build: microservices/review-service
    image: hands-on/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1
      # ★ 변경 (7줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/review.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USR}
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    image: hands-on/product-composite-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,streaming_partitioned
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product-composite.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}  # ★ 추가
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
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
    environment:  # ★ 추가
      - MONGO_INITDB_ROOT_USERNAME=${MONGODB_USR}  # ★ 추가
      - MONGO_INITDB_ROOT_PASSWORD=${MONGODB_PWD}  # ★ 추가
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
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PWD}  # ★ 변경 (전: - MYSQL_ROOT_PASSWORD=rootpwd)
      - MYSQL_DATABASE=review-db
      - MYSQL_USER=${MYSQL_USR}  # ★ 변경 (전: - MYSQL_USER=user)
      - MYSQL_PASSWORD=${MYSQL_PWD}  # ★ 변경 (전: - MYSQL_PASSWORD=pwd)
    healthcheck:
      test: "/usr/bin/mysql --user=${MYSQL_USR} --password=${MYSQL_PWD} --execute \"SHOW DATABASES;\""  # ★ 변경 (전: test: "/usr/bin/mysql --user=user --password=pwd --execute \"SHOW D...)
      interval: 5s
      timeout: 2s
      retries: 60

  rabbitmq:
    image: rabbitmq:4.0.7-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    environment:  # ★ 추가
      - RABBITMQ_DEFAULT_USER=${RABBITMQ_USR}  # ★ 추가
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PWD}  # ★ 추가
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 2s
      retries: 60

  gateway:
    build: spring-cloud/gateway
    image: hands-on/gateway
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      # ★ 변경 (6줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/gateway.yml
      - SPRING_SSL_BUNDLE_JKS_GATEWAY_KEYSTORE_PASSWORD=${GATEWAY_TLS_PWD}
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  auth-server:
    build: spring-cloud/authorization-server
    image: hands-on/auth-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/auth-server.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}  # ★ 추가
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost/actuator/health"]
      interval: 5s
      timeout: 2s
      retries: 60
# ★ 삭제 (11줄, 첫 줄: config-server:)

  zipkin-server:
    image: openzipkin/zipkin:3.5.0
    restart: always
    mem_limit: 1024m
    environment:
      - STORAGE_TYPE=mem
    ports:
      - 9411:9411
```

## 8-4. `docker-compose.yml`
```bash
code docker-compose.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
services:
  product:
    build: microservices/product-service
    image: hands-on/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      # ★ 변경 (8줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  recommendation:
    build: microservices/recommendation-service
    image: hands-on/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      # ★ 변경 (8줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/recommendation.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_MONGODB_AUTHENTICATION_DATABASE=admin
      - SPRING_MONGODB_USERNAME=${MONGODB_USR}
      - SPRING_MONGODB_PASSWORD=${MONGODB_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mongodb:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  review:
    build: microservices/review-service
    image: hands-on/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      # ★ 변경 (7줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/review.yml
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
      - SPRING_DATASOURCE_USERNAME=${MYSQL_USR}
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    depends_on:
      mysql:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy

  product-composite:
    build: microservices/product-composite-service
    image: hands-on/product-composite-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/product-composite.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}  # ★ 추가
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
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
    environment:  # ★ 추가
      - MONGO_INITDB_ROOT_USERNAME=${MONGODB_USR}  # ★ 추가
      - MONGO_INITDB_ROOT_PASSWORD=${MONGODB_PWD}  # ★ 추가
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
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PWD}  # ★ 변경 (전: - MYSQL_ROOT_PASSWORD=rootpwd)
      - MYSQL_DATABASE=review-db
      - MYSQL_USER=${MYSQL_USR}  # ★ 변경 (전: - MYSQL_USER=user)
      - MYSQL_PASSWORD=${MYSQL_PWD}  # ★ 변경 (전: - MYSQL_PASSWORD=pwd)
    healthcheck:
      test: "/usr/bin/mysql --user=${MYSQL_USR} --password=${MYSQL_PWD} --execute \"SHOW DATABASES;\""  # ★ 변경 (전: test: "/usr/bin/mysql --user=user --password=pwd --execute \"SHOW D...)
      interval: 5s
      timeout: 2s
      retries: 60

  rabbitmq:
    image: rabbitmq:4.0.7-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    environment:  # ★ 추가
      - RABBITMQ_DEFAULT_USER=${RABBITMQ_USR}  # ★ 추가
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PWD}  # ★ 추가
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 2s
      retries: 60

  gateway:
    build: spring-cloud/gateway
    image: hands-on/gateway
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      # ★ 변경 (6줄, 전: 2줄)
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/gateway.yml
      - SPRING_SSL_BUNDLE_JKS_GATEWAY_KEYSTORE_PASSWORD=${GATEWAY_TLS_PWD}
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
    ports:
      - "8443:8443"
    depends_on:
      auth-server:
        condition: service_healthy

  auth-server:
    build: spring-cloud/authorization-server
    image: hands-on/auth-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CONFIG_LOCATION=file:/config-repo/application.yml,file:/config-repo/auth-server.yml  # ★ 변경 (전: - CONFIG_SERVER_USR=${CONFIG_SERVER_USR})
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USR}  # ★ 변경 (전: - CONFIG_SERVER_PWD=${CONFIG_SERVER_PWD})
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PWD}  # ★ 추가
    volumes:  # ★ 추가
      - $PWD/config-repo:/config-repo  # ★ 추가
    healthcheck:
      test: ["CMD", "wget", "-O -", "-q", "http://localhost/actuator/health"]
      interval: 5s
      timeout: 2s
      retries: 60
# ★ 삭제 (11줄, 첫 줄: config-server:)

  zipkin-server:
    image: openzipkin/zipkin:3.5.0
    restart: always
    mem_limit: 1024m
    environment:
      - STORAGE_TYPE=mem
    ports:
      - 9411:9411
```

## 8-5. `settings.gradle`
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
include ':spring-cloud:gateway'
include ':spring-cloud:authorization-server'
// ★ 삭제: include ':spring-cloud:config-server'

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

## 8-6. `test-em-all.bash`
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
: ${HOST=localhost}
: ${PORT=8443}
: ${USE_K8S=false}
: ${PROD_ID_REVS_RECS=1}
: ${PROD_ID_NOT_FOUND=13}
: ${PROD_ID_NO_RECS=113}
: ${PROD_ID_NO_REVS=213}
: ${SKIP_CB_TESTS=false}
: ${NAMESPACE=hands-on}
# ★ 삭제 (2줄, 첫 줄: : ${CONFIG_SERVER_USR=dev-usr})

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

    if [[ $USE_K8S == "false" ]]
    then
        EXEC="docker compose exec -T product-composite"
    else
        EXEC="kubectl -n $NAMESPACE exec deploy/product-composite -- "
    fi

    assertEqual "CLOSED" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    for ((n=0; n<3; n++))
    do
        assertCurl 500 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
        message=$(echo $RESPONSE | jq -r .message)
        assertEqual "Did not observe any item or terminal signal within 2000ms" "${message:0:57}"
    done

    assertEqual "OPEN" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 404 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND $AUTH -s"
    assertEqual "Product Id: $PROD_ID_NOT_FOUND not found in fallback cache!" "$(echo $RESPONSE | jq -r .message)"

    echo "Will sleep for 10 sec waiting for the CB to go Half Open..."
    sleep 10

    assertEqual "HALF_OPEN" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    for ((n=0; n<3; n++))
    do
        assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
        assertEqual "product name C" "$(echo "$RESPONSE" | jq -r .name)"
    done

    assertEqual "CLOSED" "$($EXEC wget -qO - http://localhost/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    assertEqual "CLOSED_TO_OPEN"      "$($EXEC wget -qO - http://localhost/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-3].stateTransition)"
    assertEqual "OPEN_TO_HALF_OPEN"   "$($EXEC wget -qO - http://localhost/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-2].stateTransition)"
    assertEqual "HALF_OPEN_TO_CLOSED" "$($EXEC wget -qO - http://localhost/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-1].stateTransition)"
}

set -e

echo "Start Tests:" `date`

echo "HOST=${HOST}"
echo "PORT=${PORT}"
echo "USE_K8S=${USE_K8S}"
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

# ★ 삭제 (5줄, 첫 줄: assertCurl 200 "curl -H "accept:application/jso...)
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
if [[ $USE_K8S == "false" ]]  # ★ 변경 (전: assertEqual "https://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.serve...)
then  # ★ 추가
  assertEqual "https://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.servers[0].url')"  # ★ 추가
fi  # ★ 추가
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

## 확인 1: 도커 컴포즈 (설정 서버 없이)
```bash
cd ~/msa
./gradlew build
docker compose build
./test-em-all.bash start stop
```
마지막 줄 `End, all tests OK`. minikube를 쓰기 전에 컴포즈는 내려가 있어야 한다(`stop`).

## 확인 2: minikube 준비 (Ingress, cert-manager)
```bash
minikube start --driver=docker --container-runtime=docker --cpus=4 --memory=10g
minikube addons enable ingress
helm repo add jetstack https://charts.jetstack.io
helm install cert-manager jetstack/cert-manager --create-namespace -n cert-manager --set crds.enabled=true --wait
```
- `minikube addons enable ingress`: Ingress 규칙을 실제로 처리하는 ingress-nginx 컨트롤러를 설치한다.
- cert-manager: 인증서를 만들어 Secret에 넣어 준다. `--wait`라 다 뜰 때까지 기다린다(1~2분).
- 16장에서 만든 클러스터가 남아 있으면 `helm uninstall hands-on-dev-env -n hands-on`부터 한다.

## 확인 3: 이미지와 헬름 설치
```bash
eval $(minikube docker-env)
docker compose build
for f in kubernetes/helm/components/*; do helm dep up $f; done
for f in kubernetes/helm/environments/*; do helm dep up $f; done
helm install hands-on-dev-env kubernetes/helm/environments/dev-env -n hands-on --create-namespace
kubectl wait --timeout=900s --for=condition=ready pod --all -n hands-on
kubectl get pods -n hands-on
kubectl get ingress,certificate -n hands-on
```
파드 9개(config-server, gateway가 빠졌다)가 `1/1 Running`, Ingress의 HOSTS가 `minikube.me`, Certificate의 READY가 `True`.

## 확인 4: 테스트 (교수님 안내와 다름: WSL)
교수님 안내는 `/etc/hosts`에 `$(minikube ip) minikube.me`를 넣고 `HOST=minikube.me PORT=443`인데, WSL에서는 `minikube ip`에 닿지 않는다. 대신:
1. `/etc/hosts`에 `127.0.0.1 minikube.me ...` (한 번만, [../README.md](../README.md)의 준비 참고)
2. Ingress 컨트롤러를 내 PC의 8443 포트로 연결한다.
```bash
kubectl port-forward -n ingress-nginx service/ingress-nginx-controller 8443:443
```
`Forwarding from 127.0.0.1:8443 -> 443`이 나오고 멈춰 있으면 된다. **새 터미널**에서:
```bash
cd ~/msa
HOST=minikube.me PORT=8443 USE_K8S=true ./test-em-all.bash
```
마지막 줄 `End, all tests OK`. 끝나면 port-forward 터미널에서 `Ctrl+C`.

## 끝내기
```bash
helm uninstall hands-on-dev-env -n hands-on
kubectl delete namespace hands-on
eval $(minikube docker-env -u)
minikube stop
```
cert-manager와 ingress는 남겨 둬도 된다(18장에서도 cert-manager를 쓴다).
