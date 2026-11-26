# 03. 게이트웨이: HTTPS, JWT 검증, 인가 서버 라우트

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 10장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Security 7 때문에 교재 11장과 다르게 쓰는 부분.
> 교재 "HTTPS를 사용한 외부 통신 보호", "에지 서버 변경" 절.

## 1. HTTPS 인증서 만들기 (edge.p12)
교재는 소스에 들어 있는 `edge.p12`를 쓴다. 같은 설정(별칭 `localhost`, 비밀번호 `password`)으로 직접 만든다. **자체 서명** 인증서라 브라우저와 curl이 "신뢰할 수 없음"으로 본다(curl은 `-k`).
```bash
mkdir -p spring-cloud/gateway/src/main/resources/keystore
keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 -storetype PKCS12 \
  -keystore spring-cloud/gateway/src/main/resources/keystore/edge.p12 -validity 3650 \
  -storepass password -dname "CN=localhost, OU=msa, O=msa, C=KR"
keytool -list -keystore spring-cloud/gateway/src/main/resources/keystore/edge.p12 -storepass password
```
마지막 명령 결과에 `localhost, ..., PrivateKeyEntry`가 보이면 된다. `keytool`은 JDK에 들어 있다.

## 2. build.gradle
```bash
code spring-cloud/gateway/build.gradle
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
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-security'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'  // ★ 추가 (교재: spring-security-oauth2-resource-server, spring-security-oauth2-jose 두 줄 → Boot 4는 이 스타터가 있어야 JWT 검증 자동 설정이 켜짐)
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

**교재와 다름:** 교재는 `spring-security-oauth2-resource-server`, `spring-security-oauth2-jose` 두 줄이다. Boot 4는 자동 설정이 모듈별로 나뉘어서, 이 두 줄만으로는 JWT 검증기(`ReactiveJwtDecoder`)가 만들어지지 않고 게이트웨이가 뜨지 않는다. **`spring-boot-starter-oauth2-resource-server`** 한 줄이 그 둘과 자동 설정을 함께 가져온다.

## 3. SecurityConfig (새 파일)
```bash
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/SecurityConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable()
        .authorizeExchange(exchange -> exchange
          .pathMatchers("/headerrouting/**").permitAll()
          .pathMatchers("/actuator/**").permitAll()
          .pathMatchers("/eureka/**").permitAll()
          .pathMatchers("/oauth2/**").permitAll()
          .pathMatchers("/login/**").permitAll()
          .pathMatchers("/error/**").permitAll()
          .pathMatchers("/openapi/**").permitAll()
          .anyExchange().authenticated()))
      .oauth2ResourceServer(server -> server
        .jwt(Customizer.withDefaults()));
    return http.build();
  }

}
```

| 경로 | 규칙 | 이유 |
|---|---|---|
| `/oauth2/**`, `/login/**`, `/error/**` | 허용 | 토큰을 **받으러** 오는 길이니 토큰 없이 |
| `/eureka/**` | 허용 | 게이트웨이는 통과시키고 Eureka가 직접 기본 인증으로 막는다(02) |
| `/openapi/**`, `/actuator/**`, `/headerrouting/**` | 허용 | 문서, 상태, 예제 |
| 그 밖 (`/product-composite/**`) | **유효한 JWT 필요** | scope 확인은 composite가 한다(04) |

## 4. HealthCheckConfiguration
```bash
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/HealthCheckConfiguration.java
```
아래처럼 맞추기 (전체 파일)
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
    registry.put("auth-server",       () -> getHealth("http://auth-server"));  // ★ 추가

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

## 5. application.yml
```bash
code spring-cloud/gateway/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 8443  # ★ 변경 (전: 8080) HTTPS

spring.application.name: gateway

spring.cloud.gateway.server.webflux.trusted-proxies: ".*"

# ★ 추가 (ssl 두 블록): HTTPS용 인증서(keystore/edge.p12)
spring.ssl.bundle.jks.gateway:
  key:
    alias: localhost
  keystore:
    type: PKCS12
    location: classpath:keystore/edge.p12
    password: password

server.ssl.bundle: gateway

# ★ 변경 (전: app.eureka-server 한 줄): Eureka 계정, 인가 서버 주소 추가
app:
  eureka-username: u
  eureka-password: p
  eureka-server: localhost
  auth-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"  # ★ 변경 (계정 추가)
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

# ★ 추가 (oauth2-server, oauth2-login, oauth2-error 라우트 3개): 인가 서버로 보냄
- id: oauth2-server
  uri: lb://auth-server
  predicates:
  - Path=/oauth2/**

- id: oauth2-login
  uri: lb://auth-server
  predicates:
  - Path=/login/**

- id: oauth2-error
  uri: lb://auth-server
  predicates:
  - Path=/error/**

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

spring.security.oauth2.resourceserver.jwt.issuer-uri: http://${app.auth-server}:9999  # ★ 추가: 토큰 검증용 인가 서버 주소

management.endpoint.gateway.enabled: true
management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

logging:
  level:
    root: INFO
    org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator: INFO
    org.springframework.cloud.gateway: TRACE
    org.springframework.web.server.adapter.HttpWebHandlerAdapter: TRACE  # ★ 추가

---
spring.config.activate.on-profile: docker

# ★ 변경 (전: app.eureka-server 한 줄)
app:
  eureka-server: eureka
  auth-server: auth-server
```

| 바뀐 곳 | 뜻 |
|---|---|
| `server.port: 8443`, `spring.ssl.bundle...`, `server.ssl.bundle` | 1절의 인증서로 HTTPS |
| 라우트 `oauth2-server`, `oauth2-login`, `oauth2-error` | `/oauth2/**` 등을 `lb://auth-server`로 (인가 서버는 밖에 포트를 안 연다) |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | 토큰 발급자 주소. 여기서 `/.well-known/openid-configuration`과 공개키를 받아 JWT 서명을 확인하고, 토큰의 `iss`가 같은지도 본다 |

## 확인 (도커 필요 없음)
```bash
./gradlew :spring-cloud:gateway:build
```
`BUILD SUCCESSFUL`이면 된다(테스트 1개).

다음: [04-composite-security.md](04-composite-security.md)
