# 01. springdoc-openapi 의존성 추가

> 표기: 한 줄만 바뀌면 줄 끝에 `★ 추가` / `★ 변경 (전: 원래 줄)` / `★ 삭제: 지울 줄`. 여러 줄이면 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(`★ 전:` 줄 = 지우고 바꿀 원래 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(`★ 지움:` 줄을 지운다). `★ 참고:` = 교재와 다른 이유. 표시 없는 줄 = 이미 있는 줄. ★ 주석은 입력하지 않아도 된다.

`implementation` 줄은 **application.yml이 아니라 build.gradle**에 넣는다(Gradle 의존성 문법). 넣는 곳은 **서로 다른 build.gradle 두 개**다.

## 1. api 프로젝트
```bash
code api/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.api'
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
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    compileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'org.springdoc:springdoc-openapi-starter-common:3.0.3'  // ★ 추가 — 교재: 2.8.6
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2. product-composite-service
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
	implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'  // ★ 추가 — 교재: 2.8.6

}

tasks.named('test') {
	useJUnitPlatform()
}
```

## 왜 모듈이 다른가
| 프로젝트 | 모듈 | 이유 |
|---|---|---|
| api | `springdoc-openapi-starter-common` | 인터페이스에 붙일 **애너테이션**(`@Tag`, `@Operation`, `@ApiResponses` 등)만 필요하다 |
| composite | `springdoc-openapi-starter-webflux-ui` | 실제로 **문서를 만들어 웹으로 제공**하는 기능 + **Swagger UI 화면**까지 필요하다. 우리 서비스가 WebFlux라서 `webflux-ui`를 쓴다 |

## 왜 3.0.3인가 (교재: 2.8.6)
springdoc은 Spring Boot 메이저 버전마다 맞는 버전대가 따로 있다. Maven Central에서 각 버전의 부모 POM(어떤 Spring Boot로 만들어졌는지)을 확인했다.

| springdoc | 기준 Spring Boot |
|---|---|
| 2.8.6 (교재) | 3.4.4 → Boot 3용 |
| 3.0.0 ~ **3.0.3** | **4.0.x** → 우리(4.0.9)와 맞음 |
| 3.1.x ~ 3.2.0 | 4.1.x |

Boot 4.0.x 줄의 최신인 **3.0.3**을 쓴다. 확인 방법:
```bash
curl -s https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi/3.0.3/springdoc-openapi-3.0.3.pom | grep -A3 "<parent>"
```
결과에 `spring-boot-starter-parent` `4.0.5`가 보이면 Boot 4.0.x용이다.
- 교재 테스트(test-em-all.bash)가 기대하는 OpenAPI 버전 `"3.1.0"`은 3.0.3의 기본값(`springdoc.api-docs.version` = `openapi-3-1`)과 같다.
- 경로(`/openapi/swagger-ui.html` 등)와 애너테이션 패키지(`io.swagger.v3.oas...`)는 교재 그대로다.

## 확인
1. 두 build.gradle을 각각 열고 `Shift+Alt+U` (VS Code에 의존성 반영)
2. 의존성을 받아 오는지 빌드:
   ```bash
   ./gradlew build
   ```
   `BUILD SUCCESSFUL`이면 된다. 버전을 잘못 쓰면 `Could not find org.springdoc:...` 오류가 난다.

다음: [02-openapi-config.md](02-openapi-config.md)
