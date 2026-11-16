# 01. 게이트웨이 (새 프로젝트 spring-cloud/gateway)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 9장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4나 외부 사이트 때문에 교재 10장과 다르게 쓰는 부분.
> 교재 "스프링 클라우드 게이트웨이 설정" 절.

## 1. settings.gradle
```bash
code settings.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
include ':api'
include ':util'
include ':spring-cloud:eureka-server'
include ':spring-cloud:gateway'  // ★ 추가
```

## 2. build.gradle (새 파일)
```bash
mkdir -p spring-cloud/gateway
code spring-cloud/gateway/build.gradle
```
붙여넣기 (새 파일)
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
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'  // 교재에는 없음 (Gradle 9)
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

| 줄 | 하는 일 |
|---|---|
| `spring-cloud-starter-gateway-server-webflux` | 게이트웨이(리액티브, Netty 기반) |
| `spring-cloud-starter-netflix-eureka-client` | `lb://` 주소를 Eureka로 찾기 + 자기도 등록 |
| `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/gateway/routes` |

## 3. Application (새 파일)
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

`@LoadBalanced WebClient.Builder`는 4절 헬스 체크가 `http://product` 같은 이름으로 부르기 위한 것이다(9장 composite와 같음). 라우트의 `lb://`는 이 빈 없이도 게이트웨이가 알아서 처리한다.

## 4. 헬스 체크 (새 파일)
9장까지 composite에 있던 것을 게이트웨이로 옮긴다. 이제 **composite까지 포함해 4개**를 본다.
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
import org.springframework.boot.health.contributor.*;  // 교재와 다름 (교재: org.springframework.boot.actuate.health.*)
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

**교재와 다름:** 헬스 클래스 패키지가 Boot 4에서 `org.springframework.boot.health.contributor`로 옮겨졌다(7장 composite와 같은 이유).

## 5. application.yml (새 파일)
```bash
mkdir -p spring-cloud/gateway/src/main/resources
code spring-cloud/gateway/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 8080

spring.application.name: gateway

# During development, allow forwarding the X-Forwarded-* and Forwarded headers from any proxies.
# When usd in QA and production, this should be set to a specific IP address or CIDR range, e.g. 10\.0\.0\..* (Java Regular Expression).
# For more information, see https://spring.io/security/cve-2025-41235
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
  uri: http://httpbin:8080  # 교재와 다름 (교재: http://httpstat.us, 지금 응답 없음 → compose의 httpbin 컨테이너)
  predicates:
  - Host=i.feel.lucky:8080
  - Path=/headerrouting/**
  filters:
  - SetPath=/status/200  # 교재와 다름 (교재: /200)

- id: host_route_418
  uri: http://httpbin:8080  # 교재와 다름 (교재: http://httpstat.us, 지금 응답 없음 → compose의 httpbin 컨테이너)
  predicates:
  - Host=im.a.teapot:8080
  - Path=/headerrouting/**
  filters:
  - SetPath=/status/418  # 교재와 다름 (교재: /418)

- id: host_route_501
  uri: http://httpbin:8080  # 교재와 다름 (교재: http://httpstat.us, 지금 응답 없음 → compose의 httpbin 컨테이너)
  predicates:
  - Path=/headerrouting/**
  filters:
  - SetPath=/status/501  # 교재와 다름 (교재: /501)

management.endpoint.gateway.enabled: true
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

| 라우트 ID | 조건 | 보내는 곳 | 하는 일 |
|---|---|---|---|
| `product-composite` | `Path=/product-composite/**` | `lb://product-composite` | 공개 API |
| `product-composite-swagger-ui` | `Path=/openapi/**` | `lb://product-composite` | Swagger UI, API 문서 |
| `eureka-api` | `Path=/eureka/api/{segment}` | eureka:8761 | `SetPath=/eureka/{segment}` → `/eureka/api/apps`가 `/eureka/apps`로 |
| `eureka-web-start` | `Path=/eureka/web` | eureka:8761 | `SetPath=/` → Eureka 첫 화면 |
| `eureka-web-other` | `Path=/eureka/**` | eureka:8761 | 웹 화면이 불러오는 css/js 등 |
| `host_route_200` | `Host=i.feel.lucky:8080` + `Path=/headerrouting/**` | httpbin | `/status/200` |
| `host_route_418` | `Host=im.a.teapot:8080` + 같은 Path | httpbin | `/status/418` (I'm a teapot) |
| `host_route_501` | `Path=/headerrouting/**` 만 | httpbin | 위 둘에 안 맞으면 `/status/501` |

| 그 밖의 설정 | 뜻 |
|---|---|
| `trusted-proxies: ".*"` | 앞단 프록시가 보낸 `X-Forwarded-*`를 믿음 (개발용, 교재 주석 참고) |
| `management.endpoint.gateway.enabled` + `exposure.include: "*"` | `/actuator/gateway/routes`로 라우트 목록 보기 |
| `logging ... org.springframework.cloud.gateway: TRACE` | 어떤 라우트를 탔는지 로그로 보기 |

**교재와 다름 (헤더 라우팅 3개):** 교재는 외부 사이트 `http://httpstat.us/200` 등을 쓰는데 지금 응답하지 않는다. 같은 일을 하는 httpbin을 compose 컨테이너로 띄우고(03) `http://httpbin:8080`, 경로 `/status/200`으로 바꿨다. 그래서 **헤더 라우팅은 compose 안에서만** 동작한다(로컬 실행 시엔 `httpbin` 이름을 못 찾음).

## 6. 테스트 (새 파일)
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

## 7. Dockerfile (새 파일)
```bash
code spring-cloud/gateway/Dockerfile
```
붙여넣기 (새 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /extracted
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
COPY --from=builder /extracted/layers/dependencies/ ./
COPY --from=builder /extracted/layers/spring-boot-loader/ ./
COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
COPY --from=builder /extracted/layers/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 확인 (도커 필요 없음)
VS Code에서 `Shift+Alt+U` 후:
```bash
./gradlew :spring-cloud:gateway:build
```
`BUILD SUCCESSFUL`이면 된다(테스트 1개).
