# 12장 트러블슈팅

## build.gradle: `Could not find org.springframework.boot:spring-boot-starter-aop:.`
**원인:** Boot 4에서 `spring-boot-starter-aop`이 `spring-boot-starter-aspectj`로 이름이 바뀌었다. 예전 이름은 Boot 4 BOM에 없어서 버전을 못 찾는다.
**해결:** [03](03-config-clients.md)처럼 바꾼다.
```groovy
	implementation 'org.springframework.boot:spring-boot-starter-aspectj'
```

## composite `MessagingTests` 2개 실패: `"productId":0`, `"name":null`
**원인:** 7장부터 쓰던 Jackson 3 설정(`spring.jackson.mapper.allow-final-fields-as-mutators: true`)이 composite의 `application.yml`에 있었다. 12장에서 그 파일을 지우면 테스트에서도 빠진다. 그러면 이벤트 JSON을 객체로 바꿀 때 값이 비어 버린다.
**해결:** 테스트용 `src/test/resources/application.yml` 맨 위에 그 줄을 넣는다([03](03-config-clients.md) 4절).

## 테스트가 `localhost:8888`에 접속하려다 실패 (`Could not locate PropertySource`, `Connection refused`)
**원인:** 테스트에는 설정 서버가 없는데 클라이언트가 켜져 있다. `failFast: true`라서 설정을 못 받으면 앱이 아예 안 뜬다.
**해결:** 해당 테스트의 `@SpringBootTest` / `@DataMongoTest` / `@DataJpaTest`에 `spring.cloud.config.enabled=false`가 있는지 확인한다([03](03-config-clients.md) 3절).

## 테스트 실패: `ConfigServicePropertySourceLocator` ... `HttpClientErrorException$Unauthorized`
**원인:** 위와 같이 테스트에 `spring.cloud.config.enabled=false`가 빠졌는데, 마침 [02](02-config-repo.md) 끝에서 로컬로 띄운 설정 서버가 8888에 켜져 있었다. 테스트가 그 설정 서버에 계정 없이 접속해서 401을 받는다(설정 서버가 꺼져 있으면 `Connection refused`).
**해결:** 테스트 파일을 [03](03-config-clients.md)대로 고치고, 설정 서버를 띄운 터미널에서 `Ctrl+C`. 아래 명령에 아무것도 안 나오면 꺼진 것이다.
```bash
ss -ltn | grep 8888
```

## compose: 서비스가 몇 분 뒤 종료됨 (`Could not locate PropertySource` + `config-server`)
**원인:** retry를 20번 다 해도 설정 서버에서 설정을 못 받았다.
**확인:**
```bash
docker compose logs config-server | tail -20
```
- 설정 서버가 아예 안 떴으면 위 로그에 오류가 있다.
- 로그에 `Serving configurations from folder: file:/config-repo`가 있는지 본다. `~/msa`가 아닌 곳에서 `docker compose up`을 하면 `$PWD/config-repo`가 엉뚱한 폴더(빈 폴더)로 붙는다. **`cd ~/msa` 후 실행**한다.

## 서비스 로그에 `401` (설정 서버 접근 거부)
**원인:** 서비스가 보낸 계정(`CONFIG_SERVER_USR/PWD`)과 설정 서버 계정(`SPRING_SECURITY_USER_NAME/PASSWORD`)이 다르거나 비어 있다.
**해결:** `~/msa/.env`가 있는지, compose 파일의 각 서비스에 `CONFIG_SERVER_USR`, `CONFIG_SERVER_PWD` 두 줄이 있는지 확인한다([04](04-docker-compose-and-test.md)).
```bash
cat ~/msa/.env
```

## 암호문이 안 풀림 (설정에 `invalid.app.eureka-password: <n/a>`, 설정 서버 로그에 `Cannot decrypt key`)
**원인:** 설정 서버의 `ENCRYPT_KEY`가 없거나 암호문을 만들 때와 다른 키다.
**해결:** `.env`의 `CONFIG_SERVER_ENCRYPT_KEY`가 `my-very-secure-encrypt-key`(교재 암호문을 만든 키)인지 확인한다. 키를 바꿨다면 새 키로 [02](02-config-repo.md)의 암호문을 다시 만들어야 한다(`/config/encrypt`).
