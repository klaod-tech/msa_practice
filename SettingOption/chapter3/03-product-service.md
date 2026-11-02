# 03. product-service 구현 + 단독 실행 (교재 68~70쪽)

## 1. `build.gradle`: api, util 의존성 + plain jar 끄기
```groovy
plugins { ... }

jar {
    enabled = false      // plain jar를 만들지 않음
}

dependencies {
    ...
    implementation project(':api')
    implementation project(':util')
}
```
- `jar { enabled = false }`는 본문에 없고 소스에만 있다. 없으면 `build/libs`에 `*-plain.jar`도 함께 생긴다. 그러면 `java -jar .../*.jar`가 plain jar를 먼저 잡아서 `no main manifest attribute` 오류가 난다.
- `jar { }`는 `dependencies { }` **밖에** 둔다.

## 2. 메인 클래스에 `@ComponentScan`
```java
@SpringBootApplication
@ComponentScan("se.magnus")
public class ProductServiceApplication { ... }
```
스프링은 기본적으로 메인 클래스의 패키지(`se.magnus.microservices.core.product`) 아래만 스캔한다. util의 `se.magnus.util.http.ServiceUtil`은 범위 밖이라, 이 설정이 없으면 테스트에서 `No qualifying bean of type 'se.magnus.util.http.ServiceUtil'` 오류가 난다.

## 3. `services/ProductServiceImpl.java`
위치: `microservices/product-service/src/main/java/se/magnus/microservices/core/product/services/`
```java
@RestController
public class ProductServiceImpl implements ProductService {
    private final ServiceUtil serviceUtil;     // 필드 선언 필수

    public ProductServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product getProduct(int productId) {
        // 예외 if문 두 줄은 04(오류 처리)에서 추가
        return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
    }
}
```
- 인터페이스(`ProductService`)는 api에, 구현(`ProductServiceImpl`)은 서비스 안에 둔다.
- 생성자 매개변수는 생성자 안에서만 쓸 수 있으므로 `private final ServiceUtil serviceUtil;` 필드가 꼭 있어야 한다.

## 4. `application.properties` 삭제 → `application.yml`
```yaml
spring.application.name: product-service

server.port: 7002
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG
```
- **`spring.web.error.include-message`는 교재와 다르다.** 교재(Boot 3)의 `server.error.include-message`는 Boot 4.0부터 이름이 바뀌었고, 옛 이름은 **오류 없이 조용히 무시된다**(Boot jar 메타데이터: `deprecated since 4.0.0, level: error`). VS Code에서 빨간 밑줄이 사라지면 올바른 이름이다.
- YAML은 `키: 값` 형식이다. properties 문법인 `=`를 쓰면 안 된다.
- 들여쓰기는 **스페이스만** 쓴다(탭 금지). 같은 단계끼리 칸 수를 맞춘다(2칸 권장).
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
