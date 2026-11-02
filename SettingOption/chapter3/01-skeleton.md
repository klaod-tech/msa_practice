# 01. 기본 골격 마이크로서비스 생성

자세한 명령은 [../setting/README.md](../setting/README.md)의 06~09에 있다. 요약하면 다음과 같다.

1. `create-projects.bash`로 `spring init`을 실행해 서비스 4개를 만든다. 교재 스크립트는 Boot 3 / Java 17로 생성되므로, Boot 4.0.9 / Java 21로 직접 작성한 스크립트를 썼다.
2. 루트 `settings.gradle`에 서비스 4개를 `include`한다. 각 서비스 안의 `settings.gradle`은 루트에서 빌드할 때 무시되므로 그대로 둔다.
3. Gradle 래퍼(`gradlew`, `gradle/`)를 루트로 복사하고, 각 서비스에 남은 래퍼는 삭제한다.
4. `./gradlew build`로 빌드한다.
5. 커밋: `Chapter03: 기본 골격 마이크로서비스 생성`

다음: [02-api-util.md](02-api-util.md)
