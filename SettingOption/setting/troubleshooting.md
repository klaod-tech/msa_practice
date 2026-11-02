# 환경 설정 시행착오와 주의사항

## 22장: 도구 설치 (01~05)
| 상황 | 해결 | 문서 |
|---|---|---|
| WSL에서 Java 자동 완성이 엉뚱함 | Java 확장이 Windows에만 있고 WSL에는 없음 → WSL에 Extension Pack for Java 설치 | 01 |
| Helm을 apt 저장소(`baltocdn.com` 서명 키) 방식으로 설치하다 실패 | 받아 둔 키를 지우고(`sudo rm -f /usr/share/keyrings/helm.gpg`) 공식 스크립트 `get-helm-3`로 설치 | 02 |
| `apt install openjdk-17-jdk`로 Java 17을 먼저 설치함 | SDKMAN으로 Java 21을 설치한 뒤 apt 버전은 삭제 (아래 명령) | 03 |
| apt Java를 지운 뒤에도 `java -version`이 이상하게 나옴 | `hash -r`로 셸의 명령 경로 캐시를 비우고 `source "$HOME/.sdkman/bin/sdkman-init.sh"` 실행 | 03 |
| `21-graalce`도 함께 설치됨 | 실습에는 Temurin을 기본값으로 쓴다. 필요 없으면 `sdk uninstall java 21-graalce` | 03 |
| `downloadIstio`를 버전 지정 없이 실행하면 최신 버전이 설치됨 | `ISTIO_VERSION=1.24.2 TARGET_ARCH=x86_64`를 지정해 다시 설치 | 04 |

apt Java 17 삭제 명령:
```bash
sudo apt remove -y openjdk-17-jdk openjdk-17-jre openjdk-17-jdk-headless openjdk-17-jre-headless
hash -r
```

## 3장: 기본 골격 (06~09)
| 상황 | 해결 | 문서 |
|---|---|---|
| 교재 스크립트를 그대로 실행하면 | Boot 3 / Java 17로 생성되므로 쓰지 않고, 직접 작성한 스크립트를 사용 | 06 |
| `cd msa` 실패 | 홈이 아닌 위치에서 실행한 경우이므로 `cd ~/msa` 사용 | 06 |
| 서비스마다 settings.gradle이 있는데 다 고쳐야 하나? | 아니다. `include`는 루트 settings.gradle에만 넣는다 | 07 |
| 교재에 없는 `-not -path "*/build/*"` | 빌드 후라서 `build/` 폴더의 파일을 목록에서 빼기 위함 | 08 |
| GitHub 푸시 인증 실패 | 비밀번호 대신 Personal Access Token 입력 (Settings → Developer settings → Tokens (classic), `repo` 권한) | 09 |
| 토큰 저장 위치 | `~/.git-credentials` (공용 PC라면 실습 후 삭제) | 09 |

## 공통
| 상황 | 해결 |
|---|---|
| 교재 기준 버전(Boot 3 / Java 17)과 다름 | 이 실습은 Java 21 / Spring Boot 4 기준으로 진행. 교재와 달라지는 코드는 [../chapter3/README.md](../chapter3/README.md)의 요약표 참고 |
