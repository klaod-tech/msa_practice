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
