# 06. 서비스 4개 생성

## 환경 확인
```bash
java -version          # Java 21 확인
spring --version       # Spring CLI 확인
sudo apt install -y tree
```

## 실습 폴더 생성
```bash
cd ~
mkdir msa
cd ~/msa
```

## `create-projects.bash` 작성
교재 스크립트는 Boot 3 / Java 17로 생성되므로 쓰지 않고, Boot 4.0.9 / Java 21로 직접 작성했다.
```bash
nano create-projects.bash
```
```bash
#!/usr/bin/env bash
mkdir -p microservices
cd microservices

create() {
  spring init \
  --boot-version=4.0.9 \
  --type=gradle-project \
  --java-version=21 \
  --packaging=jar \
  --name=$1 \
  --package-name=$2 \
  --groupId=$2 \
  --dependencies=actuator,webflux \
  --version=1.0.0-SNAPSHOT \
  $1
}

create product-service           se.magnus.microservices.core.product
create review-service            se.magnus.microservices.core.review
create recommendation-service    se.magnus.microservices.core.recommendation
create product-composite-service se.magnus.microservices.composite.product
```

## 스크립트 실행
```bash
chmod +x create-projects.bash
./create-projects.bash
ls microservices
```

다음: [07-multi-project.md](07-multi-project.md)
