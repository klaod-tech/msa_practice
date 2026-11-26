# 02. composite: Prometheus 지표

> 시작: 19장까지 따라 한 상태 / 결과: 20장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

같은 의존성.

## 2-1. `microservices/product-composite-service/build.gradle`
```bash
code microservices/product-composite-service/build.gradle
```
아래처럼 맞추기 (전체 파일)
```groovy
plugins {
    id 'org.springframework.boot' version '4.0.9'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'se.magnus.microservices.composite.product'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

ext {
    springCloudVersion = '2025.1.4'
    resilience4jVersion = '2.4.0'
}
repositories {
    mavenCentral()
}

jar {
    enabled = false
}

springBoot {
    buildInfo()
}

dependencies {
    implementation project(':api')
    implementation project(':util')
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-webclient'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-rabbit'
    implementation 'org.springframework.cloud:spring-cloud-starter-stream-kafka'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'org.springframework.boot:spring-boot-starter-zipkin'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'org.springdoc:springdoc-openapi-starter-webflux-ui:3.0.3'
    implementation "io.github.resilience4j:resilience4j-spring-boot4:${resilience4jVersion}"
    implementation "io.github.resilience4j:resilience4j-reactor:${resilience4jVersion}"
    implementation 'org.springframework.boot:spring-boot-starter-aspectj'
    implementation 'io.micrometer:micrometer-registry-prometheus'  // ★ 추가

    implementation group: 'io.netty', name: 'netty-resolver-dns-native-macos', classifier: 'osx-aarch_64'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
    testImplementation 'org.springframework.cloud:spring-cloud-stream-test-binder'
    testImplementation 'io.projectreactor:reactor-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        mavenBom "io.github.resilience4j:resilience4j-bom:${resilience4jVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

다음: [03-spring-cloud.md](03-spring-cloud.md)
