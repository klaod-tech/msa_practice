# 03. product-service 구현 + 단독 실행 (교재 68~70쪽)

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄, 표시 없는 줄 = 이미 있는 줄(넣을 위치 확인용)

## 1. `microservices/product-service/build.gradle` 수정
```bash
code microservices/product-service/build.gradle
```
**두 군데 추가**
① `plugins { }` 블록과 `group` 사이에 `jar { }` 블록
② `dependencies { }` 블록의 **마지막 줄(`testRuntimeOnly ...`) 아래, 닫는 `}` 위**에 두 줄
```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '4.0.9'
	id 'io.spring.dependency-management' version '1.1.7'
}

jar {                          // ★ 추가 ①
    enabled = false            // ★ 추가 ①
}                              // ★ 추가 ①

group = 'se.magnus.microservices.core.product'
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
	implementation project(':api')     // ★ 추가 ②
	implementation project(':util')    // ★ 추가 ②
}

tasks.named('test') {
	useJUnitPlatform()
}
```
- `jar { enabled = false }`는 본문에 없고 소스에만 있다. 없으면 `build/libs`에 `*-plain.jar`도 함께 생긴다. 그러면 `java -jar .../*.jar`가 plain jar를 먼저 잡아서 `no main manifest attribute` 오류가 난다.
- `jar { }`를 `dependencies { }` **안에** 넣지 않는다. 의존성이 아니라 jar 작업 설정이다.

## 2. 메인 클래스 `ProductServiceApplication.java` 수정
```bash
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/ProductServiceApplication.java
```
**import 한 줄, 애너테이션 한 줄 추가**
```java
package se.magnus.microservices.core.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;      // ★ 추가

@SpringBootApplication
@ComponentScan("se.magnus")                                         // ★ 추가
public class ProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

}
```
스프링은 기본적으로 메인 클래스의 패키지(`se.magnus.microservices.core.product`) 아래만 스캔한다. util의 `se.magnus.util.http.ServiceUtil`은 범위 밖이라, 이 설정이 없으면 테스트에서 `No qualifying bean of type 'se.magnus.util.http.ServiceUtil'` 오류가 난다.

## 3. `services/ProductServiceImpl.java` (새 파일, 전체)
위치: `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/`
```bash
mkdir -p microservices/product-service/src/main/java/se/magnus/microservices/core/product/services
code microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/ProductServiceImpl.java
```
아래 내용 전체 입력

이 단계(오류 처리 전)의 전체 내용. 예외 `if`문은 [04](04-error-handling.md)에서 추가한다.
```java
package se.magnus.microservices.core.product.services;

import org.springframework.web.bind.annotation.RestController;

import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {
    private final ServiceUtil serviceUtil;      // 필드 선언 필수

    public ProductServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product getProduct(int productId) {
        return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
    }
}
```
- 인터페이스(`ProductService`)는 api에, 구현(`ProductServiceImpl`)은 서비스 안에 둔다.
- 생성자 매개변수는 생성자 안에서만 쓸 수 있으므로 `private final ServiceUtil serviceUtil;` 필드가 꼭 있어야 한다. 없으면 `cannot find symbol: variable serviceUtil`.
- 교재 소스의 `ProductServiceImpl`은 오류 처리까지 끝난 최종 버전이라 `LOG`와 예외 `if`문이 있다. 지금 붙이면 예외 클래스가 없어서 오류가 난다.

## 4. `application.properties` 삭제 → `application.yml` (새 파일, 전체)
위치: `microservices/product-service/src/main/resources/`
```bash
rm microservices/product-service/src/main/resources/application.properties
```
```bash
code microservices/product-service/src/main/resources/application.yml
```
아래 내용 전체 입력
```yaml
spring.application.name: product-service

server.port: 7002
spring.web.error.include-message: always      # ★ 교재와 다름 (교재: server.error.include-message)

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
- **`spring.web.error.include-message`**: 교재(Boot 3)의 `server.error.include-message`는 Boot 4.0부터 이름이 바뀌었고, 옛 이름은 **오류 없이 조용히 무시된다**(Boot jar 메타데이터: `deprecated since 4.0.0, level: error`). VS Code에서 빨간 밑줄이 사라지면 올바른 이름이다.
- 삭제한 properties의 `spring.application.name=product-service`는 yml 첫 줄로 옮겼다. YAML은 `=`가 아니라 `키: 값`이다.
- 들여쓰기는 **스페이스만**(탭 금지), 같은 단계끼리 칸 수를 맞춘다(2칸).
- yml은 **각 서비스의 `src/main/resources/`**에 둔다. 레포 루트에 두면, 실행 위치의 `./application.yml`이 `~/msa`에서 띄우는 모든 서비스에 적용된다.
- `se.magnus` 밑줄은 로거 이름이라 무시해도 된다.

## 5. 빌드·실행·테스트 (70쪽 7~9단계)
교재의 `cd $BOOK_HOME/...` 대신 **내 레포에서** 실행한다.
```bash
cd ~/msa
./gradlew clean build                                         # BUILD SUCCESSFUL
java -jar microservices/product-service/build/libs/*.jar &    # "Started ProductServiceApplication" 확인
curl http://localhost:7002/product/123
kill $(jobs -p)                                               # 서비스를 띄운 그 터미널에서
```
실제 응답:
```json
{"name":"name-123","productId":123,"serviceAddress":"301B-03/127.0.1.1:7002","weight":123}
```
교재와 다른 점(모두 정상):
- 필드 순서가 알파벳순이다(Jackson 3 기본).
- IP가 `127.0.1.1`이다(Ubuntu의 `/etc/hosts`가 호스트명을 이 주소로 연결).

다음: [04-error-handling.md](04-error-handling.md)
