# 01. Windows 쪽 준비

> **환경 설정이 이미 되어 있으면 [06-create-projects.md](06-create-projects.md)부터 시작한다.**
> WSL/Ubuntu, Docker Desktop, VS Code, Java 21, Spring CLI, jq, 교재 소스(`~/spring-microservice`)가 이미 설치돼 있고 `~/msa` 폴더만 새로 만드는 경우다. 01~05는 건너뛴다.
> 09(GitHub)는 기존 레포(`msa_practice`)에 push하지 않는다. 새 폴더는 기록이 달라서 push가 거절되고, 강제로 밀면 기존 기록과 문서가 덮어써진다.

> 이 부분은 명령 기록이 남아 있지 않다. 지금 상태로 확인된 결과만 적는다.

## WSL2 + Ubuntu 24.04
- 일반적으로는 PowerShell(관리자)에서 `wsl --install -d Ubuntu-24.04`를 실행한다.
- `/etc/wsl.conf` 내용:
  ```ini
  [boot]
  systemd=true

  [user]
  default=leo
  ```

## Docker Desktop
- Windows에 설치하고 Settings → Resources → WSL integration에서 Ubuntu를 켠다.
- 그러면 WSL 안의 `docker` 명령이 `/mnt/wsl/docker-desktop/...`에 연결된다. **Ubuntu 안에 Docker를 따로 설치하지 않는다.**

## VS Code
- Windows에 설치하고 WSL 확장(Remote - WSL)을 추가한다.
- Ubuntu 터미널에서 `code .`를 실행하면 VS Code가 WSL에 연결된 상태로 열린다.
- **확장은 WSL 쪽에 따로 설치해야 동작한다.** Windows에 Java 확장이 있어도 WSL에 없으면 `.java` 파일에서 단어 기반 자동 완성만 동작한다. 확장 탭에서 `Extension Pack for Java`를 찾아 "WSL: Ubuntu-24.04에 설치"를 누른다.

## 확인
```bash
lsb_release -d     # Ubuntu 24.04 확인
cd ~
code .             # VS Code 연결 확인
```

다음: [02-apt-helm.md](02-apt-helm.md)
