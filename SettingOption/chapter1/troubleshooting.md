# 1장 시행착오와 주의사항 (도구 설치)

## 22장: 도구 설치 (01)
| 상황 | 해결 | 문서 |
|---|---|---|
| WSL에서 Java 자동 완성이 엉뚱함 | Java 확장이 Windows에만 있고 WSL에는 없음 → WSL에 Extension Pack for Java 설치 | 01 |
| Helm을 apt 저장소(`baltocdn.com` 서명 키) 방식으로 설치하다 실패 | 받아 둔 키를 지우고(`sudo rm -f /usr/share/keyrings/helm.gpg`) 공식 스크립트 `get-helm-3`로 설치 | 01 |
| `apt install openjdk-17-jdk`로 Java 17을 먼저 설치함 | SDKMAN으로 Java 21을 설치한 뒤 apt 버전은 삭제 (아래 명령) | 01 |
| apt Java를 지운 뒤에도 `java -version`이 이상하게 나옴 | `hash -r`로 셸의 명령 경로 캐시를 비우고 `source "$HOME/.sdkman/bin/sdkman-init.sh"` 실행 | 01 |
| `21-graalce`도 함께 설치됨 | 실습에는 Temurin을 기본값으로 쓴다. 필요 없으면 `sdk uninstall java 21-graalce` | 01 |
| `downloadIstio`를 버전 지정 없이 실행하면 최신 버전이 설치됨 | `ISTIO_VERSION=1.24.2 TARGET_ARCH=x86_64`를 지정해 다시 설치 | 01 |

apt Java 17 삭제 명령:
```bash
sudo apt remove -y openjdk-17-jdk openjdk-17-jre openjdk-17-jdk-headless openjdk-17-jre-headless
hash -r
```
