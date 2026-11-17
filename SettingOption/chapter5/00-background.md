# 00. 배경: 4장과 무엇이 다른가 (OpenAPI 문서화)

> 코드 없이 읽기만 하는 문서. 5장 전체를 한눈에 보는 요약.

## 1. 4장까지
- composite의 API(`GET /product-composite/{productId}`)가 **어떤 입력을 받고 어떤 응답을 주는지** 알려면 코드를 열어 봐야 했다.
- 호출은 터미널에서 `curl`로만 했다.

## 2. 5장이 끝나면
```
브라우저 http://localhost:8080/openapi/swagger-ui.html
   └▶ Swagger UI 화면: API 목록, 설명, 응답 코드, [Try it out] 버튼으로 바로 호출
        └ 내용은 /openapi/v3/api-docs (OpenAPI JSON 문서)에서 가져옴
             └ 이 문서는 springdoc이 코드의 어노테이션과 yml 문구를 읽어서 자동으로 만듦
```
- **코드가 곧 문서**가 된다. API를 바꾸면 문서도 같이 바뀐다.
- 문서를 붙이는 대상은 **밖에 공개되는 composite API뿐**이다(핵심 서비스는 안에서만 쓰니까).

## 3. 4장과 달라지는 점
| 항목 | 4장 | 5장 |
|---|---|---|
| 의존성 | - | api: `springdoc-openapi-starter-common`, composite: `springdoc-openapi-starter-webflux-ui` (교재 2.8.6 → 우리 **3.0.3**) |
| composite Application | - | `OpenAPI` 빈: 문서 전체 정보(제목, 버전, 연락처, 라이선스) |
| composite yml | - | `springdoc:` (UI, 문서 경로), `api:` (각 API의 설명 문구) |
| API 인터페이스 (`ProductCompositeService`) | `@GetMapping`만 | `@Tag`, `@Operation`, `@ApiResponses` 추가. 문구는 `${api....}`로 yml에서 가져옴 |
| 오류 응답 | 404, 422 | **400(Bad Request)** 추가: `BadRequestException` + 핸들러 |
| test-em-all.bash | - | Swagger UI, api-docs 주소가 200인지 확인하는 줄 추가 |

- 문구를 코드가 아니라 yml에 두는 이유: 설명을 고칠 때 Java를 다시 컴파일할 필요가 없고, 코드가 짧아진다.

## 4. 각 문서에서 생기는 것
| 문서 | 내용 |
|---|---|
| 01 | api, composite build.gradle에 springdoc 의존성 |
| 02 | `OpenAPI` 빈, yml(springdoc 경로, api 설명 문구) |
| 03 | 인터페이스 어노테이션, `BadRequestException`, 400 핸들러 |
| 04 | 빌드, 구동, test-em-all, Swagger UI에서 Try it out |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| OpenAPI | REST API를 설명하는 표준 형식(JSON/YAML). 예전 이름은 Swagger 명세 |
| springdoc-openapi | 스프링 코드를 읽어 OpenAPI 문서를 자동으로 만드는 라이브러리 |
| Swagger UI | OpenAPI 문서를 보여 주고 직접 호출해 보는 웹 화면 |
| `@Operation`, `@ApiResponses` | 메서드 하나(API 하나)의 설명과 가능한 응답 코드 |

10장부터는 Swagger UI도 게이트웨이를 거쳐 같은 주소(`localhost:8080/openapi/...`)로 연다.
