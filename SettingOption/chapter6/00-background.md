# 00. 배경: 5장과 무엇이 다른가 (영속성)

> 코드 없이 읽기만 하는 문서. 6장 전체를 한눈에 보는 요약.

## 1. 5장까지
- 핵심 서비스는 요청이 오면 **고정된 가짜 데이터**를 만들어 돌려줬다. 저장도 삭제도 없다.
- API는 조회(GET)만 있었다.

## 2. 6장이 끝나면
```
                       POST/GET/DELETE /product-composite
 client ─▶ product-composite ─┬─▶ product ────────▶ MongoDB   product-db        (products 컬렉션)
                              ├─▶ recommendation ─▶ MongoDB   recommendation-db (recommendations 컬렉션)
                              └─▶ review ─────────▶ MySQL     review-db         (reviews 테이블)
```
- 핵심 서비스마다 **자기 DB**를 갖는다. 다른 서비스의 DB는 직접 보지 않는다(마이크로서비스 원칙: 데이터 소유는 서비스별).
- product와 recommendation은 **MongoDB 서버 하나**를 같이 쓰지만 **데이터베이스는 따로**다.
- composite는 DB가 없다. 받은 묶음을 세 서비스로 나눠 보낸다.

## 3. 서비스 안의 계층
```
프로토콜 계층   @RestController (api 인터페이스)  ─ GlobalControllerExceptionHandler (util)
서비스 계층     ...ServiceImpl      ← 매퍼(MapStruct): API 객체 ↔ 엔티티
영속성 계층     ...Repository (Spring Data) + ...Entity ──▶ DB
```
| 클래스 | 하는 일 |
|---|---|
| 엔티티 (`ProductEntity`) | DB에 저장되는 모양. `id`, `version`(낙관적 잠금) 같은 DB용 필드가 있다 |
| API 모델 (`Product`) | 서비스끼리 주고받는 모양. `serviceAddress` 같은 API용 필드가 있다 |
| 매퍼 (`ProductMapper`) | 둘 사이를 변환. MapStruct가 구현 코드를 자동 생성 |
| 리포지토리 (`ProductRepository`) | `save`, `findById`, `deleteAll` 등을 인터페이스만으로 제공(Spring Data) |

## 4. 5장과 달라지는 점
| 항목 | 5장 | 6장 |
|---|---|---|
| 데이터 | 매번 새로 만든 가짜 값 | **DB에 저장된 값** |
| 핵심 서비스 API | GET만 | **POST(생성), DELETE(삭제)** 추가 |
| composite API | GET만 | POST(묶음을 나눠 생성), DELETE(세 서비스에서 삭제) |
| 없는 productId 조회 | 13번만 404(하드코딩) | DB에 없으면 404 |
| 의존성 | - | Spring Data MongoDB / JPA + MySQL 드라이버, MapStruct, Lombok, Testcontainers |
| compose | 서비스 4개 | **`mongodb`, `mysql` 컨테이너** 추가, 서비스는 DB가 healthy가 된 뒤 시작 |
| 테스트 | 서비스 테스트만 | **PersistenceTests**(리포지토리), **MapperTests** 추가. 테스트용 DB는 **Testcontainers**가 도커로 잠깐 띄움 |
| test-em-all.bash | 이미 있는 값 조회 | 시작할 때 **테스트 데이터를 지우고 다시 만든 뒤** 조회 |

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 영속성(persistence) | 앱이 꺼져도 데이터가 남도록 DB에 저장하는 것 |
| 낙관적 잠금 (`@Version`) | 두 요청이 같은 데이터를 동시에 고치면 나중 것을 실패시킴(`OptimisticLockingFailureException`) |
| 멱등(idempotent) | 같은 요청을 여러 번 해도 결과가 같음. DELETE는 없어도 200 |
| Testcontainers | 테스트할 때 진짜 DB를 도커 컨테이너로 띄웠다가 끝나면 지우는 라이브러리 |
| 컬렉션 / 테이블 | MongoDB의 저장 단위 / MySQL의 저장 단위 |

다음: [01-dependencies.md](01-dependencies.md)
