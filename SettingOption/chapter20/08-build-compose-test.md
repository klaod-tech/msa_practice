# 08. test-em-all, 실행

> 시작: 19장까지 따라 한 상태 / 결과: 20장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

`test-em-all.bash`: 쿠버네티스에서 `https://health.minikube.me/actuator/prometheus`가 200인지 확인한다.

## 8-1. `test-em-all.bash`
```bash
code test-em-all.bash
```
아래처럼 맞추기 (전체 파일)
```bash
#!/usr/bin/env bash
: ${HOST=minikube.me}
: ${PORT=443}
: ${USE_K8S=true}
: ${HEALTH_URL=https://health.minikube.me}
: ${MGM_PORT=4004}
: ${PROD_ID_REVS_RECS=1}
: ${PROD_ID_NOT_FOUND=13}
: ${PROD_ID_NO_RECS=113}
: ${PROD_ID_NO_REVS=213}
: ${SKIP_CB_TESTS=false}
: ${NAMESPACE=hands-on}

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

function testCompositeCreated() {

    if ! assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
    then
        echo -n "FAIL"
        return 1
    fi

    set +e
    assertEqual "$PROD_ID_REVS_RECS" $(echo $RESPONSE | jq .productId)
    if [ "$?" -eq "1" ] ; then return 1; fi

    assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
    if [ "$?" -eq "1" ] ; then return 1; fi

    assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")
    if [ "$?" -eq "1" ] ; then return 1; fi

    if ! assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
    then
        echo -n "FAIL"
        return 1
    fi
    if ! assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
    then
        echo -n "FAIL"
        return 1
    fi
    set -e
}

function waitForMessageProcessing() {
    echo "Wait for messages to be processed... "

    sleep 1

    n=0
    until (testCompositeCreated)
    do
        n=$((n + 1))
        if [[ $n == 40 ]]
        then
            echo " Give up"
            exit 1
        else
            sleep 6
            echo -n ", retry #$n "
        fi
    done
    echo "All messages are now processed!"
}

function recreateComposite() {
  local productId=$1
  local composite=$2

  assertCurl 202 "curl -X DELETE $AUTH -k https://$HOST:$PORT/product-composite/${productId} -s"
  assertEqual 202 $(curl -X POST -s -k https://$HOST:$PORT/product-composite -H "Content-Type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" --data "$composite" -w "%{http_code}")
}

function setupTestdata() {

  body="{\"productId\":$PROD_ID_NO_RECS"
  body+=\
',"name":"product name A","weight":100, "reviews":[
  {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
  {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
  {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
]}'
  recreateComposite "$PROD_ID_NO_RECS" "$body"

  body="{\"productId\":$PROD_ID_NO_REVS"
  body+=\
',"name":"product name B","weight":200, "recommendations":[
  {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
  {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
  {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
]}'
  recreateComposite "$PROD_ID_NO_REVS" "$body"

  body="{\"productId\":$PROD_ID_REVS_RECS"
  body+=\
',"name":"product name C","weight":300, "recommendations":[
      {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
      {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
      {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
  ], "reviews":[
      {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
      {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
      {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
  ]}'
  recreateComposite "$PROD_ID_REVS_RECS" "$body"

}

function testCircuitBreaker() {

    echo "Start Circuit Breaker tests!"

    if [[ $USE_K8S == "false" ]]
    then
        EXEC="docker compose exec -T product-composite"
    else
        EXEC="kubectl -n $NAMESPACE exec deploy/product-composite -c product-composite -- "
    fi

    assertEqual "CLOSED" "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    for ((n=0; n<3; n++))
    do
        assertCurl 500 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
        message=$(echo $RESPONSE | jq -r .message)
        assertEqual "Did not observe any item or terminal signal within 2000ms" "${message:0:57}"
    done

    assertEqual "OPEN" "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS?delay=3 $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
    assertEqual "Fallback product$PROD_ID_REVS_RECS" "$(echo "$RESPONSE" | jq -r .name)"

    assertCurl 404 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND $AUTH -s"
    assertEqual "Product Id: $PROD_ID_NOT_FOUND not found in fallback cache!" "$(echo $RESPONSE | jq -r .message)"

    echo "Will sleep for 10 sec waiting for the CB to go Half Open..."
    sleep 10

    assertEqual "HALF_OPEN" "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    for ((n=0; n<3; n++))
    do
        assertCurl 200 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS $AUTH -s"
        assertEqual "product name C" "$(echo "$RESPONSE" | jq -r .name)"
    done

    assertEqual "CLOSED" "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/health | jq -r .components.circuitBreakers.details.product.details.state)"

    assertEqual "CLOSED_TO_OPEN"      "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-3].stateTransition)"
    assertEqual "OPEN_TO_HALF_OPEN"   "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-2].stateTransition)"
    assertEqual "HALF_OPEN_TO_CLOSED" "$($EXEC wget -qO - http://localhost:${MGM_PORT}/actuator/circuitbreakerevents/product/STATE_TRANSITION | jq -r .circuitBreakerEvents[-1].stateTransition)"
}

set -e

echo "Start Tests:" `date`

echo "HOST=${HOST}"
echo "PORT=${PORT}"
echo "USE_K8S=${USE_K8S}"
echo "HEALTH_URL=${HEALTH_URL}"
echo "MGM_PORT=${MGM_PORT}"
echo "SKIP_CB_TESTS=${SKIP_CB_TESTS}"

if [[ $@ == *"start"* ]]
then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  docker compose down --remove-orphans
  echo "$ docker compose up -d"
  docker compose up -d
fi

waitForService curl -k $HEALTH_URL/actuator/health

ACCESS_TOKEN=$(curl -k https://writer:secret-writer@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq .access_token -r)
echo ACCESS_TOKEN=$ACCESS_TOKEN
AUTH="-H \"Authorization: Bearer $ACCESS_TOKEN\""

setupTestdata

waitForMessageProcessing

assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 404 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 200 "curl $AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

assertCurl 422 "curl $AUTH -k https://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

assertCurl 400 "curl $AUTH -k https://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

assertCurl 401 "curl -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

READER_ACCESS_TOKEN=$(curl -k https://reader:secret-reader@$HOST:$PORT/oauth2/token -d grant_type=client_credentials -d scope="product:read" -s | jq .access_token -r)
echo READER_ACCESS_TOKEN=$READER_ACCESS_TOKEN
READER_AUTH="-H \"Authorization: Bearer $READER_ACCESS_TOKEN\""

assertCurl 200 "curl $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertCurl 403 "curl -X DELETE $READER_AUTH -k https://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"

echo "Swagger/OpenAPI tests"
assertCurl 302 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ksL https://$HOST:$PORT/openapi/swagger-ui.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/index.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/swagger-ui/oauth2-redirect.html"
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs"
assertEqual "3.1.0" "$(echo $RESPONSE | jq -r .openapi)"
if [[ $USE_K8S == "false" ]]
then
  assertEqual "https://$HOST:$PORT" "$(echo $RESPONSE | jq -r '.servers[0].url')"
fi
assertCurl 200 "curl -ks  https://$HOST:$PORT/openapi/v3/api-docs.yaml"

# ★ 추가 시작 (5줄)
if [[ $USE_K8S == "true" ]]
then
  echo "Prometheus metrics tests"
  assertCurl 200 "curl -ks https://health.minikube.me/actuator/prometheus"
fi
# ★ 추가 끝

if [[ $SKIP_CB_TESTS == "false" ]]
then
    testCircuitBreaker
fi

if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi

echo "End, all tests OK:" `date`
```

## 확인 1: 도커 컴포즈 (18장과 같음)
```bash
cd ~/msa
./gradlew build
docker compose build
HOST=localhost PORT=8443 HEALTH_URL=https://localhost:8443 USE_K8S=false ./test-em-all.bash start stop
```
마지막 줄 `End, all tests OK`.

## 확인 2: 18장 상태의 클러스터에 이 장의 차트 설치
18장의 확인 2~4(minikube 12GB, cert-manager, 이스티오와 관찰 도구)가 된 상태에서 시작한다.
```bash
eval $(minikube docker-env)
docker compose build
for f in kubernetes/helm/components/*; do helm dep up $f; done
for f in kubernetes/helm/environments/*; do helm dep up $f; done
helm upgrade --install istio-hands-on-addons kubernetes/helm/environments/istio-system -n istio-system --wait
helm upgrade --install hands-on-dev-env kubernetes/helm/environments/dev-env -n hands-on
kubectl wait --timeout=900s --for=condition=ready pod --all -n hands-on
kubectl -n istio-system create deployment mail-server --image maildev/maildev:2.0.5
kubectl -n istio-system expose deployment mail-server --port=1080,1025 --type=ClusterIP
kubectl -n istio-system wait --timeout=300s --for=condition=available deployment/mail-server
```
- istio-system 차트가 이 장에서 바뀌었다(`expose-mail.yml`: 메일 서버 화면을 `mail.minikube.me`로 노출). 그래서 `upgrade`한다.
- 메일 서버(maildev)는 Grafana 알림 메일을 받아 보는 시험용이다. 교재처럼 직접 만든다(1080: 웹 화면, 1025: 메일 받기).

## 확인 3: 테스트 (교수님 안내와 다름: WSL, 443 포트)
이 장의 test-em-all에는 `https://health.minikube.me/actuator/prometheus`처럼 **443 포트를 고정**한 줄이 있다. 그래서 이스티오 입구를 내 PC의 **443** 포트로 연결한다. 1024보다 낮은 포트라 [1장 4-5절](../chapter1/01-ubuntu-environment.md) 준비 6번(`ip_unprivileged_port_start=443`)을 먼저 해 둔다.
```bash
kubectl port-forward -n istio-system service/istio-ingressgateway 443:443
```
`Forwarding from 127.0.0.1:443 -> 8443`이 나오고 멈춰 있으면 된다. **새 터미널**에서 교수님 안내 그대로:
```bash
cd ~/msa
./test-em-all.bash
```
기본값(`HOST=minikube.me`, `PORT=443`, `HEALTH_URL=https://health.minikube.me`)을 그대로 쓴다. 중간에 `Prometheus metrics tests`가 나오고 마지막 줄 `End, all tests OK`.

## 확인 4: 지표 보기
```bash
curl -sk https://health.minikube.me/actuator/prometheus | grep -m3 "^jvm_memory_used_bytes"
curl -sk -o /dev/null -w "%{http_code}\n" https://grafana.minikube.me/
curl -sk -o /dev/null -w "%{http_code}\n" https://mail.minikube.me/
```
- 첫째: composite의 JVM 메모리 지표(`application="product-composite"` 태그가 붙어 있다).
- 둘째, 셋째: Grafana, 메일 서버 화면 `200`. 브라우저로 보려면 https://grafana.minikube.me (Windows 브라우저는 18장 troubleshooting의 Windows hosts 설정).
- Grafana에서 대시보드 가져오기: Dashboards → New → Import → `kubernetes/grafana/api-export-import/Hands-on-Dashboard.json` 업로드(교수님 안내: Grafana 9에서 만든 것이라 일부 패널이 다르게 보일 수 있다).

## 끝내기
```bash
helm uninstall hands-on-dev-env -n hands-on
kubectl -n istio-system delete deployment,service mail-server
eval $(minikube docker-env -u)
minikube stop
```
