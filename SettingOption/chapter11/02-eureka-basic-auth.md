# 02. Eureka를 HTTP 기본 인증으로 막기

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 10장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Security 7 때문에 교재 11장과 다르게 쓰는 부분.
> 교재 "디스커버리 서버 보호" 절.

## 1. eureka-server
```bash
code spring-cloud/eureka-server/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'se.magnus.springcloud'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)  // 교재와 다름 (교재: 24)
	}
}

repositories {
	mavenCentral()
}

ext {
	springCloudVersion = '2025.1.4'  // 교재와 다름 (교재: 2025.0.0, 7장부터)
}

jar {
	enabled = false
}

dependencies {
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
	implementation 'org.springframework.boot:spring-boot-starter-security'  // ★ 추가
	implementation 'org.glassfish.jaxb:jaxb-runtime'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-restclient-test'  // 교재에는 없음 (Boot 4: RestTemplate 테스트 지원)
	testImplementation 'org.springframework.boot:spring-boot-resttestclient'  // 교재에는 없음 (Boot 4: TestRestTemplate이 별도 모듈)
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

dependencyManagement {
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

```bash
code spring-cloud/eureka-server/src/main/java/se/magnus/springcloud/eurekaserver/SecurityConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.eurekaserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private final String username;
  private final String password;

  public SecurityConfig(
    @Value("${app.eureka-username}") String username,
    @Value("${app.eureka-password}") String password
  ) {
    this.username = username;
    this.password = password;
  }

  @Bean
  public InMemoryUserDetailsManager userDetailsService() {
    @SuppressWarnings("deprecation")
    UserDetails user = User.withDefaultPasswordEncoder()
      .username(username)
      .password(password)
      .roles("USER")
      .build();
    return new InMemoryUserDetailsManager(user);
  }

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
      .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
```

| 부분 | 뜻 |
|---|---|
| `InMemoryUserDetailsManager` | yml의 `app.eureka-username/password`로 사용자 한 명 |
| `csrf.disable()` | 서비스들이 POST/PUT으로 등록, 하트비트를 보내야 해서 끔 |
| `httpBasic` | 요청마다 `아이디:비밀번호`(Basic 헤더)로 인증 |

```bash
code spring-cloud/eureka-server/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server:
  port: 8761

# ★ 추가 (app 블록): HTTP 기본 인증 계정
app:
  eureka-username: u
  eureka-password: p

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    waitTimeInMsWhenSyncEmpty: 0
    response-cache-update-interval-ms: 5000

management.endpoints.web.exposure.include: "*"
```

```bash
code spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;  // ★ 추가
import org.springframework.boot.resttestclient.TestRestTemplate;  // 교재와 다름 (교재: org.springframework.boot.test.web.client.TestRestTemplate)
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;  // 교재에는 없음 (Boot 4)
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate  // 교재에는 없음 (Boot 4)
class EurekaServerApplicationTests {

  @Test
  void contextLoads() {
  }

  // ★ 추가: yml의 계정으로 HTTP 기본 인증을 붙인 TestRestTemplate을 쓴다
  @Value("${app.eureka-username}")
  private String username;

  @Value("${app.eureka-password}")
  private String password;

  @Autowired
  void setTestRestTemplate(TestRestTemplate testRestTemplate) {
    this.testRestTemplate = testRestTemplate.withBasicAuth(username, password);
  }

  private TestRestTemplate testRestTemplate;  // ★ 변경 (전: @Autowired로 바로 주입)

  @Test
  void catalogLoads() {

    String expectedReponseBody = "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}";
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }

  @Test
  void healthy() {
    String expectedReponseBody = "{\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}";  // 교재와 다름 (교재: {"status":"UP"}, Boot 4는 groups도 보여 줌)
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }
}
```

테스트도 이제 계정이 있어야 `/eureka/apps`를 볼 수 있어서 `withBasicAuth`를 붙인 `TestRestTemplate`을 쓴다.

## 2. 서비스 4개: Eureka 주소에 계정
`defaultZone`을 `http://u:p@서버:8761/eureka/` 모양으로 바꾼다. 바뀌는 곳은 맨 위 `app:` 블록과 `defaultZone` 한 줄이다.
```bash
code microservices/product-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: product

# ★ 변경 (전: app.eureka-server 한 줄): Eureka 계정 추가
app:
  eureka-username: u
  eureka-password: p
  eureka-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"  # ★ 변경 (계정 추가)
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

server.port: 7002
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)

spring.mongodb:  # 교재와 다름 (교재: spring.data.mongodb, 6장부터 유지)
  host: localhost
  port: 27017
  database: product-db

spring.jackson.mapper.allow-final-fields-as-mutators: true

spring.cloud.function.definition: messageProcessor

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: products
    group: productsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

---
spring.config.activate.on-profile: docker

server.port: 8080

app.eureka-server: eureka

spring.mongodb.host: mongodb  # 교재와 다름 (교재: spring.data.mongodb.host, 6장부터 유지)

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: recommendation

# ★ 변경 (전: app.eureka-server 한 줄): Eureka 계정 추가
app:
  eureka-username: u
  eureka-password: p
  eureka-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"  # ★ 변경 (계정 추가)
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

server.port: 7003
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)

spring.mongodb:  # 교재와 다름 (교재: spring.data.mongodb, 6장부터 유지)
  host: localhost
  port: 27017
  database: recommendation-db

spring.jackson.mapper.allow-final-fields-as-mutators: true

spring.cloud.function.definition: messageProcessor

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: recommendations
    group: recommendationsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.springframework.data.mongodb.core.ReactiveMongoTemplate: DEBUG

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

---
spring.config.activate.on-profile: docker

server.port: 8080

app.eureka-server: eureka

spring.mongodb.host: mongodb  # 교재와 다름 (교재: spring.data.mongodb.host, 6장부터 유지)

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

```bash
code microservices/review-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: review

# ★ 변경 (전: app.eureka-server 한 줄): Eureka 계정 추가
app:
  eureka-username: u
  eureka-password: p
  eureka-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"  # ★ 변경 (계정 추가)
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

server.port: 7004
spring.web.error.include-message: always  # 교재와 다름 (Boot 4, 3장부터 유지)

spring.jpa.hibernate.ddl-auto: update

spring.datasource:
  url: jdbc:mysql://localhost/review-db
  username: user
  password: pwd

spring.datasource.hikari.initializationFailTimeout: 60000

spring.jackson.mapper.allow-final-fields-as-mutators: true

spring.cloud.function.definition: messageProcessor

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings.messageProcessor-in-0:
    destination: reviews
    group: reviewsGroup

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true

spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

---
spring.config.activate.on-profile: docker

server.port: 8080

app.eureka-server: eureka

spring.datasource.url: jdbc:mysql://mysql/review-db

spring.rabbitmq.host: rabbitmq

spring.cloud.stream.kafka.binder.brokers: kafka

---
spring.config.activate.on-profile: streaming_partitioned

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  partitioned: true
  instanceCount: 2

---
spring.config.activate.on-profile: streaming_instance_0

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 0

---
spring.config.activate.on-profile: streaming_instance_1

spring.cloud.stream.bindings.messageProcessor-in-0.consumer.instanceIndex: 1

---
spring.config.activate.on-profile: kafka

management.health.rabbit.enabled: false
spring.cloud.stream.defaultBinder: kafka
spring.kafka.bootstrap-servers: kafka:9092
spring.cloud.stream.kafka.binder.replication-factor: 1
```

composite와 gateway의 yml은 바뀌는 곳이 더 있어서 03, 04에서 전체를 보인다.

## 확인 (도커 필요 없음)
```bash
./gradlew :spring-cloud:eureka-server:build
```
`BUILD SUCCESSFUL`이면 된다(테스트 3개).
