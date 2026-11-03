# 03. 마이크로서비스 도커 이미지 만들기 (교재 99쪽~)

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄 (괄호 안은 교재 원래 값), 표시 없는 줄 = 이미 있는 줄(위치 확인용)
> 모든 명령은 `~/msa`에서 실행한다.

product-service부터 시작한다. 교재 소스: `$BOOK_HOME/Chapter04/microservices/product-service/`

---

## 1. application.yml에 docker 프로파일 추가

`code microservices/product-service/src/main/resources/application.yml` 들어가서 **맨 끝에** 4줄 추가

```yaml
logging:
  level:
    root: INFO
    se.magnus: DEBUG

---                                              # ★ 추가
spring.config.activate.on-profile: docker        # ★ 추가
                                                 # ★ 추가
server.port: 8080                                # ★ 추가
```
- `---`은 한 yml 파일 안에서 설정 묶음을 나누는 구분선이다.
- 그 아래는 **`docker` 프로파일로 실행할 때만** 적용된다. 위쪽 값 중 겹치는 것(`server.port`)을 덮어쓴다.
  - 평소 `java -jar`로 실행 → 7002
  - 컨테이너에서 docker 프로파일로 실행 → 8080
- 컨테이너는 각자 따로 떨어진 환경이라 서비스마다 포트를 다르게 줄 필요가 없다. 그래서 모두 8080을 쓴다.

---

## 2. Dockerfile 작성

`code microservices/product-service/Dockerfile` 들어가서 아래 내용 전체 입력 (새 파일, 확장자 없음)

교재 본문에 먼저 나오는 `FROM openjdk:24` 4줄짜리는 **원리 설명용 예시**라 쓰지 않는다. Docker Hub의 `openjdk` 공식 이미지는 더 이상 관리되지 않는다(deprecated). 교재 소스(`$BOOK_HOME/Chapter04/microservices/product-service/Dockerfile`)의 2단계 버전을 쓰고 ★ 표시한 곳을 바꾼다.

```dockerfile
# ★ 변경 ① (교재: eclipse-temurin:24_36-jre-noble)
FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /extracted
ADD ./build/libs/*.jar app.jar
# ★ 변경 ② (교재: RUN java -Djarmode=layertools -jar app.jar extract)
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

# ★ 변경 ① (교재: eclipse-temurin:24_36-jre-noble)
FROM eclipse-temurin:21-jre-noble
WORKDIR /application
# ★ 변경 ③ 아래 4줄 경로에 layers/ 추가 (교재: /extracted/dependencies/ ...)
COPY --from=builder /extracted/layers/dependencies/ ./
COPY --from=builder /extracted/layers/spring-boot-loader/ ./
COPY --from=builder /extracted/layers/snapshot-dependencies/ ./
COPY --from=builder /extracted/layers/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```
`#`으로 **시작하는 줄**은 Dockerfile 주석이라 그대로 복사해도 된다. (줄 **중간**의 `#`은 주석이 아니라서 명령 뒤에 붙이면 안 된다.)

### 바꾼 이유와 확인 방법

**① 베이스 이미지 → `21-jre-noble`**
- `24_36`에서 `24`는 자바 버전, `_36`은 **그 버전의 빌드 번호**다. 앞만 바꾼 `21_36`은 존재하지 않는 태그다(이렇게 썼다가 확인해서 고쳤다).
- jar가 Java 21로 컴파일되었으므로 실행 환경도 21로 맞춘다. `21-jre-noble` = 자바 21 최신 빌드, 실행용 JRE, Ubuntu 24.04(noble) 기반.
- 확인 (이미지를 받지 않고 태그 존재만 확인):
  ```bash
  docker manifest inspect eclipse-temurin:21_36-jre-noble > /dev/null && echo 있음 || echo 없음   # 없음
  docker manifest inspect eclipse-temurin:21-jre-noble > /dev/null && echo 있음 || echo 없음      # 있음
  ```

**② `layertools` → `tools ... extract --layers --launcher --destination layers`**
- `layertools`는 Boot 3.3부터 deprecated다. Boot 4.0.9에서는 경고와 함께 아직 동작하지만 없어질 방식이라 새 방식으로 바꾼다(교수님 안내).
- 확인: 옛 방식을 실행하면 **경고 메시지가 대체 명령을 직접 알려 준다.** 새 방식 옵션은 jar 내장 도움말로 본다.
  ```bash
  cd /tmp && cp ~/msa/microservices/product-service/build/libs/*.jar app.jar
  java -Djarmode=layertools -jar app.jar extract
  # Warning: This command is deprecated. Use '-Djarmode=tools extract --layers --launcher' instead.
  java -Djarmode=tools -jar app.jar help extract
  ```

| 옵션 | 뜻 |
|---|---|
| `--layers` | 교재처럼 4개 레이어 폴더로 나눠서 풀기 |
| `--launcher` | 스프링 부트 실행기(JarLauncher)도 같이 풀기 → `ENTRYPOINT`를 교재 그대로 쓸 수 있음 |
| `--destination layers` | 풀 위치를 `layers/` 폴더로 지정. 빼면 jar 이름을 딴 `app/` 폴더에 풀린다 |

**③ COPY 경로에 `layers/` 추가**
- 새 방식은 `--destination`으로 지정한 폴더 **안에** 풀기 때문에 경로가 한 단계 깊어진다. 바뀐 건 폴더 이름 하나뿐이고 COPY 줄 수(4줄)는 그대로다.
  ```
  옛 방식 (layertools)          새 방식 (tools --destination layers)
  /extracted/                   /extracted/
  ├── app.jar                   ├── app.jar
  ├── dependencies/             └── layers/
  ├── spring-boot-loader/           ├── dependencies/
  ├── snapshot-dependencies/        ├── spring-boot-loader/
  └── application/                  ├── snapshot-dependencies/
                                    └── application/
  ```
- ②만 바꾸고 ③을 안 바꾸면 COPY에서 `"/extracted/dependencies": not found` 오류가 난다.
- 확인:
  ```bash
  cd /tmp && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers
  ls layers            # application  dependencies  snapshot-dependencies  spring-boot-loader
  ```
- 교재의 `/extracted//application/`(슬래시 2개)은 오타 같은 표기라 `/extracted/layers/application/`으로 정리했다.

### 구성
| 부분 | 하는 일 |
|---|---|
| 1단계 `AS builder` | jar를 넣고 `tools extract`로 4개 폴더(레이어)로 풀어낸다 |
| 2단계 | 풀어낸 폴더만 최종 이미지로 복사한다. 1단계 이미지는 버려진다 |
| `COPY` 4줄 | 잘 안 바뀌는 것(의존성 라이브러리)부터 자주 바뀌는 것(내 코드) 순서. 레이어마다 따로 복사해야 Docker가 레이어별로 캐시를 쓴다. 내 코드만 바뀌면 앞의 레이어는 재사용해서 빌드가 빠르다 |
| `EXPOSE 8080` | 컨테이너가 8080을 쓴다고 표시 (1번의 docker 프로파일 포트) |
| `ENTRYPOINT` | 컨테이너가 시작될 때 실행할 명령. 풀어낸 폴더 구조에서 스프링 부트를 띄운다. `JarLauncher`는 Boot 4.0.9 jar에도 그대로 있다 |

다음: [04-jar-build.md](04-jar-build.md)
