# 2장 시행착오와 주의사항 (실습 폴더, 기본 골격)

## 3장: 기본 골격 (02)
| 상황 | 해결 | 문서 |
|---|---|---|
| 교재 스크립트를 그대로 실행하면 | Boot 3 / Java 17로 생성되므로 쓰지 않고, 직접 작성한 스크립트를 사용 | 02 |
| `cd msa` 실패 | 홈이 아닌 위치에서 실행한 경우이므로 `cd ~/msa` 사용 | 02 |
| 서비스마다 settings.gradle이 있는데 다 고쳐야 하나? | 아니다. `include`는 루트 settings.gradle에만 넣는다 | 02 |
| 교재에 없는 `-not -path "*/build/*"` | 빌드 후라서 `build/` 폴더의 파일을 목록에서 빼기 위함 | 02 |
| GitHub 푸시 인증 실패 | 비밀번호 대신 Personal Access Token 입력 (Settings → Developer settings → Tokens (classic), `repo` 권한) | 02 |
| 토큰 저장 위치 | `~/.git-credentials` (공용 PC라면 실습 후 삭제) | 02 |

## 공통
| 상황 | 해결 |
|---|---|
| 교재 기준 버전(Boot 3 / Java 17)과 다름 | 이 실습은 Java 21 / Spring Boot 4 기준으로 진행. 교재와 달라지는 코드는 [../chapter3/README.md](../chapter3/README.md)의 요약표 참고 |
