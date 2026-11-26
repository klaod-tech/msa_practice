# 03. 인가 서버: Prometheus 지표

> 시작: 19장까지 따라 한 상태 / 결과: 20장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

인가 서버도 같은 의존성.

## 3-1. `spring-cloud/authorization-server/build.gradle`
```bash
code spring-cloud/authorization-server/build.gradle
```
아래처럼 맞추기 (전체 파일)
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

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-authorization-server'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.micrometer:micrometer-tracing-bridge-brave'
    implementation 'org.springframework.boot:spring-boot-starter-zipkin'
    implementation 'io.micrometer:micrometer-registry-prometheus'  // ★ 추가

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-security-test'
    testImplementation 'org.htmlunit:htmlunit'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

다음: [04-config-repo.md](04-config-repo.md)
