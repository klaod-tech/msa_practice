# 03. docker-compose.yml, test-em-all.bash, 실행

> 시작: 교수님 `Chapter03/2-basic-rest-services` 상태 / 결과: 교수님 `Chapter04`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

- `docker-compose.yml` (새 파일): 서비스 4개를 정의한다. `build`는 Dockerfile 위치, `mem_limit: 512m`은 메모리 상한(자바가 이 값을 보고 힙 크기를 정한다), composite만 `ports: "8080:8080"`으로 밖에 연다.
- `test-em-all.bash`: 기본 포트가 8080으로 바뀌고, `start`를 주면 컴포즈를 새로 띄우고 서비스가 응답할 때까지 기다린 뒤(`waitForService`) 테스트하고, `stop`을 주면 끝나고 내린다.

## 3-1. `docker-compose.yml` (새 파일)
```bash
code docker-compose.yml
```
붙여넣기 (새 파일)
```yaml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  recommendation:
    build: microservices/recommendation-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  review:
    build: microservices/review-service
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  product-composite:
    build: microservices/product-composite-service
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

## 3-2. `test-em-all.bash`
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
: ${HOST=localhost}
: ${PORT=8080}  # ★ 변경 (전: : ${PORT=7001})
: ${PROD_ID_REVS_RECS=1}
: ${PROD_ID_NOT_FOUND=13}
: ${PROD_ID_NO_RECS=113}
: ${PROD_ID_NO_REVS=213}

function assertCurl() {

  local expectedHttpCode=$1
  local curlCmd="$2 -w \"%{http_code}\""
  local result=$(eval $curlCmd)
  local httpCode="${result:(-3)}"
  RESPONSE='' && (( ${#result} > 3 )) && RESPONSE="${result%???}"

  if [ "$httpCode" = "$expectedHttpCode" ]
  then
    if [ "$httpCode" = "200" ]
    then
      echo "Test OK (HTTP Code: $httpCode)"
    else
      echo "Test OK (HTTP Code: $httpCode, $RESPONSE)"
    fi
  else
    echo  "Test FAILED, EXPECTED HTTP Code: $expectedHttpCode, GOT: $httpCode, WILL ABORT!"
    echo  "- Failing command: $curlCmd"
    echo  "- Response Body: $RESPONSE"
    exit 1
  fi
}

function assertEqual() {

  local expected=$1
  local actual=$2

  if [ "$actual" = "$expected" ]
  then
    echo "Test OK (actual value: $actual)"
  else
    echo "Test FAILED, EXPECTED VALUE: $expected, ACTUAL VALUE: $actual, WILL ABORT"
    exit 1
  fi
}

# ★ 추가 (27줄)
function testUrl() {
  url=$@
  if $url -ks -f -o /dev/null
  then
    return 0
  else
    return 1
  fi;
}

function waitForService() {
  url=$@
  echo -n "Wait for: $url... "
  n=0
  until testUrl $url
  do
    n=$((n + 1))
    if [[ $n == 100 ]]
    then
      echo " Give up"
      exit 1
    else
      sleep 3
      echo -n ", retry #$n "
    fi
  done
  echo "DONE, continues..."
}

set -e

echo "Start Tests:" `date`  # ★ 추가

echo "HOST=${HOST}"
echo "PORT=${PORT}"
# ★ 추가 (9줄)

if [[ $@ == *"start"* ]]
then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  docker compose down --remove-orphans
  echo "$ docker compose up -d"
  docker compose up -d
fi

waitForService curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS

assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 404 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 422 "curl http://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

assertCurl 400 "curl http://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

# ★ 추가 (6줄)
if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi

echo "End, all tests OK:" `date`
```

## 확인: 빌드, 이미지, 컴포즈로 실행, 자동 테스트
```bash
cd ~/msa
./gradlew build
docker compose build
./test-em-all.bash start stop
```
- `./gradlew build`: `BUILD SUCCESSFUL`. 이미지는 `build/libs`의 jar로 만들기 때문에 **먼저** 빌드해야 한다.
- `docker compose build`: 이미지 4개(`msa-product` 등, 앞부분은 작업 폴더 이름).
- `./test-em-all.bash start stop`: 컴포즈를 띄우고(`Restarting the test environment...`) 응답할 때까지 기다렸다가(`Wait for: ...`) 테스트한 뒤 내린다. 마지막 줄 `End, all tests OK`.

직접 띄워서 불러 보기:
```bash
docker compose up -d
curl -s localhost:8080/product-composite/1 | jq .serviceAddresses
docker compose down
```
`serviceAddresses`에 컨테이너 이름과 8080 포트가 보이면 컨테이너 안에서 동작하는 것이다. 응답이 없으면 아직 뜨는 중이니 몇 초 뒤 다시 부른다.
