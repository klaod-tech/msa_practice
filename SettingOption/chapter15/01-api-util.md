# 01. `api`, `util`: 3부 기준으로 맞추기

> 시작: 14장까지 따라 한 상태 / 결과: 15장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

3부 코드와 맞추는 단계다. **Lombok 데이터 클래스(`Product.java` 등)는 건드리지 않는다.**
- `api/build.gradle`: Lombok 줄은 그대로 두고 나머지를 맞춘다.
- 프로젝트마다 `settings.gradle`, `.gitignore`가 새로 생긴다(각 프로젝트를 따로 열어도 빌드되게 하는 파일. 루트 빌드에는 영향 없음).
- `ReviewService`, `Event`(Jackson 3용 생성자), 예외 클래스, `HttpErrorInfo`, `ServiceUtil` 정리.

## 1-1. `api/build.gradle`
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
    implementation 'org.springdoc:springdoc-openapi-starter-common:3.0.3'  // ★ 추가
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    compileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
// ★ 삭제: implementation 'org.springdoc:springdoc-openapi-starter-common:3.0.3'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 1-2. `api/settings.gradle` (새 파일)
```bash
code api/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'api'
```

## 1-3. `api/.gitignore` (새 파일)
```bash
code api/.gitignore
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

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
```

## 1-4. `api/src/main/java/se/magnus/api/core/review/ReviewService.java`
```bash
code api/src/main/java/se/magnus/api/core/review/ReviewService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.core.review;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReviewService {

  Mono<Review> createReview(Review body);

  @GetMapping(
    value = "/review",
    produces = "application/json")
  // ★ 변경 시작 (전 2줄 → 1줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: Flux<Review> getReviews(
  // ★ 전: @RequestParam(value = "productId", required = true) int productId);
  Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
  // ★ 변경 끝

  Mono<Void> deleteReviews(int productId);
}
```

## 1-5. `api/src/main/java/se/magnus/api/event/Event.java`
```bash
code api/src/main/java/se/magnus/api/event/Event.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.event;

import static java.time.ZonedDateTime.now;

import java.time.ZonedDateTime;
// ★ 추가 시작 (2줄)
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;
// ★ 추가 끝

public class Event<K, T> {

  public enum Type {
    CREATE,
    DELETE
  }

  // ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: private final Type eventType;
  // ★ 전: private final K key;
  // ★ 전: private final T data;
  // ★ 전: private final ZonedDateTime eventCreatedAt;
  private Type eventType;
  private K key;
  private T data;
  private ZonedDateTime eventCreatedAt;
  // ★ 변경 끝

  public Event() {
    this.eventType = null;
    this.key = null;
    this.data = null;
    this.eventCreatedAt = null;
  }

  public Event(Type eventType, K key, T data) {
    this.eventType = eventType;
    this.key = key;
    this.data = data;
    this.eventCreatedAt = now();
  }

  public Type getEventType() {
    return eventType;
  }

  public K getKey() {
    return key;
  }

  public T getData() {
    return data;
  }

  @JsonSerialize(using = ZonedDateTimeSerializer.class)  // ★ 추가
  public ZonedDateTime getEventCreatedAt() {
    return eventCreatedAt;
  }
}
```

## 1-6. `api/src/main/java/se/magnus/api/exceptions/InvalidInputException.java`
```bash
code api/src/main/java/se/magnus/api/exceptions/InvalidInputException.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.exceptions;

public class InvalidInputException extends RuntimeException {
  public InvalidInputException() {}

  public InvalidInputException(String message) {
    super(message);
  }

  public InvalidInputException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidInputException(Throwable cause) {
    super(cause);
  }
}
```

## 1-7. `api/src/main/java/se/magnus/api/exceptions/NotFoundException.java`
```bash
code api/src/main/java/se/magnus/api/exceptions/NotFoundException.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.exceptions;

public class NotFoundException extends RuntimeException {
  public NotFoundException() {}

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotFoundException(Throwable cause) {
    super(cause);
  }
}
```

## 1-8. `util/build.gradle`
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
    testImplementation 'org.junit.platform:junit-platform-launcher'
// ★ 삭제 시작 (4줄) — 아래 줄들을 지운다
// ★ 지움: compileOnly platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
// ★ 지움: annotationProcessor platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
// ★ 지움: compileOnly 'org.projectlombok:lombok'
// ★ 지움: annotationProcessor 'org.projectlombok:lombok'
// ★ 삭제 끝
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 1-9. `util/settings.gradle` (새 파일)
```bash
code util/settings.gradle
```
붙여넣기 (새 파일)
```groovy
rootProject.name = 'util'
```

## 1-10. `util/.gitignore` (새 파일)
```bash
code util/.gitignore
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

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
```

## 1-11. `util/src/main/java/se/magnus/util/http/HttpErrorInfo.java`
```bash
code util/src/main/java/se/magnus/util/http/HttpErrorInfo.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.util.http;

import java.time.ZonedDateTime;
import org.springframework.http.HttpStatus;

public class HttpErrorInfo {
  // ★ 변경 시작 (전 4줄 → 4줄) — "전:" 줄은 지우고 그 아래 줄로 바꾼다
  // ★ 전: private final ZonedDateTime timestamp;
  // ★ 전: private final String path;
  // ★ 전: private final HttpStatus httpStatus;
  // ★ 전: private final String message;
  private ZonedDateTime timestamp;
  private String path;
  private HttpStatus httpStatus;
  private String message;
  // ★ 변경 끝

  public HttpErrorInfo() {
    timestamp = null;
    this.httpStatus = null;
    this.path = null;
    this.message = null;
  }

  public HttpErrorInfo(HttpStatus httpStatus, String path, String message) {
    timestamp = ZonedDateTime.now();
    this.httpStatus = httpStatus;
    this.path = path;
    this.message = message;
  }

  public ZonedDateTime getTimestamp() {
    return timestamp;
  }

  public String getPath() {
    return path;
  }

  public int getStatus() {
    return httpStatus.value();
  }

  public String getError() {
    return httpStatus.getReasonPhrase();
  }

  public String getMessage() {
    return message;
  }
}
```

## 1-12. `util/src/main/java/se/magnus/util/http/ServiceUtil.java`
```bash
code util/src/main/java/se/magnus/util/http/ServiceUtil.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.util.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);

  private final String port;

  private String serviceAddress = null;

  public ServiceUtil(@Value("${server.port}") String port) {

    this.port = port;
  }

  public String getServiceAddress() {
    if (serviceAddress == null) {
      serviceAddress = findMyHostname() + "/" + findMyIpAddress() + ":" + port;
    }
    return serviceAddress;
  }

  private String findMyHostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown host name";
    }
  }

  private String findMyIpAddress() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "unknown IP address";
    }
  }
}
```

다음: [02-core-services.md](02-core-services.md)
