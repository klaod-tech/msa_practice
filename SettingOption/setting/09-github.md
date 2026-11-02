# 09. GitHub 업로드

```bash
git init -b main
git add .
git status
git commit -m "Chapter03: 기본 골격 마이크로서비스 생성"
git remote add origin https://github.com/klaod-tech/msa_practice.git
git config --global credential.helper store
git push -u origin main
```
- 커밋 메시지는 `ChapterNN: 설명` 형식으로 쓴다.
- 푸시할 때 비밀번호 대신 **Personal Access Token**을 입력한다 → [troubleshooting.md](troubleshooting.md)
- `credential.helper store`로 토큰이 `~/.git-credentials`에 저장된다. 다음부터는 입력하지 않아도 된다.

다음 단계(API 구현): [../chapter3/README.md](../chapter3/README.md)
