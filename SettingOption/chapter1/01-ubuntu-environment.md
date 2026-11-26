# 01. Ubuntu 환경 설정 (처음 한 번만)

> 교재 22장. WSL2 + Ubuntu 24.04, Docker Desktop, VS Code, Java 21, Spring CLI 등 도구를 설치한다.
> **이미 설치되어 있으면 건너뛰고 [02-project-folder.md](../chapter2/02-project-folder.md)부터 시작한다.** (`~/msa` 폴더만 새로 만드는 경우)
> 막히면 [troubleshooting.md](troubleshooting.md).

## 0. 이미 Ubuntu-24.04를 쓰고 있는데 1장부터 다시 해 보고 싶을 때
**기존 Ubuntu를 지우지 말고, 연습용 Ubuntu를 하나 더 설치한다.** 기존 것을 지우면(`wsl --unregister`) 그 안의 파일이 전부 사라지고 되돌릴 수 없다. 예: push 안 한 커밋, `auth0/env.bash`(GitHub에 없는 비밀값), 교재 소스, `/etc/hosts` 설정.

PowerShell(관리자)에서 다른 이름으로 설치한다 (WSL 2.4 이상, `wsl --version`으로 확인):
```powershell
wsl --install Ubuntu-24.04 --name Ubuntu-test
```
- 설치가 끝나면 새 사용자 이름과 비밀번호를 정한다. 시작 메뉴 또는 Windows Terminal에서 `Ubuntu-test`을 열고 아래 1절부터 진행한다.
- 이 문서에서 `Ubuntu-24.04`, `leo`라고 적힌 곳은 `Ubuntu-test`, 새로 정한 사용자 이름으로 바꿔 읽는다.
- **Docker Desktop**: Settings → Resources → WSL integration에서 `Ubuntu-test`도 켠다. 안 켜면 새 Ubuntu에서 `docker` 명령이 없다고 나온다.
- **VS Code**: `code .`로 열면 왼쪽 아래가 `WSL: Ubuntu-test`이다. Java 확장도 이 Ubuntu에 **따로** 설치해야 한다(1절 VS Code의 "WSL: ...에 설치").
- 둘 다 켜 둬도 된다(메모리 24GB 설정 기준). 1~7장은 minikube를 쓰지 않는다.
- 연습이 끝나서 필요 없으면 연습용만 지운다:
  ```powershell
  wsl --unregister Ubuntu-test
  ```

**그래도 기존 Ubuntu-24.04를 지우고 새로 깔 때**는 먼저 백업한다.
1. `cd ~/msa && git push` (커밋을 GitHub에 올림)
2. `auth0/env.bash`를 Windows 쪽 안전한 곳에 복사 (`cp ~/msa/auth0/env.bash /mnt/c/Users/<Windows 사용자>/`)
3. 전체 백업: `mkdir C:\backup` 후 `wsl --export Ubuntu-24.04 C:\backup\ubuntu2404.tar` (나중에 `wsl --import`로 그대로 되살릴 수 있다)
4. `wsl --unregister Ubuntu-24.04` → `wsl --install -d Ubuntu-24.04`

## 1. Windows 쪽 준비 (WSL2, Docker Desktop, VS Code)
> 이 부분은 명령 기록이 남아 있지 않다. 지금 상태로 확인된 결과만 적는다.

### WSL2 + Ubuntu 24.04
- 일반적으로는 PowerShell(관리자)에서 `wsl --install -d Ubuntu-24.04`를 실행한다. (이미 있으면 0절처럼 `--name`으로 하나 더)
- `/etc/wsl.conf`의 `default=` 뒤는 설치할 때 정한 사용자 이름이다.
- `/etc/wsl.conf` 내용:
  ```ini
  [boot]
  systemd=true

  [user]
  default=leo
  ```

### Docker Desktop
- Windows에 설치하고 Settings → Resources → WSL integration에서 Ubuntu를 켠다(연습용 Ubuntu를 추가했으면 그것도 켠다).
- 그러면 WSL 안의 `docker` 명령이 `/mnt/wsl/docker-desktop/...`에 연결된다. **Ubuntu 안에 Docker를 따로 설치하지 않는다.**

### VS Code
- Windows에 설치하고 WSL 확장(Remote - WSL)을 추가한다.
- Ubuntu 터미널에서 `code .`를 실행하면 VS Code가 WSL에 연결된 상태로 열린다.
- **확장은 WSL 쪽에 따로 설치해야 동작한다.** Windows에 Java 확장이 있어도 WSL에 없으면 `.java` 파일에서 단어 기반 자동 완성만 동작한다. 확장 탭에서 `Extension Pack for Java`를 찾아 "WSL: Ubuntu-24.04에 설치"를 누른다.

### 확인
```bash
lsb_release -d     # Ubuntu 24.04 확인
cd ~
code .             # VS Code 연결 확인
```

## 2. apt 기본 도구, Helm
### apt로 기본 도구 설치
```bash
sudo apt update
sudo apt install -y jq zip unzip siege
```
- `jq`: JSON을 보기 좋게 출력한다(`curl ... | jq .`).
- `zip`, `unzip`: SDKMAN 설치에 필요하다.
- `siege`: 부하 테스트 도구다.

### Helm 설치 (공식 스크립트)
```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
helm version
rm get_helm.sh
```
apt 저장소 방식은 실패했다 → [troubleshooting.md](troubleshooting.md)

## 3. SDKMAN → Java 21, Spring Boot CLI
### SDKMAN 설치
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk version
```
SDKMAN을 설치하면 `~/.bashrc`에 초기화 코드가 자동으로 추가된다. 새 터미널을 열면 따로 `source`하지 않아도 된다.

### Java 21 (Temurin)
```bash
sdk list java | grep tem
sdk install java 21.0.13.0+9-tem
sdk default java 21.0.13.0+9-tem
java -version
```
- `sdk default`는 이후 열리는 모든 터미널에 적용된다.
- `sdk use`는 현재 터미널에만 적용된다.

### Spring Boot CLI
```bash
sdk update
sdk list springboot
sdk install springboot 4.0.7
spring --version
```

**`java -version` 또는 `spring --version`이 `Command 'java' not found` / `JAVA_HOME not set and cannot find javac`로 나올 때**
Java는 설치됐는데 **지금 터미널이 Java 경로를 모르는 것**이다. SDKMAN은 터미널을 열 때 그 시점에 있는 도구의 경로만 PATH에 넣는데, 이 터미널은 Java를 설치하기 전에 열렸다. 설정을 다시 읽게 한다(`~/.sdkman` 사이에 공백 없이):
```bash
source ~/.sdkman/bin/sdkman-init.sh
hash -r
java -version     # openjdk version "21.0.13"
spring --version  # Spring CLI v4.0.7
```
터미널을 닫았다가 다시 열어도 된다. 화면에 나오는 `sudo apt install openjdk-...` 안내는 따르지 않는다(Java가 두 개가 되어 꼬인다).

apt로 Java 17을 먼저 설치했다가 지운 기록 → [troubleshooting.md](troubleshooting.md)

## 4. kubectl, minikube, istioctl (15장부터 사용)
### kubectl
```bash
curl -LO "https://dl.k8s.io/release/v1.32.0/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
rm kubectl
kubectl version --client
```

### minikube
```bash
curl -LO https://storage.googleapis.com/minikube/releases/v1.35.0/minikube-linux-amd64
sudo install -o root -g root -m 0755 minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64
minikube version
```

### istioctl (버전 1.24.2로 고정)
```bash
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.24.2 TARGET_ARCH=x86_64 sh -
sudo install -o root -g root -m 0755 istio-1.24.2/bin/istioctl /usr/local/bin/istioctl
rm -r istio-1.24.2
istioctl version --remote=false
```
버전을 지정하지 않으면 최신 버전이 설치된다 → [troubleshooting.md](troubleshooting.md)

## 5. 설치 버전 확인, 교재 소스 내려받기
### 설치 버전 한 번에 확인
```bash
git version ; docker version -f json | jq -r .Client.Version ; java -version 2>&1 | grep "openjdk version" ; curl --version | grep "curl" ; jq --version ; spring --version ; siege --version 2>&1 | grep SIEGE ; helm version --short ; kubectl version --client ; minikube version | grep "minikube" ; istioctl version --remote=false
```
결과가 [README.md](README.md)의 설치 결과 요약표와 같은지 확인한다.

### 교재 소스코드 내려받기
```bash
echo 'export BOOK_HOME=~/spring-microservice' >> ~/.bashrc
source ~/.bashrc
echo $BOOK_HOME

git clone https://github.com/wikibook/spring-microservice.git $BOOK_HOME
cd $BOOK_HOME
ls
code .
```
- `$BOOK_HOME`은 이후 모든 장에서 교재 소스 경로로 쓴다. 예: `$BOOK_HOME/Chapter03/2-basic-rest-services`
- 교재 소스는 **Java 24 / Spring Boot 3.5.0** 기준이다. 실습 레포(`~/msa`)로 가져올 때는 버전을 맞춰서 바꾼다.

---
다음: [02-project-folder.md](../chapter2/02-project-folder.md)
