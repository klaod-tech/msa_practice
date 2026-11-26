# 04. 인가 서버·게이트웨이 정리

> 시작: 17장까지 따라 한 상태 / 결과: 18장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

인가 서버와 게이트웨이(compose 구성용)도 추적 의존성을 맞춘다.

## 4-1. `spring-cloud/authorization-server/build.gradle`
```bash
code spring-cloud/authorization-server/build.gradle
```
아래처럼 맞추기 (전체 파일)
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

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    // ★ 변경 시작 (전 5줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    // ★ 전: implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-zipkin'
    // ★ 전: implementation 'org.springframework.retry:spring-retry:2.0.13'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'org.springframework.boot:spring-boot-starter-zipkin'
    // ★ 변경 끝

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    testImplementation 'org.htmlunit:htmlunit'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 4-2. `spring-cloud/authorization-server/src/main/java/sample/OAuth2AuthorizationServerApplication.java`
```bash
code spring-cloud/authorization-server/src/main/java/sample/OAuth2AuthorizationServerApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package sample;

// ★ 추가 시작 (5줄)
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationCustomizer;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import org.springframework.beans.factory.ObjectProvider;
// ★ 추가 끝
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;  // ★ 추가

@SpringBootApplication
public class OAuth2AuthorizationServerApplication {

  // ★ 추가 시작 (8줄)
  @Bean
  BaggagePropagation.FactoryBuilder myPropagationFactoryBuilder(
    ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
    Propagation.Factory delegate = B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.MULTI).build();
    BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
    baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
    return builder;
  }
  // ★ 추가 끝

  public static void main(String[] args) {
    SpringApplication.run(OAuth2AuthorizationServerApplication.class, args);
  }

}
```

## 4-3. `spring-cloud/authorization-server/src/test/java/sample/OAuth2AuthorizationServerApplicationTests.java`
```bash
code spring-cloud/authorization-server/src/test/java/sample/OAuth2AuthorizationServerApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package sample;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.core.Is.is;

// ★ 삭제: import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

@SpringBootTest
@AutoConfigureMockMvc
class OAuth2AuthorizationServerApplicationTests {

  @Autowired
  MockMvc mvc;

  @Test
  void requestTokenUsingClientCredentialsGrantType() throws Exception {

    String base64Credentials = Base64.getEncoder().encodeToString("writer:secret-writer".getBytes());
    this.mvc.perform(post("/oauth2/token")
      .param("grant_type", "client_credentials")
      .header("Authorization", "Basic " + base64Credentials))
        .andExpect(status().isOk());
  }

  @Test
  void requestOpenidConfiguration() throws Exception {

    this.mvc.perform(get("/.well-known/openid-configuration"))
        .andExpect(status().isOk());
  }

  @Test
  void requestJwkSet() throws Exception {

    this.mvc.perform(get("/oauth2/jwks"))
        .andExpect(status().isOk());
  }

  @Test
  void healthy() throws Exception {
    this.mvc.perform(get("/actuator/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", is("UP")));
  }

}
```

## 4-4. `spring-cloud/gateway/build.gradle`
```bash
code spring-cloud/gateway/build.gradle
```
아래처럼 맞추기 (전체 파일)
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
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-webclient'
    // ★ 변경 시작 (전 4줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    // ★ 전: implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    // ★ 전: implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'org.springframework.boot:spring-boot-starter-zipkin'
    // ★ 변경 끝
    implementation 'org.springframework.retry:spring-retry'

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

## 4-5. `spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/GatewayApplication.java`
```bash
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/GatewayApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.gateway;

// ★ 추가 시작 (5줄)
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationCustomizer;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import org.springframework.beans.factory.ObjectProvider;
// ★ 추가 끝
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class GatewayApplication {

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
  }

  // ★ 추가 시작 (8줄)
  @Bean
  BaggagePropagation.FactoryBuilder myPropagationFactoryBuilder(
    ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
    Propagation.Factory delegate = B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.MULTI).build();
    BaggagePropagation.FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(delegate);
    baggagePropagationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
    return builder;
  }
  // ★ 추가 끝

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();
    SpringApplication.run(GatewayApplication.class, args);
  }

}
```

## 4-6. `spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/HealthCheckConfiguration.java`
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

    // ★ 변경 시작 (전 5줄 → 5줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: registry.put("product",           () -> getHealth("http://product"));
    // ★ 전: registry.put("recommendation",    () -> getHealth("http://recommendation"));
    // ★ 전: registry.put("review",            () -> getHealth("http://review"));
    // ★ 전: registry.put("product-composite", () -> getHealth("http://product-composite"));
    // ★ 전: registry.put("auth-server",       () -> getHealth("http://auth-server"));
    registry.put("product",           () -> getHealth("http://product:4004"));
    registry.put("recommendation",    () -> getHealth("http://recommendation:4004"));
    registry.put("review",            () -> getHealth("http://review:4004"));
    registry.put("product-composite", () -> getHealth("http://product-composite:4004"));
    registry.put("auth-server",       () -> getHealth("http://auth-server:4004"));
    // ★ 변경 끝

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

다음: [05-config-repo.md](05-config-repo.md)
