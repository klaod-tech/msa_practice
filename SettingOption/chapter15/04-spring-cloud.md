# 04. 스프링 클라우드 서버: build.gradle, Dockerfile, 인가 서버 정리

> 시작: 14장까지 따라 한 상태 / 결과: 15장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

- 각 서버 `build.gradle`: Boot 4 스타터 이름(`spring-boot-starter-security-oauth2-...`)과 `restclient` 스타터.
- 인가 서버 정리(설정, 테스트에 `@Disabled` 등), Eureka 테스트 정리, gateway `HealthCheckConfiguration` 정리.
- 프로젝트마다 `settings.gradle`, `.gitignore`, `Dockerfile` 맞추기.

## 4-1. `spring-cloud/authorization-server/build.gradle`
```bash
code spring-cloud/authorization-server/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'  // ★ 추가
}

group = 'se.magnus.springcloud'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21)  // 교재와 다름 (교재: 24))
    }
}

repositories {
    mavenCentral()
}

ext {
    // ★ 변경 시작 (전 4줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: springCloudVersion = '2025.1.4'  // 교재와 다름 (교재: 2025.0.0, 7장부터)
    // ★ 전: }
    // ★ 전: jar {
    // ★ 전: enabled = false
    springCloudVersion = '2025.1.4'
    // ★ 변경 끝
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server'  // ★ 변경 (전: implementation 'org.springframework.boot:spring-boot-starter-oauth2...)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.boot:spring-boot-starter-restclient'  // ★ 추가
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'  // 교재에는 없음 (Boot 4: MockMvc 테스트 지원이 별도 모듈)
    // ★ 전: testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    // ★ 변경 끝
    testImplementation 'org.htmlunit:htmlunit'
// ★ 삭제: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'  // 교재에는 없음 (Gradle 9)
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

## 4-2. `spring-cloud/authorization-server/settings.gradle` (새 파일)
```bash
code spring-cloud/authorization-server/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'authorization-server'
```

## 4-3. `spring-cloud/authorization-server/.gitignore` (새 파일)
```bash
code spring-cloud/authorization-server/.gitignore
```
붙여넣기 (새 파일)
```
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

## 4-4. `spring-cloud/authorization-server/src/main/java/sample/config/AuthorizationServerConfig.java`
```bash
code spring-cloud/authorization-server/src/main/java/sample/config/AuthorizationServerConfig.java
```
아래처럼 맞추기 (전체 파일)
```java
package sample.config;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;  // ★ 변경 (전: import org.springframework.security.config.annotation.web.configure...)
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.util.matcher.RequestMatcher;
import sample.jose.Jwks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;  // ★ 추가
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;  // ★ 변경 (전: import org.springframework.security.config.annotation.web.configura...)
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

  private static final Logger LOG = LoggerFactory.getLogger(AuthorizationServerConfig.class);

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {

    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
      new OAuth2AuthorizationServerConfigurer();

    authorizationServerConfigurer
      .authorizationEndpoint(authorizationEndpoint ->
        authorizationEndpoint
          .authenticationProviders(configureAuthenticationValidator())
      );

    RequestMatcher endpointsMatcher = authorizationServerConfigurer
      .getEndpointsMatcher();

    http
      .securityMatcher(endpointsMatcher)
      .authorizeHttpRequests(authorize ->
        authorize.anyRequest().authenticated()
      )
      .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
      .with(authorizationServerConfigurer, Customizer.withDefaults());

    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
      .oidc(Customizer.withDefaults());

    http
      .exceptionHandling(exceptions ->
        exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
      )
      .oauth2ResourceServer(t -> t.jwt(Customizer.withDefaults()));

    return http.build();
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    RegisteredClient writerClient = RegisteredClient.withId(UUID.randomUUID().toString())
      .clientId("writer")
      .clientSecret("{noop}secret-writer")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .redirectUri("https://my.redirect.uri")
      .redirectUri("https://localhost:8443/openapi/swagger-ui/oauth2-redirect.html")
      .scope(OidcScopes.OPENID)
      .scope("product:read")
      .scope("product:write")
      .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
      .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
      .build();

    RegisteredClient readerClient = RegisteredClient.withId(UUID.randomUUID().toString())
      .clientId("reader")
      .clientSecret("{noop}secret-reader")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .redirectUri("https://my.redirect.uri")
      .redirectUri("https://localhost:8443/openapi/swagger-ui/oauth2-redirect.html")
      .scope(OidcScopes.OPENID)
      .scope("product:read")
      .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
      .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofHours(1)).build())
      .build();

    return new InMemoryRegisteredClientRepository(writerClient, readerClient);

  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = Jwks.generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().issuer("http://auth-server:9999").build();
  }

  private Consumer<List<AuthenticationProvider>> configureAuthenticationValidator() {
    return (authenticationProviders) ->
      authenticationProviders.forEach((authenticationProvider) -> {
        if (authenticationProvider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider) {
          Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authenticationValidator =
            new CustomRedirectUriValidator()
              .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR);

          ((OAuth2AuthorizationCodeRequestAuthenticationProvider) authenticationProvider)
            .setAuthenticationValidator(authenticationValidator);
        }
      });
  }

  static class CustomRedirectUriValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
      OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
        authenticationContext.getAuthentication();
      RegisteredClient registeredClient = authenticationContext.getRegisteredClient();
      String requestedRedirectUri = authorizationCodeRequestAuthentication.getRedirectUri();

      LOG.trace("Will validate the redirect uri {}", requestedRedirectUri);

      if (!registeredClient.getRedirectUris().contains(requestedRedirectUri)) {
        LOG.trace("Redirect uri is invalid!");
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
        throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, null);
      }
      LOG.trace("Redirect uri is OK!");
    }
  }
}
```

## 4-5. `spring-cloud/authorization-server/src/main/java/sample/jose/KeyGeneratorUtils.java`
```bash
code spring-cloud/authorization-server/src/main/java/sample/jose/KeyGeneratorUtils.java
```
아래처럼 맞추기 (전체 파일)
```java
package sample.jose;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

final class KeyGeneratorUtils {

  private KeyGeneratorUtils() {
  }

  static SecretKey generateSecretKey() {
    SecretKey hmacKey;
    try {
      hmacKey = KeyGenerator.getInstance("HmacSHA256").generateKey();  // ★ 변경 (전: hmacKey = KeyGenerator.getInstance("HmacSha256").generateKey();)
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return hmacKey;
  }

  static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }

  static KeyPair generateEcKey() {
    EllipticCurve ellipticCurve = new EllipticCurve(
        new ECFieldFp(
            new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853951")),
        new BigInteger("115792089210356248762697446949407573530086143415290314195533631308867097853948"),
        new BigInteger("41058363725152142129326129780047268409114441015993725554835256314039467401291"));
    ECPoint ecPoint = new ECPoint(
        new BigInteger("48439561293906451759052585252797914202762949526041747995844080717082404635286"),
        new BigInteger("36134250956749795798585127919587881956611106672985015071877198253568414405109"));
    ECParameterSpec ecParameterSpec = new ECParameterSpec(
        ellipticCurve,
        ecPoint,
        new BigInteger("115792089210356248762697446949407573529996955224135760342422259061068512044369"),
        1);

    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
      keyPairGenerator.initialize(ecParameterSpec);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }
}
```

## 4-6. `spring-cloud/authorization-server/src/test/java/sample/OAuth2AuthorizationServerApplicationTests.java`
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

import org.junit.jupiter.api.Disabled;  // ★ 추가
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;  // ★ 변경 (전: import org.springframework.boot.webmvc.test.autoconfigure.AutoConfi...)
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.config.enabled=false"})
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

## 4-7. `spring-cloud/authorization-server/src/test/resources/application.yml` (새 파일)
```bash
mkdir -p spring-cloud/authorization-server/src/test/resources
code spring-cloud/authorization-server/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
spring.web.error.include-message: always

ml.spring.cloud.stream.defaultBinder: rabbit
```

## 4-8. `spring-cloud/authorization-server/Dockerfile`
```bash
code spring-cloud/authorization-server/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 9999

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 4-9. `spring-cloud/config-server/build.gradle`
```bash
code spring-cloud/config-server/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 추가 시작 (2줄)
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    // ★ 추가 끝
    id 'java'
// ★ 삭제 시작 (2줄) — 아래 줄들을 지운다
// ★ 지움: id 'org.springframework.boot' version '4.0.9'  // 교재와 다름 (교재: 3.5.0)
// ★ 지움: id 'io.spring.dependency-management' version '1.1.7'
// ★ 삭제 끝
}

group = 'se.magnus.springcloud'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21)  // 교재와 다름 (교재: 24))
    }
}

repositories {
    mavenCentral()
}

ext {
    springCloudVersion = '2025.1.4'  // ★ 변경 (전: springCloudVersion = '2025.1.4'  // 교재와 다름 (교재: 2025.0.0))
}

jar {
    enabled = false
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.cloud:spring-cloud-config-server'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.security:spring-security-test'
    // ★ 전: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'  // 교재에는 없음 (Gradle 9)
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    // ★ 변경 끝
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

## 4-10. `spring-cloud/config-server/settings.gradle` (새 파일)
```bash
code spring-cloud/config-server/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'config-server'
```

## 4-11. `spring-cloud/config-server/.gitignore` (새 파일)
```bash
code spring-cloud/config-server/.gitignore
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

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

.vscode/
```

## 4-12. `spring-cloud/config-server/Dockerfile`
```bash
code spring-cloud/config-server/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
# ★ 삭제: # 교재와 다름 (교재: eclipse-temurin:24_36-jre-noble, layertools)
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8888

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 4-13. `spring-cloud/eureka-server/build.gradle`
```bash
code spring-cloud/eureka-server/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'  // ★ 추가
}

group = 'se.magnus.springcloud'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21)  // 교재와 다름 (교재: 24))
    }
}

repositories {
    mavenCentral()
}

ext {
    springCloudVersion = '2025.1.4'  // ★ 변경 (전: springCloudVersion = '2025.1.4'  // 교재와 다름 (교재: 2025.0.0, 7장부터))
}

jar {
    enabled = false
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'
    implementation 'org.glassfish.jaxb:jaxb-runtime'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // ★ 변경 시작 (전 3줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-starter-restclient-test'  // 교재에는 없음 (Boot 4: RestTemplate 테스트 지원)
    // ★ 전: testImplementation 'org.springframework.boot:spring-boot-resttestclient'  // 교재에는 없음 (Boot 4: TestRestTemplate이 별도 모듈)
    // ★ 전: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testImplementation 'org.springframework.boot:spring-boot-resttestclient'
    testImplementation 'org.springframework.boot:spring-boot-restclient'
    // ★ 변경 끝
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

## 4-14. `spring-cloud/eureka-server/settings.gradle` (새 파일)
```bash
code spring-cloud/eureka-server/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'eureka-server'
```

## 4-15. `spring-cloud/eureka-server/.gitignore` (새 파일)
```bash
code spring-cloud/eureka-server/.gitignore
```
붙여넣기 (새 파일)
```
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

## 4-16. `spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java`
```bash
code spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;  // ★ 추가
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// ★ 변경 시작 (전 2줄 → 2줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
// ★ 전: import org.springframework.boot.resttestclient.TestRestTemplate;  // 교재와 다름 (교재: org.springframework.boot.test.web.client.TestRestTemplate)
// ★ 전: import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;  // 교재에는 없음 (Boot 4)
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
// ★ 변경 끝
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureTestRestTemplate  // ★ 추가
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.cloud.config.enabled=false"})
// ★ 삭제: @AutoConfigureTestRestTemplate  // 교재에는 없음 (Boot 4)
class EurekaServerApplicationTests {

  @Test
  void contextLoads() {
  }

  @Value("${app.eureka-username}")
  private String username;

  @Value("${app.eureka-password}")
  private String password;

  @Autowired
  void setTestRestTemplate(TestRestTemplate testRestTemplate) {
    this.testRestTemplate = testRestTemplate.withBasicAuth(username, password);
  }

  private TestRestTemplate testRestTemplate;

  @Test
  void catalogLoads() {

    String expectedReponseBody = "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}";
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }

  @Test
  void healthy() {
    String expectedStatus = "\"status\":\"UP\"";  // ★ 변경 (전: String expectedReponseBody = "{\"groups\":[\"liveness\",\"readiness...)
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertTrue(entity.getBody().contains(expectedStatus));  // ★ 변경 (전: assertEquals(expectedReponseBody, entity.getBody());)
  }
}
```

## 4-17. `spring-cloud/eureka-server/Dockerfile`
```bash
code spring-cloud/eureka-server/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8761

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 4-18. `spring-cloud/gateway/build.gradle`
```bash
code spring-cloud/gateway/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    // ★ 삭제: id 'java'
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'  // ★ 추가
}

group = 'se.magnus.springcloud'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)  // ★ 변경 (전: languageVersion = JavaLanguageVersion.of(21)  // 교재와 다름 (교재: 24))
    }
}

repositories {
    mavenCentral()
}

ext {
    springCloudVersion = '2025.1.4'  // ★ 변경 (전: springCloudVersion = '2025.1.4'  // 교재와 다름 (교재: 2025.0.0, 7장부터))
}

jar {
    enabled = false
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'  // ★ 변경 (전: implementation 'org.springframework.boot:spring-boot-starter-oauth2...)
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
    // ★ 추가 시작 (2줄)
    implementation 'org.springframework.boot:spring-boot-starter-webclient'
    implementation 'org.springframework.boot:spring-boot-starter-restclient'
    // ★ 추가 끝
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.cloud:spring-cloud-config-client'
    implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
    implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'
    implementation 'org.springframework.boot:spring-boot-zipkin'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
// ★ 삭제: testRuntimeOnly 'org.junit.platform:junit-platform-launcher'  // 교재에는 없음 (Gradle 9)
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

## 4-19. `spring-cloud/gateway/settings.gradle` (새 파일)
```bash
code spring-cloud/gateway/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'gateway'
```

## 4-20. `spring-cloud/gateway/.gitignore` (새 파일)
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

## 4-21. `spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/HealthCheckConfiguration.java`
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
import org.springframework.boot.health.contributor.*;  // ★ 변경 (전: import org.springframework.boot.health.contributor.*;  // 교재와 다름 (교...)
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
    registry.put("auth-server",       () -> getHealth("http://auth-server"));

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

## 4-22. `spring-cloud/gateway/Dockerfile`
```bash
code spring-cloud/gateway/Dockerfile
```
아래처럼 맞추기 (전체 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: WORKDIR /extracted
WORKDIR /builder
# ★ 변경 끝
ADD ./build/libs/*.jar app.jar
# ★ 변경 시작 (전 1줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted
# ★ 변경 끝

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
# ★ 전: COPY --from=builder /extracted/layers/dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/spring-boot-loader/ ./
# ★ 전: COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
# ★ 전: COPY --from=builder /extracted/layers/application/ ./
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
# ★ 변경 끝

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

다음: [05-config-repo.md](05-config-repo.md)
