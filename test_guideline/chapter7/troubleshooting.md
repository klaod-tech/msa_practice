# 7장 트러블슈팅

## test-em-all이 생성 직후 조회에서 실패 (`404`, 추천·리뷰 0개)
**원인:** 생성은 이벤트로 처리돼서 composite가 202를 돌려준 뒤에 핵심 서비스가 저장한다. 아직 처리되기 전에 조회했다.
**해결:** 교수님 test-em-all은 `waitForMessageProcessing`으로 기다린다. 직접 curl로 해 볼 때는 POST 뒤 1~2초 기다렸다 조회한다.

## RabbitMQ 관리 화면을 보고 싶다
`docker compose up -d` 상태에서 브라우저로 http://localhost:15672 (아이디 `guest`, 비밀번호 `guest`). **Exchanges**에 `products`, `recommendations`, `reviews`, **Queues**에 `products.productsGroup` 등이 보인다.

## Kafka/파티션 compose로 바꿔 실행
```bash
docker compose down
export COMPOSE_FILE=docker-compose-partitions.yml   # 또는 docker-compose-kafka.yml
./test-em-all.bash start stop
unset COMPOSE_FILE
```
`COMPOSE_FILE`을 풀어 두지 않으면 다음 `docker compose` 명령도 그 파일을 쓴다.

## 6장 트러블슈팅
DB, 포트, healthcheck 관련은 [../chapter6/troubleshooting.md](../chapter6/troubleshooting.md).
