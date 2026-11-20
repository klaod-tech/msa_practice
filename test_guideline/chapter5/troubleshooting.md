# 5장 트러블슈팅

## Swagger UI가 `404`
**원인:** 주소가 다르다. 이 장은 springdoc 경로를 `/openapi/...`로 바꿨다.
**해결:** `http://localhost:8080/openapi/swagger-ui.html` (compose가 떠 있어야 한다).

## Swagger UI에 설명 대신 `${api.product-composite...}`가 그대로 보임
**원인:** composite `application.yml`의 `api:` 부분이 빠졌거나 들여쓰기가 틀렸다.
**해결:** [02](02-composite.md)의 yml 전체를 다시 맞추고 다시 빌드 → `docker compose up -d --build`.

## 4장 트러블슈팅
도커, 컴포즈 관련 오류는 [../chapter4/troubleshooting.md](../chapter4/troubleshooting.md).
