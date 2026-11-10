# 6장 트러블슈팅

## 매퍼 컴파일 오류: `has no property named ...` / `Unknown property "id"`
```
RecommendationMapper.java:14: error: The type of parameter "entity" has no property named "rating".
RecommendationMapper.java:21: error: Unknown property "id" in result type RecommendationEntity. Did you mean "null"?
```
**원인:** 엔티티의 getter/setter를 Lombok으로 만드는데, MapStruct가 Lombok보다 먼저 돌아서 getter/setter가 없는 엔티티를 봤다.
**해결:** 그 서비스의 build.gradle `dependencies`에 binding 줄이 있는지 확인한다([01](01-dependencies.md) ③).
```groovy
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
```
고친 뒤 `Shift+Alt+U`, 그리고 `./gradlew :microservices:<서비스>:clean :microservices:<서비스>:test --tests MapperTests`.

## `docker compose up` 실패: `ports are not available: exposing port TCP 0.0.0.0:3306`
```
Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:3306 -> 127.0.0.1:0: /forwards/expose returned unexpected status: 500
```
**원인:** Windows에 설치된 MySQL(`MySQL80` 서비스)이 3306을 이미 쓰고 있다. Docker Desktop은 포트를 Windows 쪽에 열기 때문에 WSL에서 `ss`로 보면 안 나온다. Windows 쪽에서 확인한다.
```bash
/mnt/c/Windows/System32/netstat.exe -ano | grep ':3306'
```
**해결:** Windows MySQL을 안 쓰면 끄고 자동 시작도 끈다. **관리자 권한 PowerShell**에서 실행한다(WSL 터미널에서는 `net`, `Set-Service`가 없다고 나온다).
```powershell
net stop MySQL80
Set-Service -Name MySQL80 -StartupType Manual
Get-Service MySQL80 | Select-Object Name,Status,StartType
```
`Stopped` / `Manual`이면 된다. 데이터는 지워지지 않고, 필요할 때 `net start MySQL80`으로 켠다(그때는 먼저 `docker compose down`). 위 `netstat` 줄이 아무것도 안 나오면 `docker compose up -d`.

**다른 방법(Windows MySQL을 계속 써야 할 때):** 아래 파일을 열어서 mysql의 왼쪽(내 PC 쪽) 포트만 바꾼다.
```bash
code docker-compose.yml
```
```yaml
    ports:
      - "3307:3306"   # ★ 변경 (전: "3306:3306")
```
오른쪽 3306은 컨테이너 안쪽 포트라 그대로 둔다. review 서비스는 도커 네트워크 안에서 `mysql:3306`으로 붙고, `docker compose exec mysql ...`도 컨테이너 안에서 실행되므로 영향이 없다.

## mongodb/mysql이 healthy가 늦거나 `unhealthy`가 됨
`docker compose ps`에서 `(health: starting)`이 오래 가거나 `(unhealthy)`가 되고, product/recommendation/review가 시작하지 않는다.
**원인:** healthcheck의 `timeout`이 교재대로 `2s`면, 검사 명령(특히 mongodb의 `mongosh`)이 시작하는 데만 2초를 넘겨 DB가 정상이어도 실패로 판정된다. 60번 연속 실패하면 `unhealthy`.
**해결:** 아래 파일을 열어서 mongodb, mysql의 healthcheck 두 곳 모두 바꾼다.
```bash
code docker-compose.yml
```
```yaml
      timeout: 10s   # ★ 변경 (교재: 2s)
```
고친 뒤 `docker compose up -d`(설정이 바뀐 DB 컨테이너만 다시 만들어진다).
