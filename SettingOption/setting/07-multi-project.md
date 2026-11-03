# 07. 멀티 프로젝트 설정

## 루트 `settings.gradle`
`code settings.gradle` 들어가서 아래 4줄 입력 (또는 아래 heredoc 명령을 터미널에 그대로 입력해도 같은 파일이 만들어진다)
```bash
cat > settings.gradle <<'EOF'
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'
EOF
```
- heredoc 문법으로 터미널에서 바로 파일을 만든다. 에디터로 직접 써도 결과는 같다.
- `>`는 파일을 덮어쓴다. 기존 내용 뒤에 붙이려면 `>>`를 쓴다.
- `include`는 **루트 `settings.gradle`에만** 넣는다. 각 서비스 안의 `settings.gradle`(`rootProject.name = '...'`)은 `spring init`이 만든 것이다. 루트에서 빌드하면 Gradle이 무시하므로 그대로 둬도 된다.
- 이후 api, util 프로젝트를 추가하면 여기에 `include ':api'`, `include ':util'`도 넣는다 → [../chapter3/02-api-util.md](../chapter3/02-api-util.md)

## Gradle 래퍼를 루트로 정리
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

다음: [08-build.md](08-build.md)
