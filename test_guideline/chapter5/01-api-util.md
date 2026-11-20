# 01. api: 문서 어노테이션, 400 예외 / util: 400 핸들러

> 시작: 교수님 `Chapter04` 상태 / 결과: 교수님 `Chapter05`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `api/build.gradle`: `springdoc-openapi-starter-common`(어노테이션만 쓰는 라이브러리라 UI 없는 common).
- `ProductCompositeService`: `@Tag`(API 묶음 이름), `@Operation`(API 하나의 요약과 설명), `@ApiResponses`(가능한 응답 코드). 문구는 코드에 직접 쓰지 않고 `${api.product-composite...}`로 yml에서 가져온다. 설명을 고칠 때 자바를 다시 컴파일하지 않아도 된다.
- `BadRequestException`(새 파일)과 `GlobalControllerExceptionHandler`: 잘못된 요청에 **400**을 돌려준다.

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
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 1-2. `api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java`
```bash
code api/src/main/java/se/magnus/api/composite/product/ProductCompositeService.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.api.composite.product;

// ★ 추가 (4줄)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "ProductComposite", description = "REST API for composite product information.")  // ★ 추가
public interface ProductCompositeService {

  // ★ 추가 (9줄)
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

## 1-3. `api/src/main/java/se/magnus/api/exceptions/BadRequestException.java` (새 파일)
```bash
code api/src/main/java/se/magnus/api/exceptions/BadRequestException.java
```
붙여넣기 (새 파일)
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

## 1-4. `util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java`
```bash
code util/src/main/java/se/magnus/util/http/GlobalControllerExceptionHandler.java
```
아래처럼 맞추기 (전체 파일)
```java
package se.magnus.util.http;

import static org.springframework.http.HttpStatus.BAD_REQUEST;  // ★ 추가
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
import se.magnus.api.exceptions.BadRequestException;  // ★ 추가
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;

@RestControllerAdvice
class GlobalControllerExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);
  // ★ 추가 (6줄)

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

다음: [02-composite.md](02-composite.md)
