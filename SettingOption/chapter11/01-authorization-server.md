# 01. 인가 서버 (새 프로젝트 spring-cloud/authorization-server)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 10장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Security 7 때문에 교재 11장과 다르게 쓰는 부분.
> 교재 "로컬 인가 서버 추가" 절. 교재 소스는 Spring Authorization Server 예제를 바탕으로 해서 패키지가 `sample`이다(그대로 둔다).

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
include ':spring-cloud:gateway'
include ':spring-cloud:authorization-server'  // ★ 추가
```

## 2. build.gradle (새 파일)
```bash
mkdir -p spring-cloud/authorization-server
code spring-cloud/authorization-server/build.gradle
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
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-authorization-server'
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'

	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'  // 교재에는 없음 (Boot 4: MockMvc 테스트 지원이 별도 모듈)
	testImplementation 'org.springframework.security:spring-security-test'
	testImplementation 'org.htmlunit:htmlunit'
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
| `spring-boot-starter-oauth2-authorization-server` | 인가 서버 (서블릿 기반, Spring MVC) |
| `spring-boot-starter-security` | 로그인 화면, 사용자 인증 |
| `spring-cloud-starter-netflix-eureka-client` | 게이트웨이가 `lb://auth-server`로 찾을 수 있게 등록 |
| `spring-boot-starter-webmvc-test` | **교재에는 없음.** Boot 4에서 `@AutoConfigureMockMvc`가 이 모듈로 옮겨졌다 |

## 3. Application (새 파일)
```bash
mkdir -p spring-cloud/authorization-server/src/main/java/sample
code spring-cloud/authorization-server/src/main/java/sample/OAuth2AuthorizationServerApplication.java
```
붙여넣기 (새 파일)
```java
package sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OAuth2AuthorizationServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(OAuth2AuthorizationServerApplication.class, args);
  }

}
```

## 4. 인가 서버 설정 (새 파일)
```bash
mkdir -p spring-cloud/authorization-server/src/main/java/sample/config
code spring-cloud/authorization-server/src/main/java/sample/config/AuthorizationServerConfig.java
```
붙여넣기 (새 파일)
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
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;  // 교재와 다름 (교재: org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers, Spring Security 7)
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.util.matcher.RequestMatcher;
import sample.jose.Jwks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;  // 교재와 다름 (교재: org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration, Spring Security 7)
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

| 빈 / 메서드 | 하는 일 |
|---|---|
| `authorizationServerSecurityFilterChain` | `/oauth2/token`, `/oauth2/authorize`, `/.well-known/...` 같은 인가 서버 엔드포인트 설정. OIDC 켬. 로그인 안 했으면 `/login`으로 |
| `registeredClientRepository` | 클라이언트 2개 등록: `writer`(read+write), `reader`(read). 토큰 유효 1시간, 동의 화면 사용 |
| `jwkSource` | 토큰 **서명용 RSA 키**를 시작할 때 만든다(재시작하면 바뀜 → 예전 토큰은 무효) |
| `authorizationServerSettings` | 토큰의 발급자(`iss`)를 `http://auth-server:9999`로 고정 (compose 안의 이름) |
| `CustomRedirectUriValidator` | 등록된 redirect URI와 **정확히 같은지**만 확인 (개발 중 `https://localhost` 허용) |

**교재와 다름:** Spring Security 7부터 Spring Authorization Server가 Spring Security에 합쳐지면서 `OAuth2AuthorizationServerConfigurer`, `OAuth2AuthorizationServerConfiguration`의 패키지가 바뀌었다(import 두 줄). 나머지 코드는 교재 그대로 컴파일, 동작한다.

## 5. 사용자, 로그인 설정 (새 파일)
```bash
code spring-cloud/authorization-server/src/main/java/sample/config/DefaultSecurityConfig.java
```
붙여넣기 (새 파일)
```java
package sample.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSecurityConfig.class);

  @Bean
  SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
      .authorizeHttpRequests(authorizeRequests -> authorizeRequests
        .requestMatchers("/actuator/**").permitAll()
        .anyRequest().authenticated()
      )
      .formLogin(withDefaults());
    return http.build();
  }

  @Bean
  UserDetailsService users() {
    @SuppressWarnings("deprecation")
    UserDetails user = User.withDefaultPasswordEncoder()
      .username("u")
      .password("p")
      .roles("USER")
      .build();
    return new InMemoryUserDetailsManager(user);
  }

}
```

사용자 `u` / `p` 한 명. `/actuator/**`는 로그인 없이 열어 둔다(compose healthcheck용).

## 6. 키 생성 도우미 (새 파일 2개)
```bash
mkdir -p spring-cloud/authorization-server/src/main/java/sample/jose
code spring-cloud/authorization-server/src/main/java/sample/jose/Jwks.java
```
붙여넣기 (새 파일)
```java
package sample.jose;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import javax.crypto.SecretKey;

public final class Jwks {

  private Jwks() {
  }

  public static RSAKey generateRsa() {
    KeyPair keyPair = KeyGeneratorUtils.generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    return new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  public static ECKey generateEc() {
    KeyPair keyPair = KeyGeneratorUtils.generateEcKey();
    ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
    ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
    Curve curve = Curve.forECParameterSpec(publicKey.getParams());
    return new ECKey.Builder(curve, publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  public static OctetSequenceKey generateSecret() {
    SecretKey secretKey = KeyGeneratorUtils.generateSecretKey();
    return new OctetSequenceKey.Builder(secretKey)
        .keyID(UUID.randomUUID().toString())
        .build();
  }
}
```

```bash
code spring-cloud/authorization-server/src/main/java/sample/jose/KeyGeneratorUtils.java
```
붙여넣기 (새 파일)
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
      hmacKey = KeyGenerator.getInstance("HmacSha256").generateKey();
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

## 7. application.yml (새 파일)
```bash
mkdir -p spring-cloud/authorization-server/src/main/resources
code spring-cloud/authorization-server/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 9999
server.forward-headers-strategy: framework

spring.application.name: auth-server

app:
  eureka-username: u
  eureka-password: p
  eureka-server: localhost

eureka:
  client:
    serviceUrl:
      defaultZone: "http://${app.eureka-username}:${app.eureka-password}@${app.eureka-server}:8761/eureka/"
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

logging:
  level:
    root: INFO

---
spring.config.activate.on-profile: docker

app.eureka-server: eureka
```

Eureka 주소에 계정(`u:p`)이 들어간 건 02에서 Eureka를 막기 때문이다.

## 8. 테스트 (새 파일)
```bash
mkdir -p spring-cloud/authorization-server/src/test/java/sample
code spring-cloud/authorization-server/src/test/java/sample/OAuth2AuthorizationServerApplicationTests.java
```
붙여넣기 (새 파일)
```java
package sample;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;  // 교재와 다름 (교재: org.springframework.boot.test.autoconfigure.web.servlet, Boot 4)
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

| 테스트 | 확인하는 것 |
|---|---|
| `requestTokenUsingClientCredentialsGrantType` | `writer:secret-writer`로 `/oauth2/token` → 200 (토큰 발급) |
| `requestOpenidConfiguration` | `/.well-known/openid-configuration` (리소스 서버가 설정을 받아 가는 곳) |
| `requestJwkSet` | `/oauth2/jwks` (서명 검증용 공개키) |
| `healthy` | `/actuator/health` UP |

## 9. Dockerfile (새 파일)
```bash
code spring-cloud/authorization-server/Dockerfile
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

EXPOSE 9999

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 확인 (도커 필요 없음)
VS Code에서 `Shift+Alt+U` 후:
```bash
./gradlew :spring-cloud:authorization-server:build
```
`BUILD SUCCESSFUL`이면 된다(테스트 4개).
