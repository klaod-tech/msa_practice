# 01. 의존성 추가 (build.gradle 3개)

> 표기: `★ 추가` = 새로 넣는 줄, `(교재: ...)` = 교재 6장과 다르게 쓰는 값, 표시 없는 줄 = 이미 있는 줄(위치 확인용)

DB를 쓰는 건 **핵심 서비스 3개**다. composite와 api, util은 이번 단계에서 의존성이 바뀌지 않는다.

| 서비스 | DB | 추가하는 것 |
|---|---|---|
| product, recommendation | MongoDB | Spring Data MongoDB, MapStruct, Testcontainers(MongoDB) |
| review | MySQL | Spring Data JPA, MySQL 드라이버, MapStruct, Testcontainers(MySQL) |

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

// ★ 추가 (ext 블록 전체)
ext {
	mapstructVersion = '1.6.3'
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
	implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'  // ★ 추가
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"  // ★ 추가

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가

	compileOnly 'org.projectlombok:lombok'  // ★ 추가 (교재에는 없음, Lombok 엔티티용)
	annotationProcessor 'org.projectlombok:lombok'  // ★ 추가 (교재에는 없음, Lombok 엔티티용)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // ★ 추가 (교재에는 없음, Lombok 엔티티용: MapStruct가 Lombok 결과를 기다리게 함)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-test'  // ★ 추가 (교재에는 없음, Boot 4에서 필요)
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'  // ★ 추가
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // ★ 추가 (교재: org.testcontainers:junit-jupiter)
	testImplementation 'org.testcontainers:testcontainers-mongodb'  // ★ 추가 (교재: org.testcontainers:mongodb)
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 2. recommendation-service
```bash
code microservices/recommendation-service/build.gradle
```
아래처럼 맞추기 (전체 파일, product와 `group`만 다르다)
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

// ★ 추가 (ext 블록 전체)
ext {
	mapstructVersion = '1.6.3'
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
	implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'  // ★ 추가
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"  // ★ 추가

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가

	compileOnly 'org.projectlombok:lombok'  // ★ 추가 (교재에는 없음, Lombok 엔티티용)
	annotationProcessor 'org.projectlombok:lombok'  // ★ 추가 (교재에는 없음, Lombok 엔티티용)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // ★ 추가 (교재에는 없음, Lombok 엔티티용: MapStruct가 Lombok 결과를 기다리게 함)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-mongodb-test'  // ★ 추가 (교재에는 없음, Boot 4에서 필요)
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'  // ★ 추가
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // ★ 추가 (교재: org.testcontainers:junit-jupiter)
	testImplementation 'org.testcontainers:testcontainers-mongodb'  // ★ 추가 (교재: org.testcontainers:mongodb)
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

// ★ 추가 (ext 블록 전체)
ext {
	mapstructVersion = '1.6.3'
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
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'  // ★ 추가
	implementation 'com.mysql:mysql-connector-j'  // ★ 추가
	implementation "org.mapstruct:mapstruct:${mapstructVersion}"  // ★ 추가

	compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가
	annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가
	testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"  // ★ 추가

	compileOnly 'org.projectlombok:lombok'  // ★ 추가 (교재에는 없음, Lombok 엔티티용)
	annotationProcessor 'org.projectlombok:lombok'  // ★ 추가 (교재에는 없음, Lombok 엔티티용)
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // ★ 추가 (교재에는 없음, Lombok 엔티티용: MapStruct가 Lombok 결과를 기다리게 함)

	testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'  // ★ 추가 (교재에는 없음, Boot 4에서 필요)
	testImplementation 'org.springframework.boot:spring-boot-testcontainers'  // ★ 추가
	testImplementation 'org.testcontainers:testcontainers-junit-jupiter'  // ★ 추가 (교재: org.testcontainers:junit-jupiter)
	testImplementation 'org.testcontainers:testcontainers-mysql'  // ★ 추가 (교재: org.testcontainers:mysql)
}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 각 줄이 하는 일
| 줄 | 역할 |
|---|---|
| `spring-boot-starter-data-mongodb` | MongoDB 연결 + `MongoRepository`/`CrudRepository` 자동 구현 |
| `spring-boot-starter-data-jpa` + `mysql-connector-j` | JPA(Hibernate)로 MySQL 테이블 사용 |
| `mapstruct` + `mapstruct-processor` | API 클래스(`Product`) ↔ 엔티티 클래스(`ProductEntity`) 변환 코드를 **컴파일할 때 자동 생성** |
| `compileOnly`/`annotationProcessor`/`testAnnotationProcessor` | 프로세서는 컴파일할 때만 필요하다. 테스트 코드 컴파일에도 쓰도록 `testAnnotationProcessor`까지 넣는다 |
| `spring-boot-starter-data-...-test` | `@DataMongoTest` / `@DataJpaTest` (영속성 계층만 띄우는 테스트) |
| `spring-boot-testcontainers` + `testcontainers-*` | 테스트할 때 **도커로 진짜 DB를 잠깐 띄우고** 끝나면 지운다 |
| `lombok` + `lombok-mapstruct-binding` | 02의 엔티티 getter/setter를 Lombok으로 만든다 (아래 ③) |

## 교재와 다른 점 (Boot 4)

### ① Testcontainers 아티팩트 이름 (교재: `junit-jupiter`, `mongodb`, `mysql`)
Spring Boot 4.0.9는 **Testcontainers 2.0.5**를 관리한다. 2.0부터 모듈 이름 앞에 `testcontainers-`가 붙었다. 교재 이름을 그대로 쓰면 버전을 못 찾는다.
```bash
# 우리 이름: 2.0.5로 잡힘
./gradlew -q :microservices:product-service:dependencies --configuration testRuntimeClasspath | grep "testcontainers-mongodb"
# +--- org.testcontainers:testcontainers-mongodb -> 2.0.5
```
교재 이름(`org.testcontainers:mongodb`)으로 바꿔서 같은 명령을 돌려 보면 `org.testcontainers:mongodb FAILED`가 나온다(확인함).

### ② 테스트 스타터 추가 (교재에는 없음)
Boot 4는 자동 설정을 기술별 모듈로 쪼갰다. `@DataMongoTest`는 `spring-boot-data-mongodb-test`, `@DataJpaTest`는 `spring-boot-data-jpa-test` 안으로 옮겨져서, 해당 테스트 스타터를 넣어야 쓸 수 있다.

### ③ Lombok 3줄 (교재에는 없음)
교재는 엔티티의 getter/setter를 직접 쓰지만, 우리는 api 프로젝트처럼 Lombok을 쓴다. 지금 Lombok은 api 프로젝트에만 있으므로 각 서비스에도 넣는다.
- `lombok` 버전은 Spring Boot가 관리해서(1.18.48) 적지 않는다.
- **`lombok-mapstruct-binding`은 반드시 같이 넣는다.** MapStruct와 Lombok은 둘 다 컴파일할 때 코드를 만드는 도구라서, MapStruct가 먼저 돌면 아직 getter/setter가 없는 엔티티를 보고 실패한다. binding이 "Lombok이 끝난 뒤에 MapStruct가 돌게" 순서를 맞춘다. 이건 Boot가 관리하지 않아서 버전(`0.2.0`)을 적는다.
- 빼먹으면 04의 매퍼 컴파일에서 이런 오류가 난다(실제로 확인함): `error: The type of parameter "entity" has no property named "rating".` → [troubleshooting.md](troubleshooting.md)

## 확인
1. 세 build.gradle을 각각 열고 `Shift+Alt+U` (VS Code에 의존성 반영)
2. 의존성이 다 받아지는지:
   ```bash
   ./gradlew :microservices:product-service:compileJava :microservices:recommendation-service:compileJava :microservices:review-service:compileJava
   ```
   `BUILD SUCCESSFUL`이면 된다. (아직 코드에서 새 라이브러리를 안 써서 컴파일은 그대로 된다)
