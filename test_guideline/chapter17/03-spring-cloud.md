# 03. config-server 삭제, 인가 서버·게이트웨이 정리

> 시작: 교수님 `Chapter16` 상태 / 결과: 교수님 `Chapter17`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`config-server` 프로젝트 삭제. 인가 서버와 게이트웨이도 설정 클라이언트를 뺀다(게이트웨이는 도커 컴포즈 구성에서만 쓴다).

## 3-1. `spring-cloud/authorization-server/build.gradle`
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

// ★ 삭제 (3줄, 첫 줄: ext {)
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    // ★ 삭제 (5줄, 첫 줄: implementation 'org.springframework.cloud:sprin...)
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation 'org.springframework.retry:spring-retry:2.0.13'  // ★ 추가

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    testImplementation 'org.htmlunit:htmlunit'
}

// ★ 삭제 (5줄, 첫 줄: dependencyManagement {)
tasks.named('test') {
    useJUnitPlatform()
}
```

## 3-2. `spring-cloud/authorization-server/src/test/java/sample/OAuth2AuthorizationServerApplicationTests.java`
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

@SpringBootTest  // ★ 변경 (전: @SpringBootTest(properties = {"spring.cloud.config.enabled=false"}))
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

## 3-3. `spring-cloud/gateway/build.gradle`
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
    // ★ 삭제 (5줄, 첫 줄: implementation 'org.springframework.cloud:sprin...)
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation 'org.springframework.retry:spring-retry'  // ★ 추가

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

## 3-4. `spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/SecurityConfig.java`
```bash
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/SecurityConfig.java
```
아래처럼 맞추기 (전체 파일)
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
          .pathMatchers("/oauth2/**").permitAll()
          .pathMatchers("/login/**").permitAll()
          .pathMatchers("/error/**").permitAll()
          .pathMatchers("/openapi/**").permitAll()
          // ★ 삭제: .pathMatchers("/config/**").permitAll()
          .anyExchange().authenticated()))
      .oauth2ResourceServer(server -> server
        .jwt(Customizer.withDefaults()));
    return http.build();
  }

}
```

## 3-5. `spring-cloud/gateway/src/test/java/se/magnus/springcloud/gateway/GatewayApplicationTests.java`
```bash
code spring-cloud/gateway/src/test/java/se/magnus/springcloud/gateway/GatewayApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.gateway;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT)  // ★ 변경 (전: @SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring...)
class GatewayApplicationTests {

  @Test
  void contextLoads() {
  }

}
```

## 3-6. 지울 파일
```bash
cd ~/msa
rm spring-cloud/authorization-server/src/main/resources/bootstrap.yml
rm spring-cloud/config-server/build.gradle
rm spring-cloud/config-server/settings.gradle
rm spring-cloud/config-server/.gitignore
rm spring-cloud/config-server/src/main/java/se/magnus/springcloud/configserver/ConfigServerApplication.java
rm spring-cloud/config-server/src/main/java/se/magnus/springcloud/configserver/SecurityConfig.java
rm spring-cloud/config-server/src/main/resources/application.yml
rm spring-cloud/config-server/src/test/java/se/magnus/springcloud/configserver/ConfigServerApplicationTests.java
rm spring-cloud/config-server/Dockerfile
rm spring-cloud/gateway/src/main/resources/bootstrap.yml
```

다음: [04-config-repo.md](04-config-repo.md)
