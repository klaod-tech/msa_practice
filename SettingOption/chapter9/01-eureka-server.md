# 01. Eureka 서버 (새 프로젝트 spring-cloud/eureka-server)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 7장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 9장과 다르게 쓰는 부분.
> 교재 "유레카 서버 설정" 절.

`microservices/`와 나란히 **`spring-cloud/`** 폴더가 생긴다. 앞으로 10장(게이트웨이), 12장(설정 서버) 같은 스프링 클라우드 인프라 서비스가 여기 들어간다.

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
include ':spring-cloud:eureka-server'  // ★ 추가
```

> 원래 파일이 마지막 줄 뒤에 줄바꿈 없이 끝나 있으면, 새 줄을 같은 줄에 붙여 쓰지 않도록 `include ':util'` 끝에서 **Enter**를 치고 쓴다(troubleshooting 참고).

## 2. build.gradle (새 파일)
```bash
mkdir -p spring-cloud/eureka-server
code spring-cloud/eureka-server/build.gradle
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
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
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

| 줄 | 하는 일 |
|---|---|
| `spring-cloud-starter-netflix-eureka-server` | Eureka 서버 |
| `jaxb-runtime` | Eureka가 XML 처리에 쓰는 라이브러리 (교재 그대로) |
| `spring-boot-starter-restclient-test`, `spring-boot-resttestclient` | **교재에는 없음.** Boot 4에서 테스트용 `TestRestTemplate`이 별도 모듈로 분리됐다 |
| `junit-platform-launcher` | 교재에는 없음. Gradle 9에서 테스트 실행에 필요 (다른 프로젝트와 같음) |

## 3. Application (새 파일)
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

`@EnableEurekaServer` 한 줄이 이 앱을 Eureka 서버로 만든다.

## 4. application.yml (새 파일)
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

| 설정 | 뜻 |
|---|---|
| `server.port: 8761` | Eureka 기본 포트 |
| `registerWithEureka: false`, `fetchRegistry: false` | 서버 자신은 클라이언트로 등록하지 않는다 (서버가 하나뿐이니까) |
| `waitTimeInMsWhenSyncEmpty: 0`, `response-cache-update-interval-ms: 5000` | 시작 직후 기다리지 않고, 목록 응답 캐시를 5초마다 갱신 (실습용으로 빠르게) |

## 5. 테스트 (새 파일)
```bash
mkdir -p spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver
code spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java
```
붙여넣기 (새 파일)
```java
package se.magnus.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
    String expectedReponseBody = "{\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}";  // 교재와 다름 (교재: {"status":"UP"}, Boot 4는 groups도 보여 줌)
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }
}
```

| 테스트 | 확인하는 것 |
|---|---|
| `catalogLoads` | `/eureka/apps`가 200이고 등록된 앱이 없음(빈 목록) |
| `healthy` | `/actuator/health`가 UP |

**교재와 다름:**
- `TestRestTemplate` 패키지가 `org.springframework.boot.resttestclient`로 바뀌었고, 주입받으려면 `@AutoConfigureTestRestTemplate`이 필요하다.
- Boot 4의 health 응답에는 `"groups":["liveness","readiness"]`가 같이 나와서 `healthy()` 기대값을 바꿨다.

## 6. Dockerfile (새 파일)
```bash
code spring-cloud/eureka-server/Dockerfile
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

EXPOSE 8761

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**교재와 다름:** 교재는 Java 24 이미지와 옛 `-Djarmode=layertools` 방식이다. 우리 서비스들(4장부터)의 Dockerfile과 같은 형식으로 쓰고 `EXPOSE`만 `8761`이다.

## 확인 (도커 필요 없음)
VS Code에서 `Shift+Alt+U` 후:
```bash
./gradlew :spring-cloud:eureka-server:build
```
`BUILD SUCCESSFUL`이면 된다(테스트 3개).

다음: [02-eureka-clients.md](02-eureka-clients.md)
