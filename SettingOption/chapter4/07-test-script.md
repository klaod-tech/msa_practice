# 07. 테스트 스크립트 자동화 (test-em-all.bash)

> 표기: `★ 추가` = 새로 넣는 줄, `★ 변경` = 바꾸는 줄 (괄호 안은 원래 값), 표시 없는 줄 = 이미 있는 줄(위치 확인용)
> 모든 명령은 `~/msa`에서 실행한다.

## 무엇이 달라지나
3장의 test-em-all.bash는 **이미 떠 있는 서비스**에 curl을 보내기만 했다. 4장 버전은 여기에 세 가지를 더한다.
1. 포트를 컴포즈의 composite 포트(8080)로 바꾼다.
2. `start`를 붙여 실행하면 **도커 컴포즈 환경을 내렸다가 새로 띄운다.**
3. 서비스가 응답할 때까지 **기다렸다가** 테스트를 시작하고, `stop`을 붙이면 끝나고 환경을 내린다.

## 작업 방법 (둘 중 하나)
| 방법 | 할 일 |
|---|---|
| **A. 전체 붙여 넣기 (권장)** | 아래 "완성 파일"을 통째로 붙여 넣는다. **수정할 곳 없음** (교재 4장 내용 그대로, Boot 4 때문에 바꿀 곳이 없다) |
| B. 지금 파일 고치기 | 지금 파일(3장 버전)에서 ①~④ **4군데**만 수정 |

"4군데"는 **3장 버전 대비** 바뀐 곳이다. 교재 4장 내용을 그대로 붙여 넣으면 고칠 곳은 없다.

---

## A. 완성 파일 (전체 붙여 넣기)
`code test-em-all.bash` 들어가서 기존 내용을 모두 지우고 아래 내용 전체 입력
```bash
#!/usr/bin/env bash
#
# Sample usage:
#
#   HOST=localhost PORT=7001 ./test-em-all.bash
#
: ${HOST=localhost}
: ${PORT=8080}
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

echo "Start Tests:" `date`

echo "HOST=${HOST}"
echo "PORT=${PORT}"

if [[ $@ == *"start"* ]]
then
  echo "Restarting the test environment..."
  echo "$ docker compose down --remove-orphans"
  docker compose down --remove-orphans
  echo "$ docker compose up -d"
  docker compose up -d
fi

waitForService curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS

# Verify that a normal request works, expect three recommendations and three reviews
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 404 (Not Found) error is returned for a non-existing productId ($PROD_ID_NOT_FOUND)
assertCurl 404 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

# Verify that no recommendations are returned for productId $PROD_ID_NO_RECS
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that no reviews are returned for productId $PROD_ID_NO_REVS
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 422 (Unprocessable Entity) error is returned for a productId that is out of range (-1)
assertCurl 422 "curl http://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

# Verify that a 400 (Bad Request) error error is returned for a productId that is not a number, i.e. invalid format
assertCurl 400 "curl http://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

if [[ $@ == *"stop"* ]]
then
    echo "We are done, stopping the test environment..."
    echo "$ docker compose down"
    docker compose down
fi

echo "End, all tests OK:" `date`
```

---

## B. 3장 버전에서 바뀐 곳 (한눈에 보기)
`code test-em-all.bash` 들어가서 `★` 표시한 줄만 고친다 (A로 했으면 읽기만 하면 된다).
표시 없는 줄은 3장 버전과 같다. `#` 뒤는 bash 주석이라 이대로 붙여 넣어도 동작한다.
```bash
#!/usr/bin/env bash
#
# Sample usage:
#
#   HOST=localhost PORT=7001 ./test-em-all.bash
#
: ${HOST=localhost}
: ${PORT=8080}                                      # ★ ① 변경 (3장: 7001)
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

function testUrl() {                                # ★ ② 추가
  url=$@                                            # ★ ② 추가
  if $url -ks -f -o /dev/null                       # ★ ② 추가
  then                                              # ★ ② 추가
    return 0                                        # ★ ② 추가
  else                                              # ★ ② 추가
    return 1                                        # ★ ② 추가
  fi;                                               # ★ ② 추가
}                                                   # ★ ② 추가

function waitForService() {                         # ★ ② 추가
  url=$@                                            # ★ ② 추가
  echo -n "Wait for: $url... "                      # ★ ② 추가
  n=0                                               # ★ ② 추가
  until testUrl $url                                # ★ ② 추가
  do                                                # ★ ② 추가
    n=$((n + 1))                                    # ★ ② 추가
    if [[ $n == 100 ]]                              # ★ ② 추가
    then                                            # ★ ② 추가
      echo " Give up"                               # ★ ② 추가
      exit 1                                        # ★ ② 추가
    else                                            # ★ ② 추가
      sleep 3                                       # ★ ② 추가
      echo -n ", retry #$n "                        # ★ ② 추가
    fi                                              # ★ ② 추가
  done                                              # ★ ② 추가
  echo "DONE, continues..."                         # ★ ② 추가
}                                                   # ★ ② 추가

set -e

echo "Start Tests:" `date`                          # ★ ③ 추가

echo "HOST=${HOST}"
echo "PORT=${PORT}"

if [[ $@ == *"start"* ]]                            # ★ ③ 추가
then                                                # ★ ③ 추가
  echo "Restarting the test environment..."         # ★ ③ 추가
  echo "$ docker compose down --remove-orphans"     # ★ ③ 추가
  docker compose down --remove-orphans              # ★ ③ 추가
  echo "$ docker compose up -d"                     # ★ ③ 추가
  docker compose up -d                              # ★ ③ 추가
fi                                                  # ★ ③ 추가

waitForService curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS# ★ ③ 추가

# Verify that a normal request works, expect three recommendations and three reviews
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_REVS_RECS -s"
assertEqual $PROD_ID_REVS_RECS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 404 (Not Found) error is returned for a non-existing productId ($PROD_ID_NOT_FOUND)
assertCurl 404 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NOT_FOUND -s"
assertEqual "No product found for productId: $PROD_ID_NOT_FOUND" "$(echo $RESPONSE | jq -r .message)"

# Verify that no recommendations are returned for productId $PROD_ID_NO_RECS
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_RECS -s"
assertEqual $PROD_ID_NO_RECS $(echo $RESPONSE | jq .productId)
assertEqual 0 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 3 $(echo $RESPONSE | jq ".reviews | length")

# Verify that no reviews are returned for productId $PROD_ID_NO_REVS
assertCurl 200 "curl http://$HOST:$PORT/product-composite/$PROD_ID_NO_REVS -s"
assertEqual $PROD_ID_NO_REVS $(echo $RESPONSE | jq .productId)
assertEqual 3 $(echo $RESPONSE | jq ".recommendations | length")
assertEqual 0 $(echo $RESPONSE | jq ".reviews | length")

# Verify that a 422 (Unprocessable Entity) error is returned for a productId that is out of range (-1)
assertCurl 422 "curl http://$HOST:$PORT/product-composite/-1 -s"
assertEqual "\"Invalid productId: -1\"" "$(echo $RESPONSE | jq .message)"

# Verify that a 400 (Bad Request) error error is returned for a productId that is not a number, i.e. invalid format
assertCurl 400 "curl http://$HOST:$PORT/product-composite/invalidProductId -s"
assertEqual "\"Type mismatch.\"" "$(echo $RESPONSE | jq .message)"

if [[ $@ == *"stop"* ]]                             # ★ ④ 추가
then                                                # ★ ④ 추가
    echo "We are done, stopping the test environment..."# ★ ④ 추가
    echo "$ docker compose down"                    # ★ ④ 추가
    docker compose down                             # ★ ④ 추가
fi                                                  # ★ ④ 추가

echo "End, all tests OK:" `date`
```

### ① 기본 포트 `7001` → `8080`
컴포즈에서는 composite만 `8080:8080`으로 내 PC에 열려 있다(06).

### ② 함수 2개 추가: `testUrl`, `waitForService`
| 함수 | 하는 일 |
|---|---|
| `testUrl` | 받은 curl 명령을 실행해서 **성공(2xx)이면 0, 아니면 1**을 돌려준다. `-k` 인증서 무시, `-s` 조용히, `-f` HTTP 오류(4xx/5xx)면 실패로 처리, `-o /dev/null` 응답 본문은 버림 |
| `waitForService` | `testUrl`이 성공할 때까지 **3초마다 재시도**, 최대 100번(약 5분). 넘으면 `Give up`하고 종료 |

왜 필요한가: `docker compose up -d`는 컨테이너를 **띄우기만 하고 바로 돌아온다.** 스프링 부트가 완전히 뜨기까지는 몇 초~수십 초가 걸리는데, 그 전에 테스트하면 `Connection refused`로 실패한다. 그래서 응답이 올 때까지 기다린다.

### ③ 시작 부분: `Start Tests` 출력, `start` 블록, `waitForService` 호출
- `$@`는 스크립트에 붙인 인자 전체다. `./test-em-all.bash start`처럼 실행하면 `start`가 들어 있어서 if 안이 실행된다.
- `down --remove-orphans`: 이전 환경을 내리고, compose 파일에 없는 남은 컨테이너도 정리한다 → 항상 깨끗한 상태에서 시작 → `up -d`로 새로 띄운다.
- `waitForService`: `start` 여부와 상관없이 **항상** 실행된다. 이미 떠 있으면 바로 `DONE`.

### ④ 끝 부분: `stop` 블록
- `./test-em-all.bash stop`처럼 `stop`을 붙이면 테스트가 끝난 뒤 `docker compose down`으로 환경을 내린다. `start stop`처럼 둘 다 붙일 수 있다.
- `set -e`와 assert 함수의 `exit 1` 때문에, 중간에 테스트가 하나라도 실패하면 여기까지 오지 않는다. 그래서 **실패하면 환경이 내려가지 않고 남아 있다** → `docker compose logs`로 원인을 볼 수 있다.
- 스크립트 안의 `docker compose`는 현재 폴더의 compose 파일을 쓰므로 **`~/msa`에서 실행**한다.

---

## 실행
**`~/msa`에서** 실행한다. 스크립트 안의 `docker compose`가 현재 폴더의 docker-compose.yml을 쓰기 때문이다.
```bash
cd ~/msa
./test-em-all.bash start stop
```
| 인자 | 하는 일 |
|---|---|
| `start` | 테스트 전에 환경을 내렸다가(`down --remove-orphans`) 새로 띄움(`up -d`) |
| `stop` | 테스트가 모두 통과하면 환경을 내림(`down`) |
| (없음) | 이미 떠 있는 환경에 테스트만 실행 |

### 출력 흐름
1. `Start Tests`, `HOST=localhost`, `PORT=8080`
2. `Restarting the test environment...` → 이전 환경 down, 4개 up (`start`)
3. `Wait for: ... , retry #1 , retry #2 ...` → 스프링이 뜰 때까지 대기 (`waitForService`)
4. `DONE, continues...` → `Test OK ...` 줄이 여러 개 (3장과 같은 테스트)
5. `We are done, stopping the test environment...` → 4개 down (`stop`)
6. 마지막 줄 **`End, all tests OK:`** → 성공

### 교재 그림 4.17과 다르게 보이는 곳 (정상)
| 교재 | 우리 | 이유 |
|---|---|---|
| `Network chapter04_default` | `Network msa_default` | 컴포즈 이름 = 폴더 이름 (06과 같은 이유) |
| `Container chapter04-product-1` 등 | `Container msa-product-1` 등 | 같은 이유 |
| `$ docker-compose down ...` (하이픈) | `$ docker compose down ...` (띄어쓰기) | 스크립트의 `echo`가 띄어쓰기로 출력한다. 같은 프로그램이다 |
| `retry #1` 한 번 | 재시도 횟수가 다를 수 있음 | PC 속도에 따라 서비스가 뜨는 시간이 다르다 |

### 실패했을 때
- `Test FAILED ...`가 나오면 거기서 멈추고, 환경은 **내려가지 않고 남아 있다**(`stop`까지 가지 않음).
- 원인 확인 → 정리:
  ```bash
  docker compose logs            # 어느 서비스에서 무슨 오류가 났는지
  docker compose down            # 확인이 끝나면 직접 정리
  ```
- `Wait for: ... Give up`이 나오면 100번(약 5분) 기다려도 서비스가 응답하지 않은 것이다 → `docker compose logs`로 시작 실패 원인을 본다.

결과: `./test-em-all.bash start stop` → 마지막 줄 `End, all tests OK` 확인 → **4장 완료**
