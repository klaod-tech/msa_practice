# 00. 배경: 왜 서비스 검색이 필요한가, 7장까지와 무엇이 다른가

> 코드 없이 읽기만 하는 문서. 교재 9장 "DNS 기반 서비스 디스커버리의 문제", "넷플릭스 유레카를 사용한 서비스 디스커버리" 절 요약.

## 1. 7장까지는 어떻게 서로를 찾았나
composite의 application.yml에 **주소를 직접** 적었다.
```yaml
app:
  product-service:
    host: localhost   # docker 프로필에서는 product
    port: 7002        # docker 프로필에서는 8080
```
도커 컴포즈에서는 `product`라는 **서비스 이름이 곧 DNS 이름**이라 `http://product:8080`으로 불렀다. 인스턴스가 하나일 때는 이걸로 충분했다.

## 2. 인스턴스가 여러 개면? (교재: DNS 기반 서비스 디스커버리의 문제)
생각할 수 있는 방법은 **라운드 로빈 DNS**다. 인스턴스마다 같은 이름으로 DNS에 IP를 등록하고, 클라이언트가 받은 IP 목록을 돌아가며 쓰면 될 것 같다.

실제로는 안 된다.

| 기대 | 실제 |
|---|---|
| 클라이언트가 IP 목록을 돌아가며 사용 | DNS 클라이언트는 보통 **첫 번째로 되는 IP 하나를 계속** 쓴다. 요청마다 라운드 로빈을 하지 않는다 |
| 인스턴스가 생기고 죽으면 바로 반영 | DNS는 결과를 **캐시**한다. 마이크로서비스 인스턴스처럼 **수시로 생겼다 없어지는** 대상에 맞게 설계되지 않았다 |

그래서 "이론적으로는 매력적이지만 서비스 디스커버리에는 실용적이지 않다"(교재).

## 3. 해결: 서비스 레지스트리 (넷플릭스 유레카)
```
           ① 등록 + 5초마다 하트비트                 ② 목록 가져오기(5초마다)
 product ─────────────────────────▶  Eureka 서버  ◀───────────────────────── product-composite
 recommendation ──────────────────▶   (레지스트리)                               │
 review ──────────────────────────▶   product: [ip1:8080, ip2:8080]             │ ③ http://product/...
                                                                               ▼  → 목록 중 하나 골라 ip로 호출
                                                                         (Spring Cloud LoadBalancer)
```

| 단계 | 누가 | 무엇을 | 9장 설정 |
|---|---|---|---|
| ① 등록 | 각 서비스 | 시작하면 **자기 이름(`spring.application.name`)과 주소**를 Eureka에 등록, 살아 있다고 주기적으로 알림(하트비트) | `leaseRenewalIntervalInSeconds: 5` |
| 만료 | Eureka 서버 | 하트비트가 끊긴 인스턴스는 목록에서 뺀다 | `leaseExpirationDurationInSeconds: 5` |
| ② 조회 | composite | 레지스트리를 **복사해서 들고 있다가** 주기적으로 갱신 | `registryFetchIntervalSeconds: 5` |
| ③ 호출 | composite | `http://product` → 들고 있는 목록에서 **하나를 골라** 실제 `ip:port`로 바꿔 호출. 매번 돌아가며 고른다 | `@LoadBalanced WebClient.Builder` |

- 위 5초 설정은 **개발/실습용**이다(변화가 빨리 보이게). 기본값은 30초 안팎이다.
- composite가 목록을 **복사해 들고 있으므로**, Eureka 서버가 잠깐 죽어도 이미 아는 인스턴스는 계속 부를 수 있다(04에서 실험).

## 4. 7장까지와 달라지는 점 한눈에

| 항목 | 7장까지 | 9장 |
|---|---|---|
| 새 컨테이너 | - | **`eureka`** (포트 8761, 웹 화면 있음) |
| 서비스 이름 | `spring.application.name: product-service` 등 | `product`, `recommendation`, `review`, `product-composite` (**Eureka 서비스 ID**) |
| composite가 부르는 주소 | `http://` + yml의 `host:port` | **`http://product`** (이름만, 포트 없음) |
| composite yml | `app.product-service.host/port` 등 6개 값 | **삭제**, 대신 `eureka.client.serviceUrl.defaultZone` |
| `WebClient.Builder` | 스프링 부트 기본 빈 | **`@LoadBalanced`** 빈을 직접 만듦 |
| 인스턴스 여러 개 | 이름 하나에 하나만 의미 있음 | `docker compose up -d --scale review=3` → composite가 3개를 **돌아가며** 호출 |
| 테스트 | 그대로 | `eureka.client.enabled=false` (테스트 중에는 Eureka 서버가 없으니까) |
| test-em-all | - | Eureka에 **서비스 4개가 등록됐는지** 확인하는 2줄 추가 |

이벤트(RabbitMQ/Kafka)는 그대로다. 메시지는 브로커가 중계하므로 서비스 검색이 필요 없다. **Eureka는 composite의 GET(HTTP) 호출과 헬스 체크에만** 쓰인다.

## 5. 용어
| 용어 | 뜻 |
|---|---|
| 서비스 디스커버리(검색) | 서비스 이름으로 지금 살아 있는 인스턴스 주소를 찾는 것 |
| 서비스 레지스트리 | 이름 → 인스턴스 목록을 들고 있는 곳 (= Eureka 서버) |
| 인스턴스 | 같은 서비스의 실행 중인 복사본 하나 (컨테이너 하나) |
| 클라이언트 측 로드 밸런싱 | 부르는 쪽(composite)이 목록에서 직접 하나를 고르는 방식 (Spring Cloud LoadBalancer) |

## 6. 문서별로 생기는 것
| 문서 | 내용 |
|---|---|
| 01 | 새 프로젝트 `spring-cloud/eureka-server` |
| 02 | 핵심 서비스 3개: Eureka 클라이언트 의존성, yml(서비스 이름, Eureka 주소), 테스트에서 Eureka 끄기 |
| 03 | composite: `@LoadBalanced` WebClient.Builder, `http://product` 같은 이름으로 호출, yml |
| 04 | settings.gradle, compose에 eureka, test-em-all(등록 확인), 실행, `--scale review=3` |
