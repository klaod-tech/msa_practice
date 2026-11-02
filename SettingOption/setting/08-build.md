# 08. 전체 빌드, 구조 확인

## 전체 빌드
```bash
./gradlew build        # BUILD SUCCESSFUL 확인
```
빌드에 성공하면 서비스마다 `build/libs/`에 jar가 두 개 생긴다.
- `*-1.0.0-SNAPSHOT.jar`: 의존성을 모두 포함한 실행용 jar (`java -jar`로 실행할 수 있음)
- `*-1.0.0-SNAPSHOT-plain.jar`: 직접 작성한 클래스만 들어 있고 의존성은 없어서 단독으로 실행되지 않음

plain jar는 나중에 `java -jar .../*.jar` 실행을 방해하므로 각 서비스의 build.gradle에 `jar { enabled = false }`를 넣어 끈다 → [../chapter3/03-product-service.md](../chapter3/03-product-service.md)

## 구조 확인
```bash
tree -I build
find microservices/product-service -type f -not -path "*/build/*"
```
`-not -path "*/build/*"`는 교재에 없다. 빌드 후라서 `build/` 폴더의 파일을 목록에서 빼기 위해 붙였다.

다음: [09-github.md](09-github.md)
