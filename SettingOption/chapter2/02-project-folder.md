# 02. 실습 폴더 만들기, VS Code로 열기, 기본 골격 (Ubuntu 24.04)

> 교재 3장 시작 부분. **시험·새로 시작할 때는 여기서부터.** 01의 도구가 설치되어 있어야 한다.
> 끝나면 `~/msa`에 서비스 4개의 뼈대가 생기고 `./gradlew build`가 성공한다. 이어서 [../chapter3/README.md](../chapter3/README.md)로 간다.
> 막히면 [troubleshooting.md](troubleshooting.md).

## 0. 어느 터미널에서 시작하나
**Ubuntu 24.04 터미널**에서 시작한다. Windows PowerShell이나 cmd가 아니다.
- 여는 법: Windows 시작 메뉴에서 **`Ubuntu 24.04`** 실행 (또는 Windows Terminal에서 `Ubuntu-24.04` 탭)
- 열리면 홈 폴더(`~` = `/home/leo`)에 있다. 프롬프트가 `leo@...:~$`처럼 `~$`로 끝나면 맞다.
- 확인:
  ```bash
  pwd            # /home/leo
  lsb_release -d # Ubuntu 24.04
  ```

**2절에서 `code .`로 VS Code를 연 다음부터는 VS Code 안의 터미널을 쓴다.**
- `` Ctrl+` `` (백틱, 숫자 1 왼쪽 키)로 열거나, 메뉴 **Terminal → New Terminal**.
- 이 터미널은 처음부터 실습 폴더(`~/msa`)에서 열리고 Ubuntu(WSL)로 동작한다. 이후 모든 장의 명령(`code 파일`, `./gradlew build`, `docker compose ...`)을 여기서 입력한다.
- 터미널 위치가 헷갈리면 언제든 `cd ~/msa`를 먼저 친다(다른 이름으로 만들었으면 그 폴더).

## 1. 환경 확인, 실습 폴더 생성
### 환경 확인
```bash
java -version          # Java 21 확인
spring --version       # Spring CLI 확인
sudo apt install -y tree
```

### 실습 폴더 생성
```bash
cd ~
mkdir msa
cd ~/msa
```
- `~/msa`가 이미 있으면(지금 이 PC처럼) 다른 이름(예: `~/msa2`)으로 만들고, 이후 모든 문서의 `~/msa`를 그 이름으로 바꿔 읽는다. 기존 `~/msa`는 건드리지 않는다.

## 2. VS Code로 열기
아직 Ubuntu 터미널에서 입력한다.
```bash
cd ~/msa
code .
```
- VS Code가 열리면 `` Ctrl+` ``로 VS Code 터미널을 열고, **이제부터는 그 터미널을 쓴다**(0절). 처음 연 Ubuntu 터미널은 닫아도 된다.
- 왼쪽 아래에 `WSL: Ubuntu-24.04`가 보이면 WSL에 연결된 상태다.
- 이후 문서의 `code 파일경로`는 이 창에서 파일을 연다. `code`로 연 탭은 `Ctrl+S`로 저장해야 파일이 생긴다.
- `.java` 파일에서 자동 완성이 이상하면 Java 확장이 WSL 쪽에 없는 것이다 → [01](../chapter1/01-ubuntu-environment.md)의 1절 "VS Code".
- 코드가 맞는데 빨간 줄이 남으면 → [../chapter3/00-background.md](../chapter3/00-background.md) 맨 위 "VS Code에 빨간 줄이 남을 때".

## 3. 서비스 4개 생성 (`create-projects.bash`)
### `create-projects.bash` 작성
```bash
code create-projects.bash
```
붙여넣기 (새 파일)

교재 스크립트는 Boot 3 / Java 17로 생성되므로 쓰지 않고, Boot 4.0.9 / Java 21로 직접 작성했다.
```bash
#!/usr/bin/env bash
mkdir -p microservices
cd microservices

create() {
  spring init \
  --boot-version=4.0.9 \
  --type=gradle-project \
  --java-version=21 \
  --packaging=jar \
  --name=$1 \
  --package-name=$2 \
  --groupId=$2 \
  --dependencies=actuator,webflux \
  --version=1.0.0-SNAPSHOT \
  $1
}

create product-service           se.magnus.microservices.core.product
create review-service            se.magnus.microservices.core.review
create recommendation-service    se.magnus.microservices.core.recommendation
create product-composite-service se.magnus.microservices.composite.product
```

### 스크립트 실행
```bash
chmod +x create-projects.bash
./create-projects.bash
ls microservices
```

## 4. 멀티 프로젝트 설정
### 루트 `settings.gradle`
```bash
code settings.gradle
```
붙여넣기 (새 파일)
```groovy
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
```

(다른 방법) 에디터 대신 터미널에서 바로 같은 파일을 만들 수도 있다.
```bash
cat > settings.gradle <<'EOF'
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
EOF
```
- heredoc 문법이다. `>`는 파일을 덮어쓴다. 기존 내용 뒤에 붙이려면 `>>`를 쓴다.
- `include`는 **루트 `settings.gradle`에만** 넣는다. 각 서비스 안의 `settings.gradle`(`rootProject.name = '...'`)은 `spring init`이 만든 것이다. 루트에서 빌드하면 Gradle이 무시하므로 그대로 둬도 된다.
- 이후 api, util 프로젝트를 추가하면 여기에 `include ':api'`, `include ':util'`도 넣는다 → [../chapter3/02-api-util.md](../chapter3/02-api-util.md)

### Gradle 래퍼를 루트로 정리
```bash
# 래퍼를 루트로 복사 (gradle은 폴더라서 -r)
cp -r microservices/product-service/gradle .
cp microservices/product-service/gradlew .
cp microservices/product-service/gradlew.bat .
cp microservices/product-service/.gitignore .

# 각 서비스에 남은 래퍼 삭제
find microservices -depth -name "gradle" -exec rm -rfv "{}" \;
find microservices -depth -name "gradlew*" -exec rm -fv "{}" \;
```
- 래퍼(`gradlew`, `gradle/wrapper/`)가 있으면 Gradle을 따로 설치하지 않아도 지정된 버전으로 빌드할 수 있다.
- 루트에서 한 번에 빌드하므로 래퍼는 루트에 하나만 있으면 된다.

## 5. 전체 빌드, 구조 확인
### 전체 빌드
```bash
./gradlew build        # BUILD SUCCESSFUL 확인
```
빌드에 성공하면 서비스마다 `build/libs/`에 jar가 두 개 생긴다.
- `*-1.0.0-SNAPSHOT.jar`: 의존성을 모두 포함한 실행용 jar (`java -jar`로 실행할 수 있음)
- `*-1.0.0-SNAPSHOT-plain.jar`: 직접 작성한 클래스만 들어 있고 의존성은 없어서 단독으로 실행되지 않음

plain jar는 나중에 `java -jar .../*.jar` 실행을 방해하므로 각 서비스의 build.gradle에 `jar { enabled = false }`를 넣어 끈다 → [../chapter3/03-product-service.md](../chapter3/03-product-service.md)

### 구조 확인
```bash
tree -I build
find microservices/product-service -type f -not -path "*/build/*"
```
`-not -path "*/build/*"`는 교재에 없다. 빌드 후라서 `build/` 폴더의 파일을 목록에서 빼기 위해 붙였다.

## 6. (선택) GitHub 업로드
> 새 폴더를 기존 레포(`msa_practice`)에 push하지 않는다. 기록이 달라서 거절되고, 강제로 밀면 기존 기록과 문서가 덮어써진다. GitHub에서 **새 레포를 먼저 만들고** 그 주소를 쓴다.

### 처음 한 번 (git 시작 + 새 레포 연결)
```bash
git init -b main
git add .
git status
git commit -m "기본 골격 마이크로서비스 생성"
git remote add origin https://github.com/<GitHub 아이디>/<새 레포 이름>.git
git config --global credential.helper store
git push -u origin main
```
- `<GitHub 아이디>/<새 레포 이름>`은 GitHub에서 만든 새 레포 주소로 바꾼다(레포 화면의 `Code` 버튼에 나오는 HTTPS 주소).
- 푸시할 때 비밀번호 대신 **Personal Access Token**을 입력한다 → [troubleshooting.md](troubleshooting.md)
- `credential.helper store`로 토큰이 `~/.git-credentials`에 저장된다. 다음부터는 입력하지 않아도 된다.

### 이후 커밋·푸시 (3장부터 매번)
처음 연결을 한 뒤로는 세 줄이면 된다. `~/msa`(VS Code 터미널)에서:
```bash
git add .
git commit -m "무엇을 했는지 한 줄"
git push
```
- `git add .`: 바뀐 파일을 모두 커밋 대상에 올린다.
- `git commit -m "..."`: 지금 상태를 기록한다. 메시지는 한 일을 짧게 적는다(예: `"product-service 구현"`, `"오류 처리 추가"`).
- `git push`: GitHub에 올린다. 처음에 `-u origin main`을 해 두었으므로 뒤에 아무것도 안 붙여도 된다.
- 올리기 전에 무엇이 바뀌었는지 보려면 `git status`.

---
다음: [../chapter3/README.md](../chapter3/README.md) (3장: API 구현부터)
