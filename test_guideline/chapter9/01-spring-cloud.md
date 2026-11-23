# 01. 새 프로젝트: spring-cloud/eureka-server

> 시작: 교수님 `Chapter07 (8장은 코드 없음)` 상태 / 결과: 교수님 `Chapter09`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

Eureka 서버를 새 프로젝트로 만든다(교재는 spring init으로 만들지만 여기서는 파일을 바로 쓴다).
- `build.gradle`: `spring-cloud-starter-netflix-eureka-server`, Spring Cloud BOM.
- `EurekaServerApplication`: `@EnableEurekaServer` 한 줄로 Eureka 서버가 된다.
- `application.yml`: 포트 8761. 자기 자신은 등록하지 않는다(`registerWithEureka: false`, `fetchRegistry: false`).
- 테스트: 카탈로그(`/eureka/apps`)와 헬스(`/actuator/health`)를 확인한다.

## 1-1. `spring-cloud/eureka-server/build.gradle` (새 파일)
```bash
mkdir -p spring-cloud/eureka-server
code spring-cloud/eureka-server/build.gradle
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
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
    implementation 'org.glassfish.jaxb:jaxb-runtime'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-resttestclient'
    testImplementation 'org.springframework.boot:spring-boot-restclient'
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

## 1-2. `spring-cloud/eureka-server/settings.gradle` (새 파일)
```bash
code spring-cloud/eureka-server/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'eureka-server'
```

## 1-3. `spring-cloud/eureka-server/.gitignore` (새 파일)
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

## 1-4. `spring-cloud/eureka-server/src/main/java/se/magnus/springcloud/eurekaserver/EurekaServerApplication.java` (새 파일)
```bash
mkdir -p spring-cloud/eureka-server/src/main/java/se/magnus/springcloud/eurekaserver
code spring-cloud/eureka-server/src/main/java/se/magnus/springcloud/eurekaserver/EurekaServerApplication.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }

}
```

## 1-5. `spring-cloud/eureka-server/src/main/resources/application.yml` (새 파일)
```bash
mkdir -p spring-cloud/eureka-server/src/main/resources
code spring-cloud/eureka-server/src/main/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
server:
  port: 8761

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

## 1-6. `spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java` (새 파일)
```bash
mkdir -p spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver
code spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = RANDOM_PORT)
class EurekaServerApplicationTests {

  @Test
  void contextLoads() {
  }

  @Autowired
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
    String expectedStatus = "\"status\":\"UP\"";
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertTrue(entity.getBody().contains(expectedStatus));
  }
}
```

## 1-7. `spring-cloud/eureka-server/Dockerfile` (새 파일)
```bash
code spring-cloud/eureka-server/Dockerfile
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

EXPOSE 8761

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

다음: [02-core-services.md](02-core-services.md)
