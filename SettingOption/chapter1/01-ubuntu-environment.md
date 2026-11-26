# 01. Ubuntu 환경 설정 (처음 한 번만)

> 교재 22장. WSL2 + Ubuntu 24.04, Docker Desktop, VS Code, Java 21, Spring CLI 등 도구를 설치한다.
> **이미 설치되어 있으면 건너뛰고 [02-project-folder.md](../chapter2/02-project-folder.md)부터 시작한다.** (`~/msa` 폴더만 새로 만드는 경우)
> 막히면 [troubleshooting.md](troubleshooting.md).

## 0. 이미 Ubuntu-24.04를 쓰고 있는데 1장부터 다시 해 보고 싶을 때
**기존 Ubuntu를 지우지 말고, 연습용 Ubuntu를 하나 더 설치한다.** 기존 것을 지우면(`wsl --unregister`) 그 안의 파일이 전부 사라지고 되돌릴 수 없다. 예: push 안 한 커밋, `auth0/env.bash`(GitHub에 없는 비밀값), 교재 소스, `/etc/hosts` 설정.

PowerShell(관리자)에서 다른 이름으로 설치한다 (WSL 2.4 이상, `wsl --version`으로 확인). **`wsl` 명령은 Windows PowerShell 명령이다. Ubuntu 터미널에서 치면 `wsl: command not found`가 난다.**

전에 만든 연습용(예: `Ubuntu-exam`)이 있으면 먼저 지운다(그 안의 파일은 모두 사라진다):
```powershell
wsl -l -v                         # 설치된 Ubuntu 목록 확인
wsl --unregister Ubuntu-exam      # 전에 만든 연습용만 (Ubuntu-24.04는 지우지 않는다)
```
새로 설치:
```powershell
wsl --install Ubuntu-24.04 --name Ubuntu-test
```
- 설치 중 `Create a default Unix user account:`에서 사용자 이름은 **`leo`**(소문자로 시작), 비밀번호는 기억하기 쉬운 것으로 정한다. `leo`로 하면 문서의 `/home/leo`, `default=leo`를 그대로 읽으면 된다. 비밀번호는 입력해도 화면에 안 보이는 게 정상이다(`sudo` 때마다 묻는다).
- 설치가 끝나면 터미널이 `/mnt/c/WINDOWS/system32`(Windows 폴더)에서 시작한다. **먼저 `cd ~`로 홈(`/home/leo`)으로 간다.**
- 다음부터는 시작 메뉴 또는 Windows Terminal에서 `Ubuntu-test`를 열고 아래 1절부터 진행한다.
- 이 문서에서 `Ubuntu-24.04`라고 적힌 곳은 `Ubuntu-test`로 바꿔 읽는다.
- **Docker Desktop**: Settings → Resources → WSL integration에서 `Ubuntu-test`도 켠다. 안 켜면 새 Ubuntu에서 `docker` 명령이 없다고 나온다.
- **VS Code**: `code .`로 열면 왼쪽 아래가 `WSL: Ubuntu-test`이다. 처음 연결할 때 VS Code가 이 Ubuntu에 서버와 확장을 설치하느라 몇 분 걸리고, 홈에 `.vscode-server`, `.redhat` 같은 숨김 폴더가 새로 생긴다(정상). Java 확장(`Extension Pack for Java`)이 "WSL: Ubuntu-test에 설치됨"으로 보이는지 확인하고, 없으면 설치한다(1절 VS Code).
- **두 Ubuntu에 같은 `/home/leo/msa`가 있다.** `code 파일`은 그 명령을 친 터미널의 Ubuntu 파일을 연다. 작업 전에 VS Code 창 왼쪽 아래가 `WSL: Ubuntu-test`인지 확인한다(기존 `Ubuntu-24.04` 창에서 열면 기존 레포 파일이 열린다).
- **Docker Desktop은 두 Ubuntu가 함께 쓴다.** 기존 Ubuntu에서 띄운 컨테이너나 minikube가 켜져 있으면 8080 등 포트가 겹친다. 연습 중에는 기존 쪽을 켜지 않는다(`docker ps`가 비어 있으면 된다).
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
> **1~7장만 할 때는 이 절과 2절의 Helm을 건너뛴다.** 15~20장(3부)을 할 때 아래를 순서대로 한다. 버전은 교수님 저장소 기준이다.

| 도구 | 버전 | 쓰는 장 |
|---|---|---|
| kubectl | 1.37.2 | 15장~ |
| minikube | 1.39.0 (Kubernetes 1.37.0) | 15장~ |
| helm | 3.22.1 (2절에서 설치) | 16장~ |
| istioctl | 1.31.4 | 18장~ |

### 4-1. kubectl
```bash
curl -LO "https://dl.k8s.io/release/v1.37.2/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
rm kubectl
kubectl version --client          # Client Version: v1.37.2
```

### 4-2. minikube
```bash
curl -LO https://storage.googleapis.com/minikube/releases/v1.39.0/minikube-linux-amd64
sudo install -o root -g root -m 0755 minikube-linux-amd64 /usr/local/bin/minikube
rm minikube-linux-amd64
minikube version                  # minikube version: v1.39.0
```
클러스터는 15장에서 `minikube start ...`로 처음 만든다(처음에는 이미지를 받아서 몇 분 걸린다).

### 4-3. helm 버전 확인
2절에서 설치했다. 버전이 다르면 2절의 스크립트를 다시 실행한다.
```bash
helm version --short              # v3.22.1+...
```

### 4-4. istioctl (버전 1.31.4로 고정)
```bash
cd ~
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.31.4 TARGET_ARCH=x86_64 sh -
sudo install -o root -g root -m 0755 istio-1.31.4/bin/istioctl /usr/local/bin/istioctl
istioctl version --remote=false   # client version: 1.31.4
```
- `ISTIO_VERSION`을 빼면 최신 버전이 설치된다 → [troubleshooting.md](troubleshooting.md)
- `~/istio-1.31.4` 폴더는 지우지 않는다. 18장에서 그 안의 애드온(Kiali, Jaeger, Prometheus, Grafana) 설정 파일을 쓴다.

### 4-5. 쿠버네티스 실습 준비 (WSL + Docker Desktop, 한 번만)
**① WSL 메모리 늘리기** — minikube에 10~12GB를 주므로 WSL 기본값(PC 메모리의 절반)으로는 Docker Desktop이 멈춘다. Windows의 `%UserProfile%\.wslconfig`(확장자 없음, 메모장으로 만들 때 `.txt`가 붙지 않게 주의) 파일에:
```ini
[wsl2]
memory=24GB
swap=8GB
```
PC 메모리 32GB 기준이다. 관리자 PowerShell에서 Docker를 끄고 WSL을 재시작한 뒤 Docker Desktop을 다시 켠다.
```powershell
Stop-Service com.docker.service -ErrorAction SilentlyContinue
Get-Process *docker* -ErrorAction SilentlyContinue | Stop-Process -Force
wsl --shutdown
```
Ubuntu에서 `free -g`의 total이 23 근처면 적용된 것이다.

**② 호스트 이름 (17장부터)** — Ubuntu에서 한 번만. 첫 줄이 없으면 WSL을 재시작할 때 `/etc/hosts`가 새로 만들어져서 넣은 줄이 사라진다.
```bash
printf '\n[network]\ngenerateHosts = false\n' | sudo tee -a /etc/wsl.conf
echo "127.0.0.1 minikube.me health.minikube.me grafana.minikube.me kiali.minikube.me prometheus.minikube.me tracing.minikube.me kibana.minikube.me elasticsearch.minikube.me mail.minikube.me dashboard.minikube.me" | sudo tee -a /etc/hosts
```
Windows 브라우저로도 열려면 `C:\Windows\System32\drivers\etc\hosts`(관리자 메모장)에 같은 줄을 추가한다.

**③ 443 포트 열기 (20장)** — 20장 test-em-all은 `https://health.minikube.me/...`처럼 443 포트를 고정해 쓴다. 1024보다 낮은 포트를 일반 사용자도 열 수 있게 한 번만 설정한다(재시작해도 유지).
```bash
echo "net.ipv4.ip_unprivileged_port_start=443" | sudo tee /etc/sysctl.d/99-unprivileged-443.conf
sudo sysctl -w net.ipv4.ip_unprivileged_port_start=443
```

**④ 접속 방법 (교재와 다름)** — 교재는 `minikube ip`(192.168.49.x)로 접속하지만, WSL + Docker Desktop에서는 그 주소에 닿지 않는다. 대신 `kubectl port-forward`로 내 PC(`localhost`)의 포트에 연결한다. 장마다 어떤 서비스를 어느 포트로 연결하는지는 각 장 마지막 문서에 있다.

**⑤ 메모리 주의** — minikube를 쓰는 동안에는 도커 컴포즈로 띄운 것을 내린다(`docker compose down`).

## 5. 설치 버전 확인, 교재 소스 내려받기
### 설치 버전 한 번에 확인
```bash
git version ; docker version -f json | jq -r .Client.Version ; java -version 2>&1 | grep "openjdk version" ; curl --version | grep "curl" ; jq --version ; spring --version ; siege --version 2>&1 | grep SIEGE ; helm version --short ; kubectl version --client ; minikube version | grep "minikube" ; istioctl version --remote=false
```
결과가 [README.md](README.md)의 설치 결과 요약표와 같은지 확인한다. 4절(과 Helm)을 건너뛰었으면 `helm`, `kubectl`, `minikube`, `istioctl` 부분은 `command not found`로 나와도 정상이다. `docker`가 안 나오면 0절의 Docker Desktop WSL integration을 켠다.

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
