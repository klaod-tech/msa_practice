# 03. 서비스 7개를 설정 서버의 클라이언트로 바꾸기

> 표기: `★ 추가` / `★ 변경` / `★ 삭제` = 11장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 / Spring Cloud 2025.1 때문에 교재 12장과 다르게 쓰는 부분.
> 교재 "마이크로서비스가 컨피그 서버를 사용하도록 설정" 절.

## 무엇을 하나
서비스마다 세 가지를 한다.
1. **build.gradle**: 설정 클라이언트 의존성 5줄 추가
2. **bootstrap.yml** (새 파일): "내 이름은 무엇이고 설정 서버는 어디 있다"만 적는다
3. **application.yml 삭제**: 내용은 [02](02-config-repo.md)의 config-repo로 옮겼다

```
서비스 시작 → bootstrap.yml 읽음 → 설정 서버(8888)에 "나 product, 프로필 docker" 요청
           → application.yml + product.yml 받아서 → 그 설정으로 나머지 시작
```

추가하는 의존성(7개 공통):

| 의존성 | 하는 일 |
|---|---|
| `spring-cloud-starter-config`, `spring-cloud-config-client` | 설정 서버에서 설정을 받아 오는 클라이언트 |
| `spring-cloud-starter-bootstrap` | `bootstrap.yml`을 `application.yml`보다 **먼저** 읽게 한다 |
| `spring-boot-starter-aspectj` | 재시도(retry)가 AOP로 동작해서 필요. **교재와 다름**: 교재는 `spring-boot-starter-aop`인데 Boot 4에서 이름이 `spring-boot-starter-aspectj`로 바뀌었다(예전 이름은 Boot 4 BOM에 없어서 버전을 못 찾는다) |
| `spring-retry` | 설정 서버가 아직 안 떴으면 기다렸다 다시 시도 |

bootstrap.yml 내용(7개 모두 같고 **첫 줄 이름만** 다르다):

| 설정 | 뜻 |
|---|---|
| `spring.application.name` | config-repo에서 찾을 파일 이름(`product` → `product.yml`). Eureka 등록 이름도 이것 |
| `failFast: true` | 설정을 못 받으면 **시작하지 않고** 끝낸다(잘못된 설정으로 뜨는 것보다 낫다) |
| `retry` | 3초 → 1.3배씩 늘려 최대 10초 간격으로 최대 20번 다시 시도. 도커에서 설정 서버가 늦게 떠도 기다린다 |
| `uri` | 로컬 `http://localhost:8888`, docker 프로필이면 `http://config-server:8888` |
| `username`/`password` | `${CONFIG_SERVER_USR}`/`${CONFIG_SERVER_PWD}` 환경변수(compose가 `.env` 값을 넣어 줌) |

## 1. build.gradle + bootstrap.yml (서비스 7개)
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
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가
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

```bash
code microservices/product-service/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: product

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
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가
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

```bash
code microservices/recommendation-service/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: recommendation

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
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가
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

```bash
code microservices/review-service/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: review

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
group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

ext {
	springCloudVersion = '2025.1.4'  // (교재: 2025.0.0)
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
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가
	testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
	  
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

```bash
code microservices/product-composite-service/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: product-composite

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
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가
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

```bash
code spring-cloud/eureka-server/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: eureka-server

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
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가
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

```bash
code spring-cloud/gateway/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: gateway

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
	implementation 'org.springframework.cloud:spring-cloud-starter-config'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-config-client'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-bootstrap'  // ★ 추가
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'  // ★ 추가 (교재: spring-boot-starter-aop, Boot 4에서 이름 바뀜)
	implementation 'org.springframework.retry:spring-retry'  // ★ 추가

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

```bash
code spring-cloud/authorization-server/src/main/resources/bootstrap.yml
```
붙여넣기 (새 파일)
```yaml
spring.application.name: auth-server

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


## 2. application.yml 삭제 (서비스 7개)
**[02](02-config-repo.md)의 config-repo 파일을 다 만든 뒤에** 지운다.
```bash
cd ~/msa
rm microservices/product-service/src/main/resources/application.yml
rm microservices/recommendation-service/src/main/resources/application.yml
rm microservices/review-service/src/main/resources/application.yml
rm microservices/product-composite-service/src/main/resources/application.yml
rm spring-cloud/eureka-server/src/main/resources/application.yml
rm spring-cloud/gateway/src/main/resources/application.yml
rm spring-cloud/authorization-server/src/main/resources/application.yml
```
지우고 나면 각 서비스의 `src/main/resources`에는 `bootstrap.yml`만 남는다(gateway는 `keystore/`도).
```bash
ls microservices/*/src/main/resources spring-cloud/*/src/main/resources
```

## 3. 테스트: 설정 서버 끄기
테스트에는 설정 서버가 없다. `spring.cloud.config.enabled=false`를 넣지 않으면 테스트가 `localhost:8888`에 접속하려다 실패한다(`failFast`).

### 3-1. ProductServiceApplicationTests.java (product-service)
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/ProductServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "eureka.client.enabled=false",
  "spring.cloud.config.enabled=false"})  // ★ 추가
class ProductServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ProductRepository repository;

  @Autowired
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Product>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();
  }

  @Test
  void getProductById() {

    int productId = 1;

    assertNull(repository.findByProductId(productId).block());
    assertEquals(0, (long)repository.count().block());

    sendCreateProductEvent(productId);

    assertNotNull(repository.findByProductId(productId).block());
    assertEquals(1, (long)repository.count().block());

    getAndVerifyProduct(productId, OK)
      .jsonPath("$.productId").isEqualTo(productId);
  }

  @Test
  void duplicateError() {

    int productId = 1;

    assertNull(repository.findByProductId(productId).block());

    sendCreateProductEvent(productId);

    assertNotNull(repository.findByProductId(productId).block());

    InvalidInputException thrown = assertThrows(
      InvalidInputException.class,
      () -> sendCreateProductEvent(productId),
      "Expected a InvalidInputException here!");
    assertEquals("Duplicate key, Product Id: " + productId, thrown.getMessage());
  }

  @Test
  void deleteProduct() {

    int productId = 1;

    sendCreateProductEvent(productId);
    assertNotNull(repository.findByProductId(productId).block());

    sendDeleteProductEvent(productId);
    assertNull(repository.findByProductId(productId).block());

    sendDeleteProductEvent(productId);
  }

  @Test
  void getProductInvalidParameterString() {

    getAndVerifyProduct("/no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/product/no-integer")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getProductNotFound() {

    int productIdNotFound = 13;
    getAndVerifyProduct(productIdNotFound, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
      .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
  }

  @Test
  void getProductInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return getAndVerifyProduct("/" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
    return client.get()
      .uri("/product" + productIdPath)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void sendCreateProductEvent(int productId) {
    Product product = new Product(productId, "Name " + productId, productId, "SA");
    Event<Integer, Product> event = new Event<>(CREATE, productId, product);
    messageProcessor.accept(event);
  }

  private void sendDeleteProductEvent(int productId) {
    Event<Integer, Product> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

### 3-2. PersistenceTests.java (product-service)
```bash
code microservices/product-service/src/test/java/se/magnus/microservices/core/product/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // 교재와 다름 (6장부터)
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})  // ★ 변경 (전: @DataMongoTest)
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();

    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(repository.save(entity))
      .expectNextMatches(createdEntity -> {
        savedEntity = createdEntity;
        return areProductEqual(entity, savedEntity);
      })
      .verifyComplete();
  }

  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);

    StepVerifier.create(repository.save(newEntity))
      .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
      .verifyComplete();

    StepVerifier.create(repository.findById(newEntity.getId()))
      .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
      .verifyComplete();

    StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    StepVerifier.create(repository.save(savedEntity))
      .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
      .verifyComplete();

    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n2"))
      .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
    StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
  }

  @Test
  void getByProductId() {

    StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
      .expectNextMatches(foundEntity -> areProductEqual(savedEntity, foundEntity))
      .verifyComplete();
  }

  @Test
  void duplicateError() {
    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
    StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {

    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

    entity1.setName("n1");
    repository.save(entity1).block();

    StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n1"))
      .verifyComplete();
  }

  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return
      (expectedEntity.getId().equals(actualEntity.getId()))
      && (expectedEntity.getVersion() == actualEntity.getVersion())
      && (expectedEntity.getProductId() == actualEntity.getProductId())
      && (expectedEntity.getName().equals(actualEntity.getName()))
      && (expectedEntity.getWeight() == actualEntity.getWeight());
  }
}
```

### 3-3. RecommendationServiceApplicationTests.java (recommendation-service)
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/RecommendationServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "eureka.client.enabled=false",
  "spring.cloud.config.enabled=false"})  // ★ 추가
class RecommendationServiceApplicationTests extends MongoDbTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private RecommendationRepository repository;

  @Autowired
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Recommendation>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();
  }

  @Test
  void getRecommendationsByProductId() {

    int productId = 1;

    sendCreateRecommendationEvent(productId, 1);
    sendCreateRecommendationEvent(productId, 2);
    sendCreateRecommendationEvent(productId, 3);

    assertEquals(3, (long)repository.findByProductId(productId).count().block());

    getAndVerifyRecommendationsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].recommendationId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int recommendationId = 1;

    sendCreateRecommendationEvent(productId, recommendationId);

    assertEquals(1, (long)repository.count().block());

    InvalidInputException thrown = assertThrows(
      InvalidInputException.class,
      () -> sendCreateRecommendationEvent(productId, recommendationId),
      "Expected a InvalidInputException here!");
    assertEquals("Duplicate key, Product Id: 1, Recommendation Id:1", thrown.getMessage());

    assertEquals(1, (long)repository.count().block());
  }

  @Test
  void deleteRecommendations() {

    int productId = 1;
    int recommendationId = 1;

    sendCreateRecommendationEvent(productId, recommendationId);
    assertEquals(1, (long)repository.findByProductId(productId).count().block());

    sendDeleteRecommendationEvent(productId);
    assertEquals(0, (long)repository.findByProductId(productId).count().block());

    sendDeleteRecommendationEvent(productId);
  }

  @Test
  void getRecommendationsMissingParameter() {

    getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getRecommendationsInvalidParameter() {

    getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getRecommendationsNotFound() {

    getAndVerifyRecommendationsByProductId("?productId=113", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getRecommendationsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/recommendation")
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
    return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus expectedStatus) {
    return client.get()
      .uri("/recommendation" + productIdQuery)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void sendCreateRecommendationEvent(int productId, int recommendationId) {
    Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "SA");
    Event<Integer, Recommendation> event = new Event<>(CREATE, productId, recommendation);
    messageProcessor.accept(event);
  }

  private void sendDeleteRecommendationEvent(int productId) {
    Event<Integer, Recommendation> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

### 3-4. PersistenceTests.java (recommendation-service)
```bash
code microservices/recommendation-service/src/test/java/se/magnus/microservices/core/recommendation/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.recommendation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;  // 교재와 다름 (6장부터)
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})  // ★ 변경 (전: @DataMongoTest)
class PersistenceTests extends MongoDbTestBase {

  @Autowired
  private RecommendationRepository repository;

  private RecommendationEntity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();

    RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
    savedEntity = repository.save(entity).block();

    assertEqualsRecommendation(entity, savedEntity);
  }

  @Test
  void create() {

    RecommendationEntity newEntity = new RecommendationEntity(1, 3, "a", 3, "c");
    repository.save(newEntity).block();

    RecommendationEntity foundEntity = repository.findById(newEntity.getId()).block();
    assertEqualsRecommendation(newEntity, foundEntity);

    assertEquals(2, (long)repository.count().block());
  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    repository.save(savedEntity).block();

    RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).block();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("a2", foundEntity.getAuthor());
  }

  @Test
  void delete() {
    repository.delete(savedEntity).block();
    assertFalse(repository.existsById(savedEntity.getId()).block());
  }

  @Test
  void getByProductId() {
    List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId()).collectList().block();

    assertThat(entityList, hasSize(1));
    assertEqualsRecommendation(savedEntity, entityList.get(0));
  }

  @Test
  void duplicateError() {
    assertThrows(DuplicateKeyException.class, () -> {
      RecommendationEntity entity = new RecommendationEntity(1, 2, "a", 3, "c");
      repository.save(entity).block();
    });
  }

  @Test
  void optimisticLockError() {

    RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
    RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();

    entity1.setAuthor("a1");
    repository.save(entity1).block();

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2).block();
    });

    RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).block();
    assertEquals(1, (int)updatedEntity.getVersion());
    assertEquals("a1", updatedEntity.getAuthor());
  }

  private void assertEqualsRecommendation(RecommendationEntity expectedEntity, RecommendationEntity actualEntity) {
    assertEquals(expectedEntity.getId(),               actualEntity.getId());
    assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
    assertEquals(expectedEntity.getRecommendationId(), actualEntity.getRecommendationId());
    assertEquals(expectedEntity.getAuthor(),           actualEntity.getAuthor());
    assertEquals(expectedEntity.getRating(),           actualEntity.getRating());
    assertEquals(expectedEntity.getContent(),          actualEntity.getContent());
  }
}
```

### 3-5. ReviewServiceApplicationTests.java (review-service)
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/ReviewServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
  "spring.cloud.stream.defaultBinder=rabbit",
  "logging.level.se.magnus=DEBUG",
  "eureka.client.enabled=false",
  "spring.jpa.hibernate.ddl-auto=update",  // ★ 추가
  "spring.cloud.config.enabled=false"})  // ★ 추가
class ReviewServiceApplicationTests extends MySqlTestBase {

  @Autowired
  private WebTestClient client;

  @Autowired
  private ReviewRepository repository;

  @Autowired
  @Qualifier("messageProcessor")
  private Consumer<Event<Integer, Review>> messageProcessor;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();
  }

  @Test
  void getReviewsByProductId() {

    int productId = 1;

    assertEquals(0, repository.findByProductId(productId).size());

    sendCreateReviewEvent(productId, 1);
    sendCreateReviewEvent(productId, 2);
    sendCreateReviewEvent(productId, 3);

    assertEquals(3, repository.findByProductId(productId).size());

    getAndVerifyReviewsByProductId(productId, OK)
      .jsonPath("$.length()").isEqualTo(3)
      .jsonPath("$[2].productId").isEqualTo(productId)
      .jsonPath("$[2].reviewId").isEqualTo(3);
  }

  @Test
  void duplicateError() {

    int productId = 1;
    int reviewId = 1;

    assertEquals(0, repository.count());

    sendCreateReviewEvent(productId, reviewId);

    assertEquals(1, repository.count());

    InvalidInputException thrown = assertThrows(
      InvalidInputException.class,
      () -> sendCreateReviewEvent(productId, reviewId),
      "Expected a InvalidInputException here!");
    assertEquals("Duplicate key, Product Id: 1, Review Id:1", thrown.getMessage());

    assertEquals(1, repository.count());
  }

  @Test
  void deleteReviews() {

    int productId = 1;
    int reviewId = 1;

    sendCreateReviewEvent(productId, reviewId);
    assertEquals(1, repository.findByProductId(productId).size());

    sendDeleteReviewEvent(productId);
    assertEquals(0, repository.findByProductId(productId).size());

    sendDeleteReviewEvent(productId);
  }

  @Test
  void getReviewsMissingParameter() {

    getAndVerifyReviewsByProductId("", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Required query parameter 'productId' is not present.");
  }

  @Test
  void getReviewsInvalidParameter() {

    getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Type mismatch.");
  }

  @Test
  void getReviewsNotFound() {

    getAndVerifyReviewsByProductId("?productId=213", OK)
      .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getReviewsInvalidParameterNegativeValue() {

    int productIdInvalid = -1;

    getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/review")
      .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
  }

  private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
    return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
  }

  private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
    return client.get()
      .uri("/review" + productIdQuery)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }

  private void sendCreateReviewEvent(int productId, int reviewId) {
    Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
    Event<Integer, Review> event = new Event<>(CREATE, productId, review);
    messageProcessor.accept(event);
  }

  private void sendDeleteReviewEvent(int productId) {
    Event<Integer, Review> event = new Event<>(DELETE, productId, null);
    messageProcessor.accept(event);
  }
}
```

### 3-6. PersistenceTests.java (review-service)
```bash
code microservices/review-service/src/test/java/se/magnus/microservices/core/review/PersistenceTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.core.review;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=update", "spring.cloud.config.enabled=false"})  // ★ 변경 (전: @DataJpaTest)
@Transactional(propagation = NOT_SUPPORTED)
class PersistenceTests extends MySqlTestBase {

  @Autowired
  private ReviewRepository repository;

  private ReviewEntity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll();

    ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
    savedEntity = repository.save(entity);

    assertEqualsReview(entity, savedEntity);
  }

  @Test
  void create() {

    ReviewEntity newEntity = new ReviewEntity(1, 3, "a", "s", "c");
    repository.save(newEntity);

    ReviewEntity foundEntity = repository.findById(newEntity.getId()).get();
    assertEqualsReview(newEntity, foundEntity);

    assertEquals(2, repository.count());
  }

  @Test
  void update() {
    savedEntity.setAuthor("a2");
    repository.save(savedEntity);

    ReviewEntity foundEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (long)foundEntity.getVersion());
    assertEquals("a2", foundEntity.getAuthor());
  }

  @Test
  void delete() {
    repository.delete(savedEntity);
    assertFalse(repository.existsById(savedEntity.getId()));
  }

  @Test
  void getByProductId() {
    List<ReviewEntity> entityList = repository.findByProductId(savedEntity.getProductId());

    assertThat(entityList, hasSize(1));
    assertEqualsReview(savedEntity, entityList.get(0));
  }

  @Test
  void duplicateError() {
    assertThrows(DataIntegrityViolationException.class, () -> {
      ReviewEntity entity = new ReviewEntity(1, 2, "a", "s", "c");
      repository.save(entity);
    });

  }

  @Test
  void optimisticLockError() {

    ReviewEntity entity1 = repository.findById(savedEntity.getId()).get();
    ReviewEntity entity2 = repository.findById(savedEntity.getId()).get();

    entity1.setAuthor("a1");
    repository.save(entity1);

    assertThrows(OptimisticLockingFailureException.class, () -> {
      entity2.setAuthor("a2");
      repository.save(entity2);
    });

    ReviewEntity updatedEntity = repository.findById(savedEntity.getId()).get();
    assertEquals(1, (int)updatedEntity.getVersion());
    assertEquals("a1", updatedEntity.getAuthor());
  }

  private void assertEqualsReview(ReviewEntity expectedEntity, ReviewEntity actualEntity) {
    assertEquals(expectedEntity.getId(),        actualEntity.getId());
    assertEquals(expectedEntity.getVersion(),   actualEntity.getVersion());
    assertEquals(expectedEntity.getProductId(), actualEntity.getProductId());
    assertEquals(expectedEntity.getReviewId(),  actualEntity.getReviewId());
    assertEquals(expectedEntity.getAuthor(),    actualEntity.getAuthor());
    assertEquals(expectedEntity.getSubject(),   actualEntity.getSubject());
    assertEquals(expectedEntity.getContent(),   actualEntity.getContent());
  }
}
```

### 3-7. ProductCompositeServiceApplicationTests.java (product-composite-service)
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/ProductCompositeServiceApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true",
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"})  // ★ 추가
@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
class ProductCompositeServiceApplicationTests {

  private static final int PRODUCT_ID_OK = 1;
  private static final int PRODUCT_ID_NOT_FOUND = 2;
  private static final int PRODUCT_ID_INVALID = 3;

  @Autowired
  private WebTestClient client;

  @MockitoBean
  private ProductCompositeIntegration compositeIntegration;

  @BeforeEach
  void setUp() {

    when(compositeIntegration.getProduct(PRODUCT_ID_OK))
      .thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));

    when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address"))));

    when(compositeIntegration.getReviews(PRODUCT_ID_OK))
      .thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));

    when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND)).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

    when(compositeIntegration.getProduct(PRODUCT_ID_INVALID)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
  }

  @Test
  void contextLoads() {
  }

  @Test
  void getProductById() {

    getAndVerifyProduct(PRODUCT_ID_OK, OK)
      .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
      .jsonPath("$.recommendations.length()").isEqualTo(1)
      .jsonPath("$.reviews.length()").isEqualTo(1);
  }

  @Test
  void getProductNotFound() {

    getAndVerifyProduct(PRODUCT_ID_NOT_FOUND, NOT_FOUND)
      .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
      .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
  }

  @Test
  void getProductInvalidInput() {

    getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_CONTENT)  // 교재와 다름 (교재: UNPROCESSABLE_ENTITY)
      .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
      .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
  }

  private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    return client.get()
      .uri("/product-composite/" + productId)
      .accept(APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus)
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody();
  }
}
```

### 3-8. MessagingTests.java (product-composite-service)
```bash
code microservices/product-composite-service/src/test/java/se/magnus/microservices/composite/product/MessagingTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.microservices.composite.product;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static reactor.core.publisher.Mono.just;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;  // 교재와 다름 (Boot 4)
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;

@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  classes = {TestSecurityConfig.class},
  properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.main.allow-bean-definition-overriding=true",
    "eureka.client.enabled=false",
    "spring.cloud.stream.defaultBinder=rabbit",  // ★ 추가
    "spring.cloud.config.enabled=false"})  // ★ 추가
@Import({TestChannelBinderConfiguration.class})
@AutoConfigureWebTestClient  // 교재와 다름 (Boot 4)
class MessagingTests {

  private static final Logger LOG = LoggerFactory.getLogger(MessagingTests.class);

  @Autowired
  private WebTestClient client;

  @Autowired
  private OutputDestination target;

  @BeforeEach
  void setUp() {
    purgeMessages("products");
    purgeMessages("recommendations");
    purgeMessages("reviews");
  }

  @Test
  void createCompositeProduct1() {

    ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
    postAndVerifyProduct(composite, ACCEPTED);

    final List<String> productMessages = getMessages("products");
    final List<String> recommendationMessages = getMessages("recommendations");
    final List<String> reviewMessages = getMessages("reviews");

    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedEvent =
      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedEvent)));

    assertEquals(0, recommendationMessages.size());
    assertEquals(0, reviewMessages.size());
  }

  @Test
  void createCompositeProduct2() {

    ProductAggregate composite = new ProductAggregate(1, "name", 1,
      singletonList(new RecommendationSummary(1, "a", 1, "c")),
      singletonList(new ReviewSummary(1, "a", "s", "c")), null);
    postAndVerifyProduct(composite, ACCEPTED);

    final List<String> productMessages = getMessages("products");
    final List<String> recommendationMessages = getMessages("recommendations");
    final List<String> reviewMessages = getMessages("reviews");

    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedProductEvent =
      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

    assertEquals(1, recommendationMessages.size());

    RecommendationSummary rec = composite.getRecommendations().get(0);
    Event<Integer, Recommendation> expectedRecommendationEvent =
      new Event<>(CREATE, composite.getProductId(),
        new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(), rec.getRate(), rec.getContent(), null));
    assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

    assertEquals(1, reviewMessages.size());

    ReviewSummary rev = composite.getReviews().get(0);
    Event<Integer, Review> expectedReviewEvent =
      new Event<>(CREATE, composite.getProductId(), new Review(composite.getProductId(), rev.getReviewId(), rev.getAuthor(), rev.getSubject(), rev.getContent(), null));
    assertThat(reviewMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
  }

  @Test
  void deleteCompositeProduct() {
    deleteAndVerifyProduct(1, ACCEPTED);

    final List<String> productMessages = getMessages("products");
    final List<String> recommendationMessages = getMessages("recommendations");
    final List<String> reviewMessages = getMessages("reviews");

    assertEquals(1, productMessages.size());

    Event<Integer, Product> expectedProductEvent = new Event<>(DELETE, 1, null);
    assertThat(productMessages.get(0), is(sameEventExceptCreatedAt(expectedProductEvent)));

    assertEquals(1, recommendationMessages.size());

    Event<Integer, Recommendation> expectedRecommendationEvent = new Event<>(DELETE, 1, null);
    assertThat(recommendationMessages.get(0), is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

    assertEquals(1, reviewMessages.size());

    Event<Integer, Review> expectedReviewEvent = new Event<>(DELETE, 1, null);
    assertThat(reviewMessages.get(0), is(sameEventExceptCreatedAt(expectedReviewEvent)));
  }

  private void purgeMessages(String bindingName) {
    getMessages(bindingName);
  }

  private List<String> getMessages(String bindingName) {
    List<String> messages = new ArrayList<>();
    boolean anyMoreMessages = true;

    while (anyMoreMessages) {
      Message<byte[]> message = getMessage(bindingName);

      if (message == null) {
        anyMoreMessages = false;

      } else {
        messages.add(new String(message.getPayload()));
      }
    }
    return messages;
  }

  private Message<byte[]> getMessage(String bindingName) {
    try {
      return target.receive(0, bindingName);
    } catch (NullPointerException npe) {
      LOG.error("getMessage() received a NPE with binding = {}", bindingName);
      return null;
    }
  }

  private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
    client.post()
      .uri("/product-composite")
      .body(just(compositeProduct), ProductAggregate.class)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus);
  }

  private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
    client.delete()
      .uri("/product-composite/" + productId)
      .exchange()
      .expectStatus().isEqualTo(expectedStatus);
  }
}
```

### 3-9. EurekaServerApplicationTests.java (eureka-server)
```bash
code spring-cloud/eureka-server/src/test/java/se/magnus/springcloud/eurekaserver/EurekaServerApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;  // 교재와 다름 (교재: org.springframework.boot.test.web.client.TestRestTemplate)
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;  // 교재에는 없음 (Boot 4)
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.cloud.config.enabled=false"})  // ★ 변경 (properties 추가)
@AutoConfigureTestRestTemplate  // 교재에는 없음 (Boot 4)
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
    String expectedReponseBody = "{\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}";  // 교재와 다름 (교재: {"status":"UP"}, Boot 4는 groups도 보여 줌)
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, entity.getStatusCode());
    assertEquals(expectedReponseBody, entity.getBody());
  }
}
```

### 3-10. GatewayApplicationTests.java (gateway)
```bash
code spring-cloud/gateway/src/test/java/se/magnus/springcloud/gateway/GatewayApplicationTests.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.springcloud.gateway;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT,
  properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"})  // ★ 추가
class GatewayApplicationTests {

  @Test
  void contextLoads() {
  }

}
```

## 4. 테스트용 설정 (새 파일)
application.yml이 없어졌으니 테스트에 필요한 최소 설정을 `src/test/resources/application.yml`에 둔다. product, recommendation, review는 `src/test/resources`가 이미 있다(logback-test.xml).

### 4-1. product-service
```bash
code microservices/product-service/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

spring.cloud.stream.defaultBinder: rabbit
```

### 4-2. recommendation-service
```bash
code microservices/recommendation-service/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

spring.cloud.stream.defaultBinder: rabbit
```

### 4-3. review-service
```bash
code microservices/review-service/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
spring.web.error.include-message: always  # 교재와 다름 (교재: server.error.include-message)
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

spring.cloud.stream.defaultBinder: rabbit

logging:
  level:
    root: INFO
    se.magnus: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 4-4. product-composite-service
```bash
mkdir -p microservices/product-composite-service/src/test/resources
code microservices/product-composite-service/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
spring.jackson.mapper.allow-final-fields-as-mutators: true  # 교재와 다름 (Jackson 3, 7장부터)

api:

  common:
    version:
    title:
    description:
    termsOfService:
    license:
    licenseUrl:
    externalDocDesc:
    externalDocUrl:
    contact:
      name:
      url:
      email:

spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings:
    products-out-0.destination: products
    recommendations-out-0.destination: recommendations
    reviews-out-0.destination: reviews
```

### 4-5. eureka-server
```bash
mkdir -p spring-cloud/eureka-server/src/test/resources
code spring-cloud/eureka-server/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
app:
  eureka-username: u
  eureka-password: p
```

### 4-6. gateway
```bash
mkdir -p spring-cloud/gateway/src/test/resources
code spring-cloud/gateway/src/test/resources/application.yml
```
붙여넣기 (새 파일)
```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri: .
```

- 핵심 서비스 3개와 composite: 3장부터 쓰던 `spring.web.error.include-message`(오류 메시지 응답에 포함), 7장부터 쓰던 Jackson 설정. **교재와 다름**: 교재는 `server.error.include-message`, Jackson 줄은 없음. 특히 Jackson 줄이 없으면 composite의 `MessagingTests` 2개가 이벤트 내용이 비어(`productId: 0`, `name: null`) 실패한다([troubleshooting](troubleshooting.md)).
- gateway: 테스트에는 인가 서버가 없으니 JWT 공개키 주소에 아무 값(`.`)이나 넣어 앱만 뜨게 한다.
- eureka-server: 기본 인증 계정(평소에는 설정 서버의 공통 application.yml에서 받는 값).
- 인가 서버는 테스트용 설정이 필요 없다(11장부터 테스트에 `spring.cloud.config.enabled=false`가 이미 있다). 교재에는 `ml.`로 시작하는 쓰이지 않는 설정 파일이 있는데 만들지 않는다.

## 5. gateway SecurityConfig
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
          .pathMatchers("/eureka/**").permitAll()
          .pathMatchers("/oauth2/**").permitAll()
          .pathMatchers("/login/**").permitAll()
          .pathMatchers("/error/**").permitAll()
          .pathMatchers("/openapi/**").permitAll()
          .pathMatchers("/config/**").permitAll()  // ★ 추가
          .anyExchange().authenticated()))
      .oauth2ResourceServer(server -> server
        .jwt(Customizer.withDefaults()));
    return http.build();
  }

}
```

`/config/**`는 토큰 없이 통과시킨다. 대신 설정 서버가 자기 계정(`dev-usr:dev-pwd`)으로 막는다.

## 확인
도커 없이 도는 테스트:
```bash
cd ~/msa
./gradlew :spring-cloud:config-server:build :spring-cloud:eureka-server:build :spring-cloud:gateway:build :spring-cloud:authorization-server:build :microservices:product-composite-service:build
```
`BUILD SUCCESSFUL`. 테스트 수: config-server 1, eureka-server 3, gateway 1, authorization-server 4, composite 8.

전체 빌드(핵심 서비스 테스트가 Testcontainers로 MongoDB/MySQL 컨테이너를 띄우므로 도커 필요):
```bash
./gradlew build
```
`BUILD SUCCESSFUL`. 핵심 서비스 테스트 수: product 13, recommendation 15, review 15.

다음: [04-docker-compose-and-test.md](04-docker-compose-and-test.md)
