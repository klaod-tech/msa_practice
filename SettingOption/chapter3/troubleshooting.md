# 3장 오류와 해결 모음

## 빌드·컴파일
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| `Project with path ':api' could not be found` | settings.gradle에 api/util 미등록, 폴더 없음 | `include ':api'`, `include ':util'` + 루트에 폴더 생성 | 02 |
| `Cannot find a Java installation ... 24` (예방) | 교재 build.gradle의 Java 24 | 21로 변경 | 02 |
| 다른 패키지에서 import 불가 | `package` 선언 누락(기본 패키지가 됨) | 파일 맨 위에 폴더 경로와 같은 `package` 선언 | 02 |
| `variable ... not initialized in the default constructor` | final 필드를 초기화하는 생성자가 없음 | 롬복 `@AllArgsConstructor` + `@NoArgsConstructor(force = true)` | 02 |
| `cannot find symbol: variable serviceUtil` | 클래스(`ServiceUtil`)가 아니라 **필드**(`serviceUtil`) 선언이 없음 | `private final ServiceUtil serviceUtil;` | 03 |
| `method does not override or implement a method from a supertype` | 인터페이스가 비어 있음, 또는 메서드를 엉뚱한 클래스에 붙임 | 인터페이스에 메서드 선언 / 올바른 파일에 붙이기 | 03, 04 |
| `no suitable constructor found for HttpErrorInfo(HttpStatus,String,String)` | HttpErrorInfo를 롬복 `@AllArgsConstructor`로 바꿈 | 교재 원본으로 되돌림 | 04 |
| `package com.fasterxml.jackson.databind does not exist` | Boot 4는 Jackson 3 | `tools.jackson.databind.ObjectMapper` | 06 |
| `exception IOException is never thrown` (예방) | Jackson 3의 readValue는 IOException을 안 던짐 | `catch (JacksonException e)` | 06 |
| composite ServiceImpl에서 `getRecommendationId()` 등 `cannot find symbol` | api의 Recommendation/Review가 비어 있음 | 데이터 클래스 먼저 작성 | 05 |
| 테스트 `contextLoads()` 실패 (`No qualifying bean ... ServiceUtil`) | 컴포넌트 스캔 범위 밖 | `@ComponentScan("se.magnus")` | 03 |
| 테스트 4개 모두 실패 (`No qualifying bean ... WebTestClient`) | Boot 4에서 WebTestClient 설정 분리 | `@AutoConfigureWebTestClient` | 07 |

## 실행
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| `no main manifest attribute` | `*.jar`가 plain jar를 먼저 잡음 | `jar { enabled = false }` | 03 |
| `Error starting ApplicationContext` (`Port 7001 was already in use`) | 옛 프로세스가 포트를 잡고 있음 | `kill <PID>` → `ss -ltnp \| grep 7001`로 확인 후 실행 | 07 |
| composite 호출 시 500 `Connection refused` | 호출 대상 서비스가 안 떠 있음 | 4개 모두 실행 | 07 |
| composite 응답 값이 전부 null/0 | Jackson 3은 final 필드에 값을 넣지 않음 + `new RestTemplate()`은 Boot 설정을 안 씀 | RestTemplate에 JsonMapper 연결 + `allow-final-fields-as-mutators: true` | 06 |
| `./test-em-all.bash: Permission denied` | 실행 권한 없음 | `chmod +x test-em-all.bash` | 07 |
| 같은 포트로 두 서비스가 충돌 | yml 복사 후 이름·포트를 안 바꿈 | 서비스별 포트 확인 | 05 |
| 루트에 만든 application.yml이 모든 서비스에 적용될 위험 | Spring Boot는 실행 위치의 `./application.yml`도 읽음 | 각 서비스 `src/main/resources/`에만 둔다 | 03 |

## 설정 (오류 없이 조용히 틀리는 것)
| 증상 | 원인 | 해결 | 문서 |
|---|---|---|---|
| yml의 `server.error.include-message`에 빨간 밑줄 | Boot 4에서 이름이 바뀌어 옛 이름은 무시됨 | `spring.web.error.include-message` | 03 |
| `UNPROCESSABLE_ENTITY`에 취소선 | Spring 7에서 deprecated | `UNPROCESSABLE_CONTENT` | 04 |
| HttpErrorInfo JSON에 `httpStatus` 필드가 추가로 나옴 | 클래스 단위 `@Getter`가 `getHttpStatus()`를 만듦 | 원본으로 되돌림 | 04 |

## VS Code
| 증상 | 원인 | 해결 |
|---|---|---|
| `code 경로`로 연 파일이 탐색기에 안 보임, 컴파일에도 안 잡힘 | 중간 폴더가 없어 저장 실패, 또는 아직 저장 안 함 | `mkdir -p` 먼저, `Ctrl+S` |
| 파일이 엉뚱한 폴더에 생김 | 탐색기 📄+ 아이콘은 마지막으로 선택한 위치 기준 | 폴더 우클릭 → 새 파일, 또는 터미널 `code 경로` |
| 오류 표시·자동 import 안 됨 | Java 확장이 프로젝트를 불러오지 못함 | build.gradle 열고 `Shift+Alt+U` |
| build.gradle 변경이 반영 안 됨 | 동기화 알림을 놓침 | 원격(WSL) 설정 `java.configuration.updateBuildConfiguration` → `automatic` |
| 자동 완성이 엉뚱함 | WSL에 Java 확장이 없어 단어 기반 추천만 동작 | WSL에 Extension Pack for Java 설치 |
| 오류 설명이 마우스를 올려도 안 뜸 | 사용자 설정 `editor.hover.enabled: off` | 문제 탭(`Ctrl+Shift+M`)에서 확인 |
| VS Code에 오류가 안 보일 때 확인 | | `./gradlew :모듈:compileJava`로 직접 컴파일 |
