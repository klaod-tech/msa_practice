# 01. 설정 서버 프로젝트 만들기 (config-server)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 11장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Cloud 2025.1 때문에 교재 12장과 다르게 쓰는 부분.
> 교재 "스프링 클라우드 컨피그 서버 설정" 절. 새 프로젝트라 파일은 전부 새로 만든다(★ 없음).

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
include ':spring-cloud:authorization-server'
include ':spring-cloud:config-server'  // ★ 추가
```

## 2. build.gradle (새 파일)
```bash
mkdir -p spring-cloud/config-server
code spring-cloud/config-server/build.gradle
```
붙여넣기 (새 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'  // 교재와 다름 (교재: 3.5.0)
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
	springCloudVersion = '2025.1.4'  // 교재와 다름 (교재: 2025.0.0)
}

jar {
	enabled = false
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.cloud:spring-cloud-config-server'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testImplementation 'org.springframework.security:spring-security-test'
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

| 의존성 | 하는 일 |
|---|---|
| `spring-cloud-config-server` | 설정 서버 본체. `/{application}/{profile}`로 설정을 나눠 주고 `/encrypt`, `/decrypt`도 제공 |
| `spring-boot-starter-security` | 설정 서버도 아무나 못 읽게 HTTP 기본 인증으로 막는다 |
| `spring-boot-starter-actuator` | `/actuator/health` |

설정 서버는 **Eureka에 등록하지 않는다**(eureka-client 없음). 다른 서비스가 Eureka 주소를 설정 서버에서 받아 가야 하므로, 설정 서버가 Eureka보다 먼저 있어야 하기 때문이다.

## 3. Application (새 파일)
```bash
mkdir -p spring-cloud/config-server/src/main/java/se/magnus/springcloud/configserver
code spring-cloud/config-server/src/main/java/se/magnus/springcloud/configserver/ConfigServerApplication.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.configserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.ConfigurableApplicationContext;

@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigServerApplication.class);

  public static void main(String[] args) {
    ConfigurableApplicationContext ctx = SpringApplication.run(ConfigServerApplication.class, args);

    String repoLocation = ctx.getEnvironment().getProperty("spring.cloud.config.server.native.searchLocations");
    LOG.info("Serving configurations from folder: " + repoLocation);
  }

}
```

`@EnableConfigServer` 한 줄이 이 앱을 설정 서버로 만든다. 시작할 때 어느 폴더의 설정을 내보내는지 로그로 찍는다.

## 4. SecurityConfig (새 파일)
```bash
code spring-cloud/config-server/src/main/java/se/magnus/springcloud/configserver/SecurityConfig.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.configserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults());
    return http.build();
  }
}
```

- 모든 요청에 로그인(HTTP 기본 인증)을 요구한다. 계정은 compose에서 `SPRING_SECURITY_USER_NAME`/`PASSWORD` 환경변수로 준다([04](04-docker-compose-and-test.md)).
- CSRF를 끄는 이유: `/encrypt`, `/decrypt`는 POST라서 CSRF가 켜져 있으면 curl로 부를 수 없다.

## 5. application.yml (새 파일)
```bash
mkdir -p spring-cloud/config-server/src/main/resources
code spring-cloud/config-server/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server.port: 8888

spring.cloud.config.server.native.searchLocations: file:${PWD}/config-repo

management.endpoint.health.show-details: "ALWAYS"
management.endpoints.web.exposure.include: "*"

logging:
  level:
    root: info

---
spring.config.activate.on-profile: docker
spring.cloud.config.server.native.searchLocations: file:/config-repo
```

| 설정 | 뜻 |
|---|---|
| `server.port: 8888` | 설정 서버 포트(관례) |
| `native.searchLocations` | **native** 프로필 = Git 대신 **로컬 폴더**에서 설정 파일을 읽는다. 로컬 실행은 `~/msa/config-repo`, 도커는 컨테이너 안 `/config-repo`(compose가 폴더를 붙여 줌) |

## 6. 테스트 (새 파일)
```bash
mkdir -p spring-cloud/config-server/src/test/java/se/magnus/springcloud/configserver
code spring-cloud/config-server/src/test/java/se/magnus/springcloud/configserver/ConfigServerApplicationTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.configserver;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.profiles.active=native"})
class ConfigServerApplicationTests {

  @Test
  void contextLoads() {
  }

}
```

## 7. Dockerfile (새 파일)
```bash
code spring-cloud/config-server/Dockerfile
```
붙여넣기 (새 파일)
```dockerfile
# 교재와 다름 (교재: eclipse-temurin:24_36-jre-noble, layertools)
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

EXPOSE 8888

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## 확인 (도커 필요 없음)
```bash
cd ~/msa
./gradlew :spring-cloud:config-server:build
```
`BUILD SUCCESSFUL`(테스트 1개 통과). 실제로 띄워 보는 것은 config-repo를 만든 뒤 [02](02-config-repo.md) 끝에서 한다.

다음: [02-config-repo.md](02-config-repo.md)
