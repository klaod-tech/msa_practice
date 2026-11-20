# 01. 핵심 서비스 (product, recommendation, review)

> 시작: 교수님 `Chapter03/2-basic-rest-services` 상태 / 결과: 교수님 `Chapter04`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

서비스마다 두 가지를 한다.
- `application.yml` 맨 아래에 `---`로 구분한 **`docker` 프로필**: 컨테이너 안에서는 포트를 8080으로 쓴다. 컴포즈가 `SPRING_PROFILES_ACTIVE=docker`로 켠다. 위쪽(기본 프로필)은 3장처럼 로컬 실행용으로 그대로 남는다.
- `Dockerfile` (새 파일): jar를 레이어(의존성, 스프링 부트 로더, 내 코드)로 풀어서 이미지를 만든다. 코드만 바뀌면 마지막 레이어만 다시 만들어져서 빠르다.

## 1-1. `microservices/product-service/src/main/resources/application.yml`
```bash
code microservices/product-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7002
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG

---  # ★ 추가
spring.config.activate.on-profile: docker  # ★ 추가

server.port: 8080  # ★ 추가
```

## 1-2. `microservices/product-service/Dockerfile` (새 파일)
```bash
code microservices/product-service/Dockerfile
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

## 1-3. `microservices/recommendation-service/src/main/resources/application.yml`
```bash
code microservices/recommendation-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7003
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG

---  # ★ 추가
spring.config.activate.on-profile: docker  # ★ 추가

server.port: 8080  # ★ 추가
```

## 1-4. `microservices/recommendation-service/Dockerfile` (새 파일)
```bash
code microservices/recommendation-service/Dockerfile
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

## 1-5. `microservices/review-service/src/main/resources/application.yml`
```bash
code microservices/review-service/src/main/resources/application.yml
```
아래처럼 맞추기 (전체 파일)
```yaml
server.port: 7004
spring.web.error.include-message: always

logging:
  level:
    root: INFO
    se.magnus: DEBUG

---  # ★ 추가
spring.config.activate.on-profile: docker  # ★ 추가

server.port: 8080  # ★ 추가
```

## 1-6. `microservices/review-service/Dockerfile` (새 파일)
```bash
code microservices/review-service/Dockerfile
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

다음: [02-composite.md](02-composite.md)
