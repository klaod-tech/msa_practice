# 01. 의존성, 로그 형식, 컨텍스트 전파 (서비스 7개)

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 13장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 14장과 다르게 쓰는 부분.
> 교재 "마이크로서비스 소스 코드에 분산 추적 추가" 절의 앞부분. composite만의 변경은 [02](02-composite-observation.md)에서 한다.

## 1. build.gradle (서비스 7개)
추가하는 의존성(7개 공통):

| 의존성 | 하는 일 |
|---|---|
| `micrometer-tracing-bridge-otel` | Micrometer Tracing(스프링이 쓰는 추적 API)을 OpenTelemetry로 연결 |
| `opentelemetry-exporter-zipkin` | 추적 정보(span)를 Zipkin 형식으로 내보냄 |
| `spring-boot-micrometer-tracing-opentelemetry` | **교재에는 없음.** Boot 4는 추적 자동 설정이 별도 모듈이다. 이게 없으면 위 두 줄을 넣어도 추적이 **켜지지 않는다**(11장 리소스 서버 스타터와 같은 경우) |
| `spring-boot-zipkin` | **교재에는 없음.** Boot 4에서 Zipkin으로 보내는 부분(전송, `management.tracing.export.zipkin.*` 설정)도 별도 모듈이다 |

review는 DB 쿼리도 추적하려고 `datasource-micrometer-spring-boot`를 하나 더 넣는다. **교재와 다름**: 교재는 1.0.0(Boot 3용). Boot 4용은 2.x이고, 이 실습의 Boot 4.0.x와 맞는 2.2.1을 쓴다(2.3.x는 Boot 4.1 기준).

composite만 추가로 바뀌는 것:
- `spring-cloud-starter-loadbalancer`: [02](02-composite-observation.md)에서 로드 밸런서 필터를 직접 붙이려고 명시한다.
- `springBoot { buildInfo() }`: 빌드 버전(1.0.0-SNAPSHOT)을 만들어 추적 정보에 태그로 붙인다([02](02-composite-observation.md)).
- `testLogging.showStandardStreams = true`: 테스트 로그를 콘솔에 보여 준다.

### 1-1. product-service
```bash
code microservices/product-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}

jar {
    enabled = false
}

group = 'se.magnus.microservices.core.product'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

ext {
	mapstructVersion = '1.6.3'
	springCloudVersion = '2025.1.4'
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	implementation project(":api")
	implementation project(':util')
	implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

	compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
	testImplementation 'org.testcontainers:testcontainers-mongodb'  // 교재와 다름 (교재: org.testcontainers:mongodb, 6장부터)
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

### 1-2. recommendation-service
```bash
code microservices/recommendation-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar {
	enabled = false
}
group = 'se.magnus.microservices.core.recommendation'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

ext {
	mapstructVersion = '1.6.3'
	springCloudVersion = '2025.1.4'
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	implementation project(':api')
	implementation project(':util')
	implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

	compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
	testImplementation 'org.testcontainers:testcontainers-mongodb'  // 교재와 다름 (교재: org.testcontainers:mongodb, 6장부터)
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

### 1-3. review-service
```bash
code microservices/review-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar {
	enabled = false
}
group = 'se.magnus.microservices.core.review'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

ext {
	mapstructVersion = '1.6.3'
	springCloudVersion = '2025.1.4'
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	implementation project(':api')
	implementation project(':util')
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'com.mysql:mysql-connector-j'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)
	implementation 'net.ttddyy.observation:datasource-micrometer-spring-boot:2.2.1'  // ★ 추가 (교재: 1.0.0, Boot 4용은 2.x)
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

	compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'  // 교재에는 없음 (6장부터)
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
	testImplementation 'org.testcontainers:testcontainers-mysql'  // 교재와 다름 (교재: org.testcontainers:mysql, 6장부터)
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

### 1-4. product-composite-service
```bash
code microservices/product-composite-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}
jar { enabled = false }

// ★ 추가 (springBoot 블록): 빌드 정보(버전)를 만들어 추적 정보에 붙인다
springBoot {
	buildInfo()
}
group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

ext {
	springCloudVersion = '2025.1.4'  // (교재: 2025.0.0)
	resilience4jVersion = '2.4.0'
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-webflux'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	implementation project(':api')
	implementation project(':util')
	implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
	implementation 'org.springframework.boot:spring-boot-starter-webclient'
	implementation "io.github.resilience4j:resilience4j-spring-boot4:${resilience4jVersion}"
	implementation "io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}"
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'org.springframework.cloud:spring-cloud-starter-loadbalancer'  // ★ 추가
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)
	testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
	  
}

dependencyManagement {
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
		mavenBom "io.github.resilience4j:resilience4j-bom:${resilience4jVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
	testLogging.showStandardStreams = true  // ★ 추가
}
```

### 1-5. eureka-server
```bash
code spring-cloud/eureka-server/build.gradle
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
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)
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

### 1-6. gateway
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
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
	implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)
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

### 1-7. authorization-server
```bash
code spring-cloud/authorization-server/build.gradle
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
	implementation 'org.springframework.boot:spring-boot-starter-oauth2-authorization-server'
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'
	implementation 'org.springframework.cloud:spring-cloud-config-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
	implementation 'org.springframework.retry:spring-retry'
	implementation 'io.micrometer:micrometer-tracing-bridge-otel'  // ★ 추가
	implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-micrometer-tracing-opentelemetry'  // ★ 추가 (교재에는 없음, Boot 4: 추적 자동 설정 모듈)
	implementation 'org.springframework.boot:spring-boot-zipkin'  // ★ 추가 (교재에는 없음, Boot 4: Zipkin 전송 자동 설정 모듈)

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

## 2. bootstrap.yml (서비스 7개)
로그 줄마다 `[서비스이름,traceId,spanId]`를 찍는다. 같은 요청이면 여러 서비스 로그에 **같은 traceId**가 찍혀서 `grep`으로 따라갈 수 있다.

| 설정 | 뜻 |
|---|---|
| `logging.pattern.correlation` | 로그의 추적 정보 자리 모양. `%X{traceId:-}` = 있으면 traceId, 없으면 빈칸 |
| `logging.include-application-name: false` | 위 패턴에 이미 서비스 이름을 넣었으니 기본으로 찍히는 이름은 끈다(두 번 안 나오게) |

### 2-1. product-service
```bash
code microservices/product-service/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: product

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

### 2-2. recommendation-service
```bash
code microservices/recommendation-service/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: recommendation

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

### 2-3. review-service
```bash
code microservices/review-service/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: review

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

### 2-4. product-composite-service
```bash
code microservices/product-composite-service/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: product-composite

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

### 2-5. eureka-server
```bash
code spring-cloud/eureka-server/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: eureka-server

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

### 2-6. gateway
```bash
code spring-cloud/gateway/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: gateway

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

### 2-7. authorization-server
```bash
code spring-cloud/authorization-server/src/main/resources/bootstrap.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
spring.application.name: auth-server

# ★ 추가 (2줄): 로그 줄마다 [서비스이름,traceId,spanId]
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
logging.include-application-name: false

spring.cloud.config:
  failFast: true
  retry:
    initialInterval: 3000
    multiplier: 1.3
    maxInterval: 10000
    maxAttempts: 20
  uri: http://localhost:8888
  username: ${CONFIG_SERVER_USR}
  password: ${CONFIG_SERVER_PWD}

---
spring.config.activate.on-profile: docker

spring.cloud.config.uri: http://config-server:8888
```

## 3. main()에 컨텍스트 전파 켜기 (4개 + composite는 02)
리액티브 코드는 처리 중에 **스레드가 바뀐다.** traceId는 스레드에 붙어 다니는 값(MDC)이라, 그냥 두면 스레드가 바뀔 때 로그에서 traceId가 사라진다. `Hooks.enableAutomaticContextPropagation()`을 앱 시작 전에 한 번 부르면 리액터가 스레드를 바꿀 때 traceId도 같이 넘겨 준다.

교재처럼 product, recommendation, review, gateway, composite 5개에 넣는다(인가 서버, Eureka는 리액티브가 아니라 필요 없음). composite는 [02](02-composite-observation.md)에서 다른 변경과 함께 한다.

### 3-1. product-service
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/ProductServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import reactor.core.publisher.Hooks;  // ★ 추가
import se.magnus.microservices.core.product.persistence.ProductEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceApplication.class);

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();  // ★ 추가: 리액터 스레드가 바뀌어도 traceId 유지
    ConfigurableApplicationContext ctx = SpringApplication.run(ProductServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ProductEntity.class);
    resolver.resolveIndexFor(ProductEntity.class).forEach(e -> indexOps.createIndex(e).block());
  }
}
```

### 3-2. recommendation-service
```bash
code microservices/recommendation-service/src/main/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import reactor.core.publisher.Hooks;  // ★ 추가
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@SpringBootApplication
@ComponentScan("se.magnus")
public class RecommendationServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceApplication.class);

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();  // ★ 추가: 리액터 스레드가 바뀌어도 traceId 유지
    ConfigurableApplicationContext ctx = SpringApplication.run(RecommendationServiceApplication.class, args);

    String mongodDbHost = ctx.getEnvironment().getProperty("spring.mongodb.host");  // 교재와 다름 (교재: spring.data.mongodb.host, 6장부터)
    String mongodDbPort = ctx.getEnvironment().getProperty("spring.mongodb.port");  // 교재와 다름 (교재: spring.data.mongodb.port, 6장부터)
    LOG.info("Connected to MongoDb: " + mongodDbHost + ":" + mongodDbPort);
  }

  @Autowired
  ReactiveMongoOperations mongoTemplate;

  @EventListener(ContextRefreshedEvent.class)
  public void initIndicesAfterStartup() {

    MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

    ReactiveIndexOperations indexOps = mongoTemplate.indexOps(RecommendationEntity.class);
    resolver.resolveIndexFor(RecommendationEntity.class).forEach(e -> indexOps.createIndex(e).block());
  }
}
```

### 3-3. review-service
```bash
code microservices/review-service/src/main/java/se/magnus/microservices/core/review/ReviewServiceApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.publisher.Hooks;  // ★ 추가
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
@ComponentScan("se.magnus")
public class ReviewServiceApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceApplication.class);

  private final Integer threadPoolSize;
  private final Integer taskQueueSize;

  public ReviewServiceApplication(
    @Value("${app.threadPoolSize:10}") Integer threadPoolSize,
    @Value("${app.taskQueueSize:100}") Integer taskQueueSize
  ) {
    this.threadPoolSize = threadPoolSize;
    this.taskQueueSize = taskQueueSize;
  }

  @Bean
  public Scheduler jdbcScheduler() {
    LOG.info("Creates a jdbcScheduler with thread pool size = {}", threadPoolSize);
    return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "jdbc-pool");
  }

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();  // ★ 추가: 리액터 스레드가 바뀌어도 traceId 유지
    ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);

    String mysqlUri = ctx.getEnvironment().getProperty("spring.datasource.url");
    LOG.info("Connected to MySQL: " + mysqlUri);
  }
}
```

### 3-4. gateway
```bash
code spring-cloud/gateway/src/main/java/se/magnus/springcloud/gateway/GatewayApplication.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;  // ★ 추가

@SpringBootApplication
public class GatewayApplication {

  @Bean
  @LoadBalanced
  public WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
  }

  public static void main(String[] args) {
    Hooks.enableAutomaticContextPropagation();  // ★ 추가: 리액터 스레드가 바뀌어도 traceId 유지
    SpringApplication.run(GatewayApplication.class, args);
  }

}
```

## 확인
composite가 아직 바뀌지 않아서 전체 빌드는 [02](02-composite-observation.md) 끝에서 한다.

다음: [02-composite-observation.md](02-composite-observation.md)
