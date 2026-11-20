# 01. 빈 폴더에서 서비스 4개 골격 만들기 (spring init)

> 시작: 빈 폴더 / 결과: 서비스 4개의 골격(아직 기능 없음). 교재 "Spring Initializr로 골격 코드 생성하기".

## 1. 작업 폴더
<!-- run -->
```bash
mkdir -p ~/msa
cd ~/msa
```
`~/msa`가 이미 있으면 다른 이름(예: `~/msa3`)을 쓰고, 이후 문서의 `~/msa`를 그 이름으로 바꿔 읽는다. 폴더는 **비어 있어야** 한다.

## 2. create-projects.bash (새 파일)
```bash
code create-projects.bash
```
붙여넣기 (새 파일)
```bash
#!/usr/bin/env bash

mkdir microservices
cd microservices

spring init \
--boot-version=4.0.9 \
--type=gradle-project \
--java-version=21 \
--packaging=jar \
--name=product-service \
--package-name=se.magnus.microservices.core.product \
--groupId=se.magnus.microservices.core.product \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
product-service

spring init \
--boot-version=4.0.9 \
--type=gradle-project \
--java-version=21 \
--packaging=jar \
--name=review-service \
--package-name=se.magnus.microservices.core.review \
--groupId=se.magnus.microservices.core.review \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
review-service

spring init \
--boot-version=4.0.9 \
--type=gradle-project \
--java-version=21 \
--packaging=jar \
--name=recommendation-service \
--package-name=se.magnus.microservices.core.recommendation \
--groupId=se.magnus.microservices.core.recommendation \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
recommendation-service

spring init \
--boot-version=4.0.9 \
--type=gradle-project \
--java-version=21 \
--packaging=jar \
--name=product-composite-service \
--package-name=se.magnus.microservices.composite.product \
--groupId=se.magnus.microservices.composite.product \
--dependencies=actuator,webflux \
--version=1.0.0-SNAPSHOT \
product-composite-service

cd ..
```
`spring init`(Spring Initializr)으로 서비스 4개를 만든다. 서비스마다 이름, 패키지, 의존성(`actuator`, `webflux`)을 정한다.

<!-- run -->
```bash
chmod +x create-projects.bash
./create-projects.bash
ls microservices
```
`product-composite-service product-service recommendation-service review-service`가 나오면 된다(인터넷 필요).

## 3. Gradle 래퍼를 루트로
서비스마다 생긴 Gradle 래퍼(`gradlew`, `gradle/`)를 루트로 하나만 옮긴다. 루트에서 한 번에 빌드하기 때문이다.
<!-- run -->
```bash
cp -r microservices/product-service/gradle .
cp microservices/product-service/gradlew microservices/product-service/gradlew.bat .
find microservices -depth -name "gradle" -exec rm -rf "{}" \;
find microservices -depth -name "gradlew*" -exec rm -f "{}" \;
```
- spring init이 만든 `build.gradle` 등은 그날그날 버전에 따라 조금씩 다르다. 다음 문서부터 필요한 파일을 **전체 내용으로** 맞추므로 신경 쓰지 않아도 된다.
- 래퍼 버전은 [04](04-build-compose-test.md)에서 교수님 코드와 같은 8.14.6으로 맞춘다.

다음: [02-api-util.md](02-api-util.md)
