# 03. SDKMAN → Java 21, Spring Boot CLI

## SDKMAN 설치
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk version
```
SDKMAN을 설치하면 `~/.bashrc`에 초기화 코드가 자동으로 추가된다. 새 터미널을 열면 따로 `source`하지 않아도 된다.

## Java 21 (Temurin)
```bash
sdk list java | grep tem
sdk install java 21.0.13.0+9-tem
sdk default java 21.0.13.0+9-tem
java -version
```
- `sdk default`는 이후 열리는 모든 터미널에 적용된다.
- `sdk use`는 현재 터미널에만 적용된다.

## Spring Boot CLI
```bash
sdk update
sdk list springboot
sdk install springboot 4.0.7
spring --version
```

apt로 Java 17을 먼저 설치했다가 지운 기록 → [troubleshooting.md](troubleshooting.md)

다음: [04-kubernetes-tools.md](04-kubernetes-tools.md)
