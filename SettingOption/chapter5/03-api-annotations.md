# 03. 인터페이스에 API 문서 애너테이션 + 400 오류 처리

교재 "ProductCompositeService 인터페이스에 API 관련 문서 추가". 파일 3개를 바꾼다.

## 1. ProductCompositeService.java (전체, 교재 그대로)
`code api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java` 들어가서 전체를 아래로 교체
```java
package se.magnus.api.composite.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "ProductComposite", description = "REST API for composite product information.")
public interface ProductCompositeService {

  /**
   * Sample usage: "curl $HOST:$PORT/product-composite/1".
   *
   * @param productId Id of the product
   * @return the composite product info, if found, else null
   */
  @Operation(
    summary = "${api.product-composite.get-composite-product.description}",
    description = "${api.product-composite.get-composite-product.notes}")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "${api.responseCodes.ok.description}"),
    @ApiResponse(responseCode = "400", description = "${api.responseCodes.badRequest.description}"),
    @ApiResponse(responseCode = "404", description = "${api.responseCodes.notFound.description}"),
    @ApiResponse(responseCode = "422", description = "${api.responseCodes.unprocessableEntity.description}")
  })
  @GetMapping(
    value = "/product-composite/{productId}",
    produces = "application/json")
  ProductAggregate getProduct(@PathVariable int productId);
}
```
| 애너테이션 | 위치 | 하는 일 |
|---|---|---|
| `@Tag` | 인터페이스 | API 묶음(리소스) 이름과 설명. Swagger UI에서 그룹 제목이 된다 |
| `@Operation` | 메서드 | 연산 하나의 요약(`summary`)과 상세 설명(`description`) |
| `@ApiResponses` / `@ApiResponse` | 메서드 | 응답 코드별 설명 (200, 400, 404, 422) |

- **`${...}` 플레이스홀더**: springdoc이 실행할 때 composite의 application.yml([02](02-openapi-config.md)의 `api:` 블록)에서 실제 문구를 찾아 넣는다. `@Value` 없이 바로 쓴다.
- springdoc은 애너테이션 말고도 `@GetMapping`, `@PathVariable`을 보고 **경로·HTTP 메서드·입력 인수**를, 반환 타입(`ProductAggregate`)을 보고 **성공 응답 JSON 구조**를 알아낸다.

## 2. BadRequestException.java (새 파일, 교재 그대로)
`code api/src/main/java/se/magnus/api/exceptions/BadRequestException.java` 들어가서 아래 내용 전체 입력
```java
package se.magnus.api.exceptions;

public class BadRequestException extends RuntimeException {
  public BadRequestException() {}

  public BadRequestException(String message) {
    super(message);
  }

  public BadRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  public BadRequestException(Throwable cause) {
    super(cause);
  }
}
```
다른 예외 클래스(InvalidInputException, NotFoundException)와 같은 모양이다. 부모 생성자를 호출하므로 롬복을 쓰지 않는다(3장 04 참고).

## 3. GlobalControllerExceptionHandler.java (전체)
`code util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java` 들어가서 전체를 아래로 교체
```java
package se.magnus.util.http;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.magnus.api.exceptions.BadRequestException;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;

@RestControllerAdvice
class GlobalControllerExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(BadRequestException.class)
  public @ResponseBody HttpErrorInfo handleBadRequestExceptions(
    ServerHttpRequest request, BadRequestException ex) {

    return createHttpErrorInfo(BAD_REQUEST, request, ex);
  }

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public @ResponseBody HttpErrorInfo handleNotFoundExceptions(
    ServerHttpRequest request, NotFoundException ex) {

    return createHttpErrorInfo(NOT_FOUND, request, ex);
  }

  @ResponseStatus(UNPROCESSABLE_CONTENT)
  @ExceptionHandler(InvalidInputException.class)
  public @ResponseBody HttpErrorInfo handleInvalidInputException(
    ServerHttpRequest request, InvalidInputException ex) {

    return createHttpErrorInfo(UNPROCESSABLE_CONTENT, request, ex);
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
- 이번에 추가: `BAD_REQUEST` static import, `BadRequestException` import, 400 핸들러 메서드(`handleBadRequestExceptions`)
- **⚠️ 교재 5장 소스를 그대로 복사하면 `UNPROCESSABLE_ENTITY`로 돌아간다.** 위 파일은 3장에서 바꾼 `UNPROCESSABLE_CONTENT`(Spring 7)를 유지했다.
- 400 핸들러를 두는 이유: springdoc은 `@RestControllerAdvice`의 `@ExceptionHandler`를 보고 **오류 응답 구조**를 문서에 넣는다. 스프링 웹플럭스가 잘못된 입력에 내는 400도 문서화되도록 핸들러를 추가한다.

## 확인
```bash
./gradlew build
```
api, util, composite가 모두 바뀌었으므로 전체 빌드로 확인한다.
