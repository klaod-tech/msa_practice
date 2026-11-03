# 01. 도커 기본 세팅 (연동 + 첫 도커 명령, 교재 93~94쪽)

## 구조
```
Windows
├── Docker Desktop  ← Docker 엔진을 실행 + 컨테이너/이미지를 화면으로 보여 줌
│        ▲
│        │ WSL integration (연동)
│        ▼
└── WSL2 Ubuntu-24.04  ← 여기 터미널에서 docker 명령을 입력
```
- `docker` 명령은 **Ubuntu 터미널에서 입력**한다. Docker Desktop 창에서 명령을 치는 게 아니다.
- Docker Desktop은 **켜져 있어야** 한다(왼쪽 아래 `Engine running`). 꺼져 있으면 Ubuntu에서 docker 명령이 동작하지 않는다.
- Ubuntu 안에 Docker를 따로 설치하지 않는다. Ubuntu의 `docker`는 Docker Desktop이 넣어 준 프로그램이다.
  ```bash
  ls -l /usr/bin/docker
  # /usr/bin/docker -> /mnt/wsl/docker-desktop/cli-tools/usr/bin/docker
  ```

---

## 1. 연동 설정 (처음 한 번)
1. Windows에서 **Docker Desktop** 실행
2. 오른쪽 위 ⚙(Settings) → **Resources** → **WSL integration**
3. **Enable integration with my default WSL distro** 체크
4. 아래 목록에서 **Ubuntu-24.04** 스위치 켜기
5. **Apply & restart**

## 2. 연동 확인
```bash
docker version --format 'client {{.Client.Version}} / server {{.Server.Version}}'
docker ps
```
정상이면:
```
client 29.0.1 / server 29.0.1
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```
- `server` 버전까지 나오면 Ubuntu → Docker Desktop 엔진까지 연결된 것이다.
- `docker ps`가 오류 없이 표 머리줄을 보여 주면 된다(실행 중인 컨테이너가 없으면 머리줄만 나온다).

참고: 이 PC의 상태
```bash
wsl.exe -l -v        # Ubuntu-24.04, docker-desktop 둘 다 Running / VERSION 2
groups               # ... docker  ← sudo 없이 docker 명령 가능
```

---

## 3. 첫 도커 명령: 우분투 컨테이너 실행
```bash
docker run -it --rm ubuntu
```
| 옵션 | 뜻 |
|---|---|
| `-it` | 컨테이너 안에서 셸을 실행하고, 내 터미널의 키보드 입력·화면 출력을 거기에 연결 |
| `--rm` | 컨테이너에서 나오면(`exit`) 컨테이너를 자동 삭제 |
| `ubuntu` | 이미지 이름. 태그가 없으면 `ubuntu:latest` |

처음에는 이미지를 내려받는다(`Unable to find image 'ubuntu:latest' locally` → `Pulling ...`). 다음부터는 바로 뜬다.

### 지금 어디에 있는지: 프롬프트로 구분
| 프롬프트 | 위치 |
|---|---|
| `leo@301B-03:~/msa$` | 내 WSL Ubuntu |
| `root@435dc56d39f6:/#` | 컨테이너 안 (`435dc56d39f6` = 컨테이너 ID, `#` = root) |

터미널 창은 같지만, `docker run -it` 이후로는 입력을 컨테이너 안의 bash가 받는다.

### 컨테이너 안에서 해 볼 것
```bash
cat /etc/os-release | grep 'PRETTY_NAME'     # PRETTY_NAME="Ubuntu 26.04.1 LTS"
ls /home                                      # 비어 있음
ls ~/msa                                      # No such file or directory
java -version                                 # command not found
exit
```
내 WSL과 파일·프로그램·사용자가 전부 따로인 **격리된 환경**이다. `latest`라서 교재 시점보다 높은 버전이 나오는 건 정상이다.

## 4. 컨테이너 확인
```bash
docker ps        # 실행 중인 컨테이너만
docker ps -a     # 중지된 것까지 전부 → --rm으로 실행했으면 비어 있다
```

## 5. (선택) 페도라
교재의 "페도라를 선호한다면"은 **선택 사항**이다. 이미지 이름만 바꾸면 다른 배포판도 똑같이 뜬다는 예시다.
```bash
docker run --rm -it fedora
cat /etc/os-release | grep 'PRETTY_NAME'
exit
```
교재 그림 제목의 "CentOS"는 이전 판의 흔적이다.

## 6. 정리 명령 (필요할 때만)
```bash
docker rm -f $(docker ps -aq)      # 모든 컨테이너 강제 중지 + 삭제 (이미지는 남음)
docker rmi ubuntu fedora           # 이미지까지 삭제
```
컨테이너가 하나도 없을 때 첫 줄을 실행하면 `requires at least 1 argument`가 나온다. 지울 게 없다는 뜻이라 무시한다.

---

## Docker Desktop 화면 보는 법
`docker run -it --rm ubuntu`로 컨테이너 안에 들어가 있는 동안 Docker Desktop을 보면:
- **Containers**: 컨테이너가 하나 보인다. 이름은 Docker가 자동으로 붙인다(예: `strange_banzai`).
- **Images**: `ubuntu` 이미지가 생긴다.
- 컨테이너를 클릭하면 나오는 탭:

| 탭 | 하는 일 |
|---|---|
| **Logs** | 컨테이너 출력 보기 (읽기 전용, 입력 불가). 터미널에서 친 명령과 결과가 비쳐 보인다 |
| **Exec** | 같은 컨테이너 안에 **셸을 하나 더** 열어서 입력 |
| Inspect / Files / Stats | 설정 / 파일 / 자원 사용량 |

- Exec 탭에서 `exit`해도 컨테이너는 계속 실행된다. 컨테이너가 끝나는 건 처음 `docker run`으로 들어간 터미널에서 `exit`할 때다.
- Exec 탭과 같은 일을 터미널에서 하려면(다른 터미널 창에서):
  ```bash
  docker ps                              # 컨테이너 이름 확인
  docker exec -it strange_banzai bash    # 이름은 매번 다름
  ```
- Logs에 보이는 `←]0;root@...` 같은 깨진 글자는 터미널 제목을 바꾸는 제어 문자라 무시한다.
- 오른쪽 위 버튼: ■ 정지 / ▶ 시작 / ↻ 재시작 / 🗑 삭제

다음: [02-java-in-docker.md](02-java-in-docker.md)
