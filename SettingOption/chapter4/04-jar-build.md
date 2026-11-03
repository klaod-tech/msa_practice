# 04. jar 빌드와 확인 (도커 이미지 빌드 준비)

> 모든 명령은 `~/msa`에서 실행한다.

## 1. jar 빌드
교재는 `cd $BOOK_HOME/Chapter04` 후 실행하지만, 우리는 **내 레포**에서 한다.
```bash
./gradlew :microservices:product-service:build
```
- **jar 빌드는 `~/msa`(루트)에서** 한다. `gradlew`가 루트에만 있고(3장에서 래퍼를 루트로 모음), `:microservices:product-service:build`도 루트 settings.gradle 기준 이름이라 폴더를 옮기지 않아도 product-service만 빌드된다. product-service 폴더 안에서 `./gradlew`를 치면 `No such file or directory`가 난다.
- `:microservices:product-service:build`는 settings.gradle의 `include ':microservices:product-service'` 이름이다. 전체(`./gradlew build`)가 아니라 **product-service만** 빌드한다.
- 폴더를 옮기는 건 [05](05-docker-image-run.md)의 `docker build` 때다(Dockerfile이 있는 `microservices/product-service`로 이동).
- Dockerfile 3번 줄 `ADD ./build/libs/*.jar app.jar`는 **이미 만들어진 jar**를 넣을 뿐이다. yml이나 코드를 바꿨으면 이미지를 만들기 전에 반드시 jar를 다시 빌드한다.

## 2. jar 확인 (교재)
```bash
ls -l microservices/product-service/build/libs/
```
첫 줄 `total 37376`은 디스크 블록 수라 볼 필요 없다. 그 아래 jar 줄을 본다.
```
-rw-r--r-- 1 leo leo 38269912 Nov  3 11:05 product-service-1.0.0-SNAPSHOT.jar
                      크기      빌드 시각     파일 이름
```
- **크기**: 의존성 라이브러리까지 전부 들어 있어서 크다 → **팻(fat) JAR**. 교재는 약 20MB, 우리는 약 38MB다(Spring Boot 버전과 의존성이 달라서 크기가 다르다).
- **빌드 시각**: 방금 `./gradlew`를 실행한 시각인지
- **jar가 하나만** 있는지: `*-plain.jar`가 없어야 `jar { enabled = false }`가 동작하는 것

---

## 3. (선택) jar 안 내용 확인

### 3-1. jar 안 파일 목록 보기 (교재)
```bash
unzip -l microservices/product-service/build/libs/product-service-1.0.0-SNAPSHOT.jar
```
`-l`은 jar(zip 파일)를 풀지 않고 **안에 든 파일 목록**만 보여 준다. 내 코드(`BOOT-INF/classes/`)와 의존성 라이브러리(`BOOT-INF/lib/*.jar`)가 함께 들어 있는 게 보인다.

### 3-2. jar 안의 yml에 docker 프로파일이 들어갔는지 보기 (교재에 없음, yml을 고친 뒤 권장)
```bash
unzip -p microservices/product-service/build/libs/*.jar BOOT-INF/classes/application.yml | tail -4
```
맨 끝에 이렇게 나오면 새 jar다.
```
---
spring.config.activate.on-profile: docker

server.port: 8080
```
안 나오면 jar가 옛날 것이다 → 1번 jar 빌드를 다시 한다.

**왜 확인하나**
- 이미지에 들어가는 건 소스 폴더의 yml이 아니라 **jar 안에 포장된 yml**이다. Dockerfile은 `build/libs`의 jar를 그대로 넣을 뿐, 소스를 다시 읽지 않는다.
- 그래서 소스 yml을 고쳐도 jar를 다시 빌드하지 않았으면 이미지에는 **옛 설정**이 들어간다. 오류도 안 나고 조용히 틀린다.
- 실제로 있었던 일: 어제 만든 jar(docker 프로파일 추가 전)로 이미지를 만들었더니, docker 프로파일을 켜도 **8080이 아니라 7002로 떴다.**
- 이미지를 만들고 컨테이너를 띄운 뒤에야 알면 원인 찾기가 어렵다. 이미지를 만들기 **전에** 확인해서 막는다.

| 명령 | 알 수 있는 것 |
|---|---|
| `ls -l` | jar가 있는지, 언제 만들어졌는지, 하나뿐인지 |
| `unzip -l` | jar 안에 어떤 파일들이 들어 있는지 (목록) |
| `unzip -p ... application.yml` | jar 안 yml의 **실제 내용** (`-p` = 파일 하나를 화면에 출력, `tail -4` = 마지막 4줄) |

---

다음: [05-docker-image-run.md](05-docker-image-run.md)
