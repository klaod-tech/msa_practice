# 01. 의존성 추가 (build.gradle 5개)

> 표기: `★ 추가` / `★ 변경` = 6장 코드 대비 바뀌는 줄, `교재와 다름 (...)` = Boot 4 때문에 교재 7장과 다르게 쓰는 부분.

| 프로젝트 | 추가/변경 | 이유 |
|---|---|---|
| product, recommendation | `data-mongodb` → **`data-mongodb-reactive`**, Stream(rabbit, kafka), Spring Cloud BOM | 리액티브 리포지토리, 이벤트 소비 |
| review | Stream(rabbit, kafka), Spring Cloud BOM | 이벤트 소비 (JPA는 그대로 블로킹) |
| product-composite | **WebClient**, Stream(rabbit, kafka), test-binder, Spring Cloud BOM | 논블로킹 HTTP, 이벤트 게시 |
| util | JUnit 런처 | `ReactorTests` 실행 |

## 1. product-service
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
	springCloudVersion = '2025.1.4'  // ★ 추가 (교재: 2025.0.0)
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
	implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'  // ★ 변경 (전: spring-boot-starter-data-mongodb)
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

	compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'  // ★ 변경 (전: spring-boot-starter-data-mongodb-test)
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
	testImplementation 'org.testcontainers:testcontainers-mongodb'  // 교재와 다름 (교재: org.testcontainers:mongodb, 6장부터)
}

dependencyManagement {  // ★ 추가 (블록 전체)
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 2. recommendation-service
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
	springCloudVersion = '2025.1.4'  // ★ 추가 (교재: 2025.0.0)
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
	implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'  // ★ 변경 (전: spring-boot-starter-data-mongodb)
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

	compileOnly 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok'  // 교재에는 없음 (6장부터)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 교재에는 없음 (6장부터)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive-test'  // ★ 변경 (전: spring-boot-starter-data-mongodb-test)
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // 교재와 다름 (교재: org.testcontainers:junit-jupiter, 6장부터)
	testImplementation 'org.testcontainers:testcontainers-mongodb'  // 교재와 다름 (교재: org.testcontainers:mongodb, 6장부터)
}

dependencyManagement {  // ★ 추가 (블록 전체)
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 3. review-service
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
	springCloudVersion = '2025.1.4'  // ★ 추가 (교재: 2025.0.0)
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
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
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

dependencyManagement {  // ★ 추가 (블록 전체)
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 4. product-composite-service
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
	testImplementation 'org.springframework.boot:spring-boot-starter-actuator-test'
	testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	implementation project(':api')
	implementation project(':util')
	implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
	implementation 'org.springframework.boot:spring-boot-starter-webclient'  // ★ 추가 (교재에는 없음, Boot 4: WebClient.Builder 제공)
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'  // ★ 추가
	implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'  // ★ 추가
	testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'  // ★ 추가
	  
}

dependencyManagement {  // ★ 추가 (블록 전체)
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
	}
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 5. util
```bash
code util/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.util'
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
    springBootVersion = '4.0.9'
}

dependencies {
    implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    implementation project(':api')
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.junit.platform:junit-platform-launcher'  // ★ 추가 (ReactorTests 실행에 필요)
    compileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 각 줄이 하는 일

| 줄 | 하는 일 |
|---|---|
| `springCloudVersion` + `dependencyManagement { mavenBom ... }` | Spring Cloud 라이브러리들의 **버전을 한 번에 맞춘다**. 그래서 아래 `spring-cloud-starter-*` 줄에는 버전을 안 쓴다 |
| `spring-cloud-starter-stream-rabbit` / `-kafka` | 메시지 브로커 연결(바인더). 둘 다 넣고 yml의 `defaultBinder`로 고른다(기본 rabbit, `kafka` 프로필이면 kafka) |
| `spring-boot-starter-data-mongodb-reactive` | `ReactiveCrudRepository`, `ReactiveMongoOperations`와 리액티브 MongoDB 드라이버 |
| `spring-boot-starter-webclient` | composite가 핵심 서비스를 부를 `WebClient.Builder` 빈 |
| `spring-cloud-stream-test-binder` | 테스트에서 진짜 RabbitMQ 없이 보낸 메시지를 꺼내 볼 수 있게 한다(`MessagingTests`) |

## 교재와 다른 점 (Boot 4)
### ① Spring Cloud 버전 (교재: `2025.0.0`)
Spring Cloud는 Boot 버전마다 짝이 정해져 있다. `2025.0.x`는 Boot 3.5용이라 Boot 4에서는 **`2025.1.x`** 를 쓴다. `2025.1.4`가 Boot 4.0.x용 최신 정식 버전이다(Spring Cloud Stream 5.0.4).

### ② `spring-boot-starter-webclient` (교재에는 없음)
Boot 4에서 WebClient 자동 설정이 webflux 스타터에서 분리됐다. 이 줄이 없으면 composite가 뜰 때 `No qualifying bean of type 'org.springframework.web.reactive.function.client.WebClient$Builder'`로 실패한다.

### ③ 리액티브 MongoDB 테스트 스타터 (6장: `spring-boot-starter-data-mongodb-test`)
본 스타터를 리액티브로 바꿨으니 테스트 스타터도 짝을 맞춘다.

### ④ util의 JUnit 런처
util에는 지금까지 테스트가 없어서 이 줄이 없었다. 없으면 `Failed to load JUnit Platform`으로 실패한다(교재 util build.gradle에는 원래 있는 줄).

## 확인
VS Code에서 `Shift+Alt+U`(Gradle 다시 불러오기) 후:
```bash
./gradlew -q :microservices:product-composite-service:dependencies --configuration runtimeClasspath | grep -oE '(spring-cloud-stream-binder-(rabbit|kafka)|spring-boot-webclient|spring-cloud-stream):[0-9.]+' | sort -u
```
아래 4줄이 나오면 된다.
```
spring-boot-webclient:4.0.9
spring-cloud-stream-binder-kafka:5.0.4
spring-cloud-stream-binder-rabbit:5.0.4
spring-cloud-stream:5.0.4
```
> 이 시점에 `./gradlew build`는 아직 하지 않는다. 코드는 03~06에서 바뀐다.
