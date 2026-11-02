# 04. 오류 처리 추가

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄, 표시 없는 줄 = 이미 있는 줄(넣을 위치 확인용)

## 전체 흐름
```
curl /product/13
 → ProductServiceImpl이 NotFoundException을 던짐
 → util의 GlobalControllerExceptionHandler(@RestControllerAdvice)가 받음
 → 404 + HttpErrorInfo(JSON)로 응답
```
핸들러가 util에 있어도 각 서비스의 `@ComponentScan("se.magnus")` 덕분에 동작한다.

## 만드는 순서
| 순서 | 파일 | 종류 | 방법 |
|---|---|---|---|
| 1 | api `exceptions/InvalidInputException.java` | 새 파일 | 교재 소스 그대로 |
| 2 | api `exceptions/NotFoundException.java` | 새 파일 | 교재 소스 그대로 |
| 3 | util `http/HttpErrorInfo.java` | 새 파일 | 교재 소스 그대로 (**롬복 쓰지 않음**) |
| 4 | util `http/GlobalControllerExceptionHandler.java` | 새 파일 | 교재 소스 + 세 곳 변경 |
| 5 | product-service `ProductServiceImpl.java` | **기존 파일 수정** | import 2줄 + `if` 2줄 추가 |

---

## 1~3. 예외 클래스, HttpErrorInfo (새 파일, 교재 소스 그대로)
방법 1 — 직접 작성
```bash
cd ~/msa
mkdir -p api/src/main/java/se/magnus/api/exceptions
code api/src/main/java/se/magnus/api/exceptions/InvalidInputException.java
code api/src/main/java/se/magnus/api/exceptions/NotFoundException.java
code util/src/main/java/se/magnus/util/http/HttpErrorInfo.java
```
방법 2 — 복사
```bash
cd ~/msa
mkdir -p api/src/main/java/se/magnus/api/exceptions
B=$BOOK_HOME/Chapter03/2-basic-rest-services
cp $B/api/src/main/java/se/magnus/api/exceptions/InvalidInputException.java api/src/main/java/se/magnus/api/exceptions/
cp $B/api/src/main/java/se/magnus/api/exceptions/NotFoundException.java     api/src/main/java/se/magnus/api/exceptions/
cp $B/util/src/main/java/se/magnus/util/http/HttpErrorInfo.java             util/src/main/java/se/magnus/util/http/
```

### 예외 클래스 위치: 본문과 소스가 다르다
- 본문: "util 프로젝트에 ... `se.magnus.util.exceptions` 패키지"
- 소스: **api** 프로젝트의 `se.magnus.api.exceptions` (3장부터 이후 장까지 전부 이 위치)

→ **소스를 따라 api에 둔다.** 핸들러가 이미 `se.magnus.api.exceptions`를 import하고, 예외는 서비스(API 구현)와 composite(API 클라이언트) 양쪽에서 쓰기 때문이다.

### 롬복을 쓰면 안 되는 이유
롬복은 **"필드를 그대로 받고, 그대로 돌려주는"** 코드만 기계적으로 만든다. 원본의 생성자와 게터가 단순 대입/반환인지 먼저 확인한다.

| 클래스 | 롬복 | 이유 |
|---|---|---|
| `InvalidInputException`, `NotFoundException` | ✗ | 자기 필드가 없고, 생성자가 모두 `super(message)`처럼 **부모 생성자에 값을 넘긴다.** 롬복은 인자 있는 `super(...)` 호출을 만들지 못한다 |
| `HttpErrorInfo` | ✗ | 생성자가 `timestamp = ZonedDateTime.now()`로 직접 계산하고, 게터가 값을 가공한다(`getStatus()` = `httpStatus.value()`, `getError()` = `getReasonPhrase()`) |

HttpErrorInfo에 클래스 단위로 `@Getter`, `@AllArgsConstructor`를 붙였을 때 생긴 문제:
- `@AllArgsConstructor`는 4개를 받는 생성자를 만든다. 핸들러는 `new HttpErrorInfo(httpStatus, path, message)`(3개)를 호출해서 `no suitable constructor found` 컴파일 오류가 났다.
- `@Getter`가 `getHttpStatus()`까지 만들어서, JSON에 `"httpStatus":"NOT_FOUND"`가 추가로 붙는다(Jackson은 게터 하나를 JSON 필드 하나로 만든다).
- 결론: **교재 원본 그대로 둔다.**

---

## 4. `util/.../http/GlobalControllerExceptionHandler.java` (새 파일, 전체)
```bash
cd ~/msa
code util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java
# 복사하려면: cp $BOOK_HOME/Chapter03/2-basic-rest-services/util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java util/src/main/java/se/magnus/util/http/
```
교재 소스를 복사(또는 입력)한 뒤 **`UNPROCESSABLE_ENTITY`를 세 곳 모두 `UNPROCESSABLE_CONTENT`로 바꾼다**(Spring 7.0.10에서 ENTITY는 deprecated).
```java
package se.magnus.util.http;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;          // ★ 변경 ① (교재: UNPROCESSABLE_ENTITY)

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;

@RestControllerAdvice
class GlobalControllerExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public @ResponseBody HttpErrorInfo handleNotFoundExceptions(
    ServerHttpRequest request, NotFoundException ex) {

    return createHttpErrorInfo(NOT_FOUND, request, ex);
  }

  @ResponseStatus(UNPROCESSABLE_CONTENT)                                          // ★ 변경 ②
  @ExceptionHandler(InvalidInputException.class)
  public @ResponseBody HttpErrorInfo handleInvalidInputException(
    ServerHttpRequest request, InvalidInputException ex) {

    return createHttpErrorInfo(UNPROCESSABLE_CONTENT, request, ex);               // ★ 변경 ③
  }

  private HttpErrorInfo createHttpErrorInfo(
    HttpStatus httpStatus, ServerHttpRequest request, Exception ex) {

    final String path = request.getPath().pathWithinApplication().value();
    final String message = ex.getMessage();

    LOG.debug("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message);
    return new HttpErrorInfo(httpStatus, path, message);
  }
}
```
본문 조각에는 `UNPROCESSABLE_ENTITY`만 덩그러니 쓰여 있는데, 파일 맨 위의 `import static` 덕분이다. 본문에는 이 import가 빠져 있다.

---

## 5. `ProductServiceImpl.java` 수정 (기존 파일)
[03](03-product-service.md)에서 만든 파일에 **두 군데 추가**한다.
① import: `ProductService` import 아래, `ServiceUtil` import 위에 두 줄
② `getProduct()` 안: **`return` 위에** 두 줄 (`return` 뒤에 넣으면 실행되지 않음)
```java
package se.magnus.microservices.core.product.services;

import org.springframework.web.bind.annotation.RestController;

import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.exceptions.InvalidInputException;      // ★ 추가 ①
import se.magnus.api.exceptions.NotFoundException;          // ★ 추가 ①
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {
    private final ServiceUtil serviceUtil;

    public ProductServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product getProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);               // ★ 추가 ②
        if (productId == 13) throw new NotFoundException("No product found for productId: " + productId);     // ★ 추가 ②
        return new Product(productId, "name-" + productId, 123, serviceUtil.getServiceAddress());
    }
}
```
주의: 이 코드를 실수로 열려 있던 `InvalidInputException.java` 안에 붙여 넣어 `@Override`, `serviceUtil` 오류가 난 적이 있다. **에디터 탭 이름이 `ProductServiceImpl.java`인지** 확인하고 붙인다.

---

## 확인
```bash
cd ~/msa
./gradlew build
java -jar microservices/product-service/build/libs/*.jar &
curl -i localhost:7002/product/0     # 422 Unprocessable Content
curl -i localhost:7002/product/13    # 404 Not Found
kill $(jobs -p)
```
응답 본문에 `timestamp, path, status, error, message`가 나오면 성공이다. `message`가 보이면 `spring.web.error.include-message` 설정도 동작하는 것이다.

다음: [05-recommendation-review.md](05-recommendation-review.md)
