# 02. 복합 서비스 (product-composite)

> 시작: 교수님 `Chapter03/2-basic-rest-services` 상태 / 결과: 교수님 `Chapter04`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

composite도 같은 `docker` 프로필과 `Dockerfile`. 다른 점은 `docker` 프로필에서 핵심 서비스 주소를 **서비스 이름**(`product`, `recommendation`, `review`)과 8080으로 바꾸는 것이다. 컨테이너끼리는 컴포즈의 서비스 이름이 곧 호스트 이름이다.

## 2-1. `microservices/product-composite-service/src/main/resources/application.yml`
```bash
code microservices/product-composite-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7001
spring.web.error.include-message: always

app:
  product-service:
    host: localhost
    port: 7002
  recommendation-service:
    host: localhost
    port: 7003
  review-service:
    host: localhost
    port: 7004

logging:
  level:
    root: INFO
    se.magnus: DEBUG
# ★ 추가 (13줄)

---
spring.config.activate.on-profile: docker

server.port: 8080

app:
  product-service:
    host: product
    port: 8080
  recommendation-service:
    host: recommendation
    port: 8080
  review-service:
    host: review
    port: 8080
```

## 2-2. `microservices/product-composite-service/Dockerfile` (새 파일)
```bash
code microservices/product-composite-service/Dockerfile
```
붙여넣기 (새 파일)
```dockerfile
FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /builder
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-noble
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

다음: [03-build-compose-test.md](03-build-compose-test.md)
