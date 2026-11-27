# 01. `api`, `util`: 3부 기준으로 맞추기

> 시작: 14장까지 따라 한 상태 / 결과: 15장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

## 0. (2부 코드가 없는 시점에서 진행) 14장 코드 받아서 시작하기
> **2부(9~14장)를 하지 않고 3부부터 시작할 때만** 한다. 1~14장을 직접 따라 왔으면 건너뛰고 바로 아래 1-1부터 한다.

3부는 2부에서 만든 시스템(gateway, 인가 서버, 설정 서버, Eureka 등)을 쿠버네티스에 올리는 과정이라 **14장 끝 상태의 코드가 있어야** 한다. 그 코드를 이 레포의 [`start-ch14/`](start-ch14/) 폴더에 넣어 두었다(SettingOption 1~14장 문서를 그대로 따라 한 결과, 빌드·test-em-all 통과 확인).

준비: 1장(도구 설치, 3부를 하려면 4절까지)과 2장 0~2절(Ubuntu 터미널, VS Code)을 해 둔다. `~/msa` 폴더는 만들지 않아도 된다(아래에서 만든다).

Ubuntu 터미널에서:
```bash
cd ~
git clone https://github.com/klaod-tech/msa_practice.git msa-repo
mkdir -p ~/msa
cp -r ~/msa-repo/SettingOption/chapter15/start-ch14/. ~/msa/
cd ~/msa
chmod +x gradlew test-em-all.bash
ls
```
- `ls`에 `api`, `util`, `microservices`, `spring-cloud`, `config-repo`, `docker-compose.yml`, `test-em-all.bash`가 보이면 된다.
- 레포가 비공개(private)면 `git clone` 때 GitHub 아이디와 토큰(비밀번호 칸)을 묻는다 → [2장 6절](../chapter2/02-project-folder.md)의 토큰 만들기.
- `~/msa`가 이미 있으면(다른 실습 코드가 들어 있으면) 다른 이름(예: `~/msa3`)으로 만들고, 이 장부터 문서의 `~/msa`를 그 이름으로 바꿔 읽는다.

14장 상태가 제대로 도는지 확인(선택, Docker Desktop이 켜져 있어야 함):
```bash
cd ~/msa
code .
./gradlew build -x test
docker compose build
./test-em-all.bash start stop
```
마지막 줄 `End, all tests OK`면 14장 상태가 맞다. 처음에는 이미지를 받느라 오래 걸린다.

로컬 커밋을 쓰려면(캡처·커밋 연습) git을 새로 시작한다:
```bash
cd ~/msa
git init -b main
git add .
git commit -m "14장 코드에서 시작"
```
`git config`를 안 했으면 먼저 [2장 02의 5-1](../chapter2/02-project-folder.md)의 `git config` 두 줄을 친다.

이제 아래 1-1부터 15장을 진행한다.

---

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
