# 7장 트러블슈팅

## 받은 이벤트가 전부 null / `NullPointerException` in `MessageProcessorConfig`
핵심 서비스 로그에 `Process message created at null...` 뒤로 `NullPointerException`(switch 줄)이 나고 재시도 후 DLQ로 간다.
**원인:** `Event`의 필드가 `final`이라 Jackson 3이 값을 못 넣었다. Spring Cloud Stream 5는 Jackson 3으로 메시지를 읽는다.
**해결:** 그 서비스의 application.yml(기본 프로필 부분)에 있는지 확인한다([05](05-core-event-consumers.md) 4절).
```yaml
spring.jackson.mapper.allow-final-fields-as-mutators: true
```

## composite가 안 뜸: `No qualifying bean of type '...WebClient$Builder'`
**원인:** Boot 4에서 WebClient 자동 설정이 별도 모듈로 분리됐다.
**해결:** composite build.gradle에 한 줄([01](01-dependencies.md) ②).
```groovy
	implementation 'org.springframework.boot:spring-boot-starter-webclient'
```
고친 뒤 `Shift+Alt+U`.

## `./gradlew :util:test` 실패: `Failed to load JUnit Platform`
**원인:** util build.gradle에 JUnit 런처가 없다(지금까지 util에 테스트가 없었음).
**해결:** util build.gradle `dependencies`에 한 줄([01](01-dependencies.md) ④).
```groovy
    testImplementation 'org.junit.platform:junit-platform-launcher'
```
