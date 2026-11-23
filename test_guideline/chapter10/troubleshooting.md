# 10장 트러블슈팅

## 헤더 라우팅(`/headerrouting`)이 응답 없음 / 타임아웃
**원인:** 교수님 코드(교재)의 대상 `httpstat.us`가 지금 응답하지 않는다. 코드 문제는 아니다. test-em-all은 이 라우트를 쓰지 않아서 통과한다.
**직접 동작을 보고 싶을 때 (선택, 교수님 코드와 다름):** 같은 일을 하는 `httpbin` 컨테이너로 바꾼다. 확인이 끝나면 되돌려도 된다.
```bash
code spring-cloud/gateway/src/main/resources/application.yml
```
라우트 3개(`host_route_200`, `host_route_418`, `host_route_501`)에서:
```yaml
  uri: http://httpbin:8080   # 교수님 코드와 다름 (교수님: http://httpstat.us)
  ...
  - SetPath=/status/200      # 교수님 코드와 다름 (교수님: /200). 418, 501도 같은 식
```
```bash
code docker-compose.yml
```
맨 아래에 서비스 하나 추가:
```yaml
  httpbin:
    image: mccutchen/go-httpbin:2.25.0
    mem_limit: 128m
```
그다음 `./gradlew :spring-cloud:gateway:build && docker compose up -d --build`, 확인:
```bash
curl http://localhost:8080/headerrouting -H "Host: i.feel.lucky:8080" -s -o /dev/null -w "%{http_code}\n"
curl http://localhost:8080/headerrouting -H "Host: im.a.teapot:8080" -s -o /dev/null -w "%{http_code}\n"
curl http://localhost:8080/headerrouting -s -o /dev/null -w "%{http_code}\n"
```
`200`, `418`, `501`.

## 로컬에서 gateway만 띄우면 health가 DOWN
**원인:** 게이트웨이 헬스 체크는 Eureka로 composite와 핵심 서비스를 찾아 상태를 묻는다. 그 서비스들이 없으면 DOWN이 정상이다.
**해결:** compose로 전체를 띄워서 본다.

## 9장 트러블슈팅
Eureka 관련은 [../chapter9/troubleshooting.md](../chapter9/troubleshooting.md).
