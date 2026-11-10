# 6장: 영속성 추가

> 참고 교재: 스프링 부트 3과 스프링 클라우드를 활용한 마이크로서비스 구축 (마그누스 라르손), 6장
> 실습 환경: **Java 21 / Spring Boot 4.0.9** (Spring Data MongoDB 5, Hibernate 7, Testcontainers 2.0.5), MapStruct 1.6.3
> 이전: [../chapter5/README.md](../chapter5/README.md) / 다음: [../chapter7/README.md](../chapter7/README.md) / 상태: **완료**

## 문서 읽는 법
- 모든 명령은 `~/msa`에서 실행한다.
- 파일 단계는 `code 경로` 코드 블록으로 시작한다(오른쪽 복사 버튼 → 터미널에 붙여넣기). 새 파일은 `mkdir -p`와 같은 블록이라 한 번에 실행된다.
- `★ 추가` / `★ 변경` = 5장 코드 대비 바뀌는 줄, `(교재: ...)` = Boot 4 때문에 교재 6장과 다르게 쓰는 부분.

## 무엇을 하나
5장까지 핵심 서비스는 고정 값을 돌려줬다. 이제 **각 서비스가 자기 DB를 갖고** 데이터를 저장, 조회, 삭제한다.

| 서비스 | DB 서버(컨테이너) | 데이터베이스 | 저장 단위 | 스프링 모듈 |
|---|---|---|---|---|
| product | `mongodb` | `product-db` | `products` 컬렉션 | Spring Data MongoDB |
| recommendation | `mongodb` | `recommendation-db` | `recommendations` 컬렉션 | Spring Data MongoDB |
| review | `mysql` | `review-db` | `reviews` 테이블 | Spring Data JPA (Hibernate) |

- DB는 **3개**이고 서버(컨테이너)는 2개다. product와 recommendation은 MongoDB 서버를 같이 쓰지만 데이터베이스가 달라서 서로의 데이터를 보지 않는다.
- composite는 DB가 없다. 받은 묶음을 세 서비스로 나눠 보내는 **생성(POST)/삭제(DELETE)** 가 생긴다.

각 핵심 서비스 안의 계층:
```
프로토콜 계층   @RestController (api 인터페이스) + GlobalControllerExceptionHandler (util)
서비스 계층     ...ServiceImpl  ── 매퍼(MapStruct)로 API 객체 ↔ 엔티티 변환
영속성 계층     ...Repository (Spring Data) + ...Entity  ──→ DB
```
composite는 영속성 계층 대신 **통합 계층**(`ProductCompositeIntegration`)이 세 서비스를 HTTP로 부른다.

## 진행 순서

| 순서 | 문서 | 내용 | 확인 |
|---|---|---|---|
| 1 | [01-dependencies.md](01-dependencies.md) | 핵심 서비스 build.gradle 3개 (MongoDB/JPA, MapStruct, Testcontainers) | 컴파일 |
| 2 | [02-persistence-layer.md](02-persistence-layer.md) | 엔티티, 리포지토리 | 컴파일 |
| 3 | [03-persistence-tests.md](03-persistence-tests.md) | Testcontainers 영속성 테스트 | `--tests PersistenceTests` (도커 필요) |
| 4 | [04-api-models-and-mappers.md](04-api-models-and-mappers.md) | API 모델 setter, MapStruct 매퍼 | `--tests MapperTests` |
| 5 | [05-core-services.md](05-core-services.md) | API 생성/삭제, 서비스 구현, Application, yml, 서비스 테스트 | 서비스별 `build` |
| 6 | [06-composite-service.md](06-composite-service.md) | composite 생성/삭제, Integration, yml, 테스트 | `./gradlew build` |
| 7 | [07-docker-compose-and-db.md](07-docker-compose-and-db.md) | compose에 MongoDB/MySQL, test-em-all.bash, mongosh/mysql로 데이터 보기 | `End, all tests OK` |
| - | [troubleshooting.md](troubleshooting.md) | 막혔던 것 모음 | |

## 교재와 다르게 하는 것

| 항목 | 교재 (Boot 3.5) | 이 실습 (Boot 4.0.9) | 문서 |
|---|---|---|---|
| Testcontainers 의존성 | `org.testcontainers:junit-jupiter`, `mongodb`, `mysql` | `testcontainers-junit-jupiter`, `testcontainers-mongodb`, `testcontainers-mysql` (2.0.5) | 01 |
| 테스트 스타터 | 없음 | `spring-boot-starter-data-mongodb-test`, `spring-boot-starter-data-jpa-test` | 01 |
| `@DataMongoTest` / `@DataJpaTest` import | `org.springframework.boot.test.autoconfigure...` | `org.springframework.boot.data.mongodb.test.autoconfigure` / `...data.jpa.test.autoconfigure` | 03 |
| `MongoDBContainer` / `MySQLContainer` import | `org.testcontainers.containers` | `org.testcontainers.mongodb` / `org.testcontainers.mysql` | 03 |
| 엔티티 getter/setter | 직접 작성 | Lombok `@Getter @Setter @NoArgsConstructor` (+ build.gradle에 lombok, `lombok-mapstruct-binding`) | 01, 02 |
| API 모델 setter | 직접 작성 | Lombok `@Setter` | 04 |
| MongoDB 연결 설정 | `spring.data.mongodb.host/port/database` | **`spring.mongodb.*`** (옛 이름은 무시됨) | 05 |
| 서비스 테스트 | `UNPROCESSABLE_ENTITY`, WebTestClient 자동 | `UNPROCESSABLE_CONTENT`, `@AutoConfigureWebTestClient` | 05, 06 |
| yml 오류 메시지 설정 | `server.error.include-message` | `spring.web.error.include-message` (3장부터) | 05 |
| ProductCompositeIntegration | Jackson 2 (`com.fasterxml`), `UNPROCESSABLE_ENTITY` | Jackson 3 (`tools.jackson`), `UNPROCESSABLE_CONTENT` (3장부터) | 06 |

## 검증한 것 / 안 한 것
- 교재 6장 코드에 위 변경(Lombok 엔티티 포함)을 적용한 복사본에서 **모든 프로젝트의 main/test 컴파일**과 **MapperTests 3개 실행**을 통과했다.
- Testcontainers를 쓰는 테스트(Persistence, ApplicationTests)와 docker compose 구동은 실습 과정이라 직접 돌리지 않았다. 실패하면 troubleshooting.md에 추가한다.
