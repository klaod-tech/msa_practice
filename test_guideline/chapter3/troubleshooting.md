# 3장 트러블슈팅

## `./create-projects.bash`가 `spring: command not found`
**원인:** Spring Boot CLI가 없거나 이 터미널에 SDKMAN이 안 올라왔다.
**해결:** [SDKMAN, Java, Spring CLI 설치](../../SettingOption/setting/03-sdkman-java-spring.md). 새 터미널을 열거나 `source ~/.sdkman/bin/sdkman-init.sh`.

## `./test-em-all.bash`가 `curl: (7) Failed to connect`
**원인:** 서비스 4개가 아직 다 뜨지 않았다.
**해결:** 로그에 `Started ...Application`이 4번 나올 때까지 기다린다. 포트가 이미 쓰이고 있으면(`Port 7001 was already in use`) 예전에 띄운 java를 끈다: `pkill -f build/libs`.

## VS Code에서 빨간 줄이 남는데 빌드는 성공
**원인:** VS Code의 Java 분석 캐시가 예전 상태다.
**해결:** 명령 팔레트(Ctrl+Shift+P) → **Java: Clean Java Language Server Workspace** → Reload.
