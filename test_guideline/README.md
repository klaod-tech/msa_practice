# test_guideline: 교수님 코드 기준 장별 실습 문서

> 기준 코드: 교수님 저장소 [spring-microservice-boot4](https://github.com/NohYounghoon3651/spring-microservice-boot4) (Spring Boot 4.0.9 / Java 21 / Spring Cloud 2025.1.4 / Gradle 8.14.6)
> 예전 문서(우리 코드 기준, Lombok 등)는 [../SettingOption/](../SettingOption/)에 그대로 있다.

## 이 문서의 약속
- **N장 문서는 교수님 `Chapter(N-1)` 상태에서 시작해서, 문서대로 하면 교수님 `ChapterN`과 같은 코드가 된다.** 교재의 영어 주석만 빠진다.
- 장마다 빈 작업 폴더에서 문서 글자 그대로 따라 해 보고, 결과를 교수님 코드와 비교한 뒤 빌드와 테스트까지 돌려서 확인했다. 확인한 내용은 장별 README의 "검증" 칸에 있다.
- 파일 단계는 `code 경로` 코드 블록 → "붙여넣기 (새 파일)" 또는 "아래처럼 맞추기 (전체 파일)" 순서다. 바뀐 줄에는 `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제`가 붙어 있다. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.
- 모든 명령은 작업 폴더 `~/msa`에서 실행한다. 다른 폴더에서 하면 `~/msa`를 그 폴더로 바꿔 읽는다.

## 중간 장부터 시작하기
교수님 코드의 앞 장 결과를 작업 폴더에 복사하고 그 장 문서를 따라 한다. 예: 12장부터 하려면 교수님 `Chapter11`을 복사한다.
```bash
cd ~
git clone https://github.com/NohYounghoon3651/spring-microservice-boot4.git
mkdir -p ~/work
cp -r ~/spring-microservice-boot4/Chapter11/. ~/work/
cd ~/work
```
이미 받았다면 `git clone` 대신 `cd ~/spring-microservice-boot4 && git pull`.

## 쿠버네티스 준비 (15장부터, 한 번만)
15~20장은 minikube에 배포한다. 이 PC(WSL + Docker Desktop)에서 확인한 준비 사항이다.

**1. 도구 버전 (교수님 기준)**

| 도구 | 버전 |
|---|---|
| minikube | 1.39.0 (Kubernetes 1.37.0) |
| kubectl | 1.37.2 |
| istioctl | 1.31.4 (18장부터) |
| helm | 3.22.1 |

```bash
minikube version; kubectl version --client; helm version --short; istioctl version --remote=false
```

**2. WSL 메모리 늘리기** — minikube에 10~12GB를 주므로, WSL 기본값(PC 메모리의 절반, 이 PC는 16GB)으로는 Docker Desktop이 멈춘다. `%UserProfile%\.wslconfig`(확장자 없음) 파일에:
```ini
[wsl2]
memory=24GB
swap=8GB
```
관리자 PowerShell에서 Docker를 끄고 WSL을 재시작한 뒤 Docker Desktop을 다시 켠다.
```powershell
Stop-Service com.docker.service -ErrorAction SilentlyContinue
Get-Process *docker* -ErrorAction SilentlyContinue | Stop-Process -Force
wsl --shutdown
```
Ubuntu에서 `free -g`의 total이 23 근처면 적용된 것이다(PC 메모리 32GB 기준).

**3. 호스트 이름 (17장부터)** — Ubuntu에서 한 번만. 첫 줄이 없으면 WSL을 재시작할 때(`wsl --shutdown`) `/etc/hosts`가 새로 만들어져서 넣은 줄이 사라진다.
```bash
printf '\n[network]\ngenerateHosts = false\n' | sudo tee -a /etc/wsl.conf
echo "127.0.0.1 minikube.me health.minikube.me grafana.minikube.me kiali.minikube.me prometheus.minikube.me tracing.minikube.me kibana.minikube.me elasticsearch.minikube.me mail.minikube.me dashboard.minikube.me" | sudo tee -a /etc/hosts
```

**4. 접속 방법 (교수님 안내와 다름)** — 교수님 안내는 `minikube ip`(192.168.49.x)로 접속하지만, WSL + Docker Desktop에서는 그 주소에 닿지 않는다. 대신 `kubectl port-forward`로 내 PC(`localhost`, `127.0.0.1`)의 포트에 연결한다. 장마다 어떤 서비스를 어느 포트로 연결하는지는 각 장의 마지막 문서에 있다.

**5. 메모리 주의** — minikube를 쓰는 동안에는 도커 컴포즈로 띄운 것을 내린다(`docker compose down`).

**6. 443 포트 열기 (20장)** — 20장 test-em-all은 `https://health.minikube.me/...`처럼 443 포트를 고정해 쓰는 줄이 있어서 port-forward를 443으로 해야 한다. 1024보다 낮은 포트를 일반 사용자도 열 수 있게 한 번만 설정한다(재시작해도 유지).
```bash
echo "net.ipv4.ip_unprivileged_port_start=443" | sudo tee /etc/sysctl.d/99-unprivileged-443.conf
sudo sysctl -w net.ipv4.ip_unprivileged_port_start=443
```

## 장 목록

| 부 | 장 | 문서 | 시작 → 결과 | 상태 |
|---|---|---|---|---|
| - | 환경 | [../SettingOption/setting/01-ubuntu-environment.md](../SettingOption/setting/01-ubuntu-environment.md) | WSL, Docker, Java, Spring CLI, kubectl 등 | 설치 완료 |
| 1부 | 3 | [chapter3](chapter3/README.md) | 빈 폴더 → Chapter03/2-basic-rest-services | 검증 완료 |
| 1부 | 4 | [chapter4](chapter4/README.md) | Chapter03/2-basic-rest-services → Chapter04 | 검증 완료 |
| 1부 | 5 | [chapter5](chapter5/README.md) | Chapter04 → Chapter05 | 검증 완료 |
| 1부 | 6 | [chapter6](chapter6/README.md) | Chapter05 → Chapter06 | 검증 완료 |
| 1부 | 7 | [chapter7](chapter7/README.md) | Chapter06 → Chapter07 | 검증 완료 |
| 2부 | 8 | (코드 없음) | | |
| 2부 | 9 | [chapter9](chapter9/README.md) | Chapter07 → Chapter09 | 검증 완료 |
| 2부 | 10 | [chapter10](chapter10/README.md) | Chapter09 → Chapter10 | 검증 완료 |
| 2부 | 11 | [chapter11](chapter11/README.md) | Chapter10 → Chapter11 | 검증 완료 |
| 2부 | 12 | [chapter12](chapter12/README.md) | Chapter11 → Chapter12 | 검증 완료 |
| 2부 | 13 | [chapter13](chapter13/README.md) | Chapter12 → Chapter13 | 검증 완료 |
| 2부 | 14 | [chapter14](chapter14/README.md) | Chapter13 → Chapter14 | 검증 완료 |
| 3부 | 15 | [chapter15](chapter15/README.md) | Chapter14 → Chapter15 | 검증 완료 |
| 3부 | 16 | [chapter16](chapter16/README.md) | Chapter15 → Chapter16 | 검증 완료 |
| 3부 | 17 | [chapter17](chapter17/README.md) | Chapter16 → Chapter17 | 검증 완료 |
| 3부 | 18 | [chapter18](chapter18/README.md) | Chapter17 → Chapter18 | 검증 완료 |
| 3부 | 19 | [chapter19](chapter19/README.md) | Chapter18 → Chapter19 | 검증 완료 |
| 3부 | 20 | [chapter20](chapter20/README.md) | Chapter19 → Chapter20 | 검증 완료 |
