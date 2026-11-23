# 01. 새 프로젝트: spring-cloud/gateway

> 시작: 교수님 `Chapter09` 상태 / 결과: 교수님 `Chapter10`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `build.gradle`: `spring-cloud-starter-gateway-server-webflux`(Boot 4용 게이트웨이 이름), Eureka 클라이언트.
- `application.yml`: **라우트** 목록. 라우트 = id + 보낼 곳(`uri`) + 조건(`predicates`) + 필터(`filters`).

| 라우트 | 조건 | 보낼 곳 |
|---|---|---|
| `product-composite` | `/product-composite/**` | `lb://product-composite` (Eureka로 찾아 로드 밸런싱) |
| `product-composite-swagger-ui` | `/openapi/**` | composite의 Swagger |
| `eureka-api`, `eureka-web-start`, `eureka-web-other` | `/eureka/api/...`, `/eureka/web`, `/eureka/**` | Eureka 서버 (경로를 바꿔서) |
| `host_route_200/418/501` | `Host` 헤더 + `/headerrouting/**` | httpstat.us 예제 (지금 응답 없음, [troubleshooting](troubleshooting.md)) |

- `HealthCheckConfiguration`: composite에 있던 "핵심 서비스 상태 묶어 보기"를 게이트웨이로 옮겼다.

## 1-1. `spring-cloud/gateway/build.gradle` (새 파일)
```bash
mkdir -p spring-cloud/gateway
code spring-cloud/gateway/build.gradle
```
붙여넣기 (새 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.springcloud'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ext {
    springCloudVersion = '2025.1.4'
}

jar {
    enabled = false
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-webclient'
    implementation 'org.springframework.boot:spring-boot-starter-restclient'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
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

## 1-2. `spring-cloud/gateway/settings.gradle` (새 파일)
```bash
code spring-cloud/gateway/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'gateway'
```

## 1-3. `spring-cloud/gateway/.gitignore` (새 파일)
```bash
code spring-cloud/gateway/.gitignore
```
붙여넣기 (새 파일)
```
HELP.md
.gradle
/build/
!gradle/wrapper/gradle-wrapper.jar

.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

.idea
*.iws
*.iml
*.ipr
/out/
bin

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
```

## 1-4. `spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/GatewayApplication.java` (새 파일)
```bash
mkdir -p spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/GatewayApplication.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class GatewayApplication {

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
  }

  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

}
```

## 1-5. `spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/HealthCheckConfiguration.java` (새 파일)
```bash
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/HealthCheckConfiguration.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.gateway;

import static java.util.logging.Level.FINE;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class HealthCheckConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(HealthCheckConfiguration.class);

  private WebClient webClient;

  public HealthCheckConfiguration(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  @Bean
  ReactiveHealthContributor healthcheckMicroservices() {

    final Map<String, ReactiveHealthIndicator> registry = new LinkedHashMap<>();

    registry.put("product",           () -> getHealth("http://product"));
    registry.put("recommendation",    () -> getHealth("http://recommendation"));
    registry.put("review",            () -> getHealth("http://review"));
    registry.put("product-composite", () -> getHealth("http://product-composite"));

    return CompositeReactiveHealthContributor.fromMap(registry);
  }

  private Mono<Health> getHealth(String baseUrl) {
    String url = baseUrl + "/actuator/health";
    LOG.debug("Setting up a call to the Health API on URL: {}", url);
    return webClient.get().uri(url).retrieve().bodyToMono(String.class)
      .map(s -> new Health.Builder().up().build())
      .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
      .log(LOG.getName(), FINE);
  }

}
```

## 1-6. `spring-cloud/gateway/src/main/resources/application.yml` (새 파일)
```bash
mkdir -p spring-cloud/gateway/src/main/resources
code spring-cloud/gateway/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 8080

spring.application.name: gateway

spring.cloud.gateway.server.webflux.trusted-proxies: ".*"

app.eureka-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: http://${app.eureka-server}:8761/eureka/
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

spring.cloud.gateway.server.webflux.routes:

- id: product-composite
  uri: lb://product-composite
  predicates:
  - Path=/product-composite/**

- id: product-composite-swagger-ui
  uri: lb://product-composite
  predicates:
  - Path=/openapi/**

- id: eureka-api
  uri: http://${app.eureka-server}:8761
  predicates:
  - Path=/eureka/api/{segment}
  filters:
  - SetPath=/eureka/{segment}

- id: eureka-web-start
  uri: http://${app.eureka-server}:8761
  predicates:
  - Path=/eureka/web
  filters:
  - SetPath=/

- id: eureka-web-other
  uri: http://${app.eureka-server}:8761
  predicates:
  - Path=/eureka/**

- id: host_route_200
  uri: http://httpstat.us
  predicates:
  - Host=i.feel.lucky:8080
  - Path=/headerrouting/**
  filters:
  - SetPath=/200

- id: host_route_418
  uri: http://httpstat.us
  predicates:
  - Host=im.a.teapot:8080
  - Path=/headerrouting/**
  filters:
  - SetPath=/418

- id: host_route_501
  uri: http://httpstat.us
  predicates:
  - Path=/headerrouting/**
  filters:
  - SetPath=/501

management.endpoint.gateway.access: unrestricted
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

logging:
  level:
    root: INFO
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: INFO
    org.springframework.cloud.gateway: TRACE

---
spring.config.activate.on-profile: docker

app.eureka-server: eureka
```

## 1-7. `spring-cloud/gateway/src/test/java/se/magnus/springcloud/gateway/GatewayApplicationTests.java` (새 파일)
```bash
mkdir -p spring-cloud/gateway/src/test/java/se/magnus/springcloud/gateway
code spring-cloud/gateway/src/test/java/se/magnus/springcloud/gateway/GatewayApplicationTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.gateway;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"eureka.client.enabled=false"})
class GatewayApplicationTests {

  @Test
  void contextLoads() {
  }

}
```

## 1-8. `spring-cloud/gateway/Dockerfile` (새 파일)
```bash
code spring-cloud/gateway/Dockerfile
```
붙여넣기 (새 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /builder
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

다음: [02-composite.md](02-composite.md)
