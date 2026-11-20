# 05. 루트 빌드 설정, 테스트 스크립트, 실행

> 시작: 교수님 `빈 폴더 + spring init 결과(01 문서)` 상태 / 결과: 교수님 `Chapter03/2-basic-rest-services`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

루트 빌드 설정과 전체 테스트 스크립트.
- `settings.gradle`: 프로젝트 6개(api, util, 서비스 4개)를 묶는다. 아래쪽 `securityPatches` 블록은 교수님 코드에 있는 것으로, 취약점이 알려진 라이브러리만 패치 버전으로 올린다(Boot 버전은 그대로).
- `gradle/wrapper/gradle-wrapper.properties`: Gradle 버전을 교수님 코드와 같은 **8.14.6**으로 맞춘다(spring init은 더 새 버전을 줄 수 있다).
- `test-em-all.bash`: 서비스들이 떠 있는 상태에서 curl로 API를 불러 결과를 자동으로 확인한다.

## 5-1. `.gitignore` (새 파일)
```bash
code .gitignore
```
붙여넣기 (새 파일)
```
HELP.md
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache
bin/
!**/src/main/**/bin/
!**/src/test/**/bin/

.idea
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/

.vscode/
```

## 5-2. `gradle/wrapper/gradle-wrapper.properties`
```bash
code gradle/wrapper/gradle-wrapper.properties
```
아래처럼 맞추기 (전체 파일)
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
# ★ 변경 (전: distributionUrl=https\://services.gradle.org/distributions/gradle-9...)
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.6-bin.zip
networkTimeout=10000
# ★ 삭제 (2줄, 첫 줄: retries=0)
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

## 5-3. `settings.gradle` (새 파일)
```bash
code settings.gradle
```
붙여넣기 (새 파일)
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

include ':api'
include ':util'
include ':microservices:product-service'
include ':microservices:review-service'
include ':microservices:recommendation-service'
include ':microservices:product-composite-service'

// See MIGRATION-BOOT4.md, "2-5. 의존성 보안 패치".
def securityPatches = [
    'com.rabbitmq:amqp-client'                                    : '5.37.1',
    'at.yawk.lz4:lz4-java'                                        : '1.11.4',
    'org.xerial.snappy:snappy-java'                               : '1.1.10.9',
    'com.google.guava:guava'                                      : '33.7.2-jre',// Netflix Eureka (archaius, servo)
    'org.apache.httpcomponents:httpclient'                        : '4.5.14',
    'org.apache.httpcomponents.client5:httpclient5'               : '5.6.3',
    'org.apache.httpcomponents.core5:httpcore5'                   : '5.4.3',
    'org.apache.httpcomponents.core5:httpcore5-h2'                : '5.4.3',
    'org.apache.commons:commons-lang3'                            : '3.20.0',
    'io.opentelemetry:opentelemetry-api'                          : '1.67.0',
    'io.opentelemetry:opentelemetry-common'                       : '1.67.0',
    'io.opentelemetry:opentelemetry-context'                      : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk'                          : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-common'                   : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-trace'                    : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-metrics'                  : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-logs'                     : '1.67.0',
    'io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi': '1.67.0',
    'io.opentelemetry:opentelemetry-exporter-common'              : '1.67.0',
    'io.opentelemetry:opentelemetry-extension-trace-propagators'  : '1.67.0',
    'io.opentelemetry:opentelemetry-exporter-zipkin'              : '1.64.0',
]
gradle.beforeProject { project ->
    project.pluginManager.withPlugin('io.spring.dependency-management') {
        project.dependencyManagement {
            dependencies {
                securityPatches.each { ga, v -> dependency "${ga}:${v}" }
            }
        }
    }
    project.buildscript.configurations.configureEach {
        resolutionStrategy.eachDependency { details ->
            def v = securityPatches["${details.requested.group}:${details.requested.name}".toString()]
            if (v) details.useVersion(v)
        }
    }
}
```

## 5-4. `test-em-all.bash` (새 파일)
```bash
code test-em-all.bash
```
붙여넣기 (새 파일)
```bash
#!/usr/bin/env bash
: ${HOST=localhost}
: ${PORT=7001}
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

set -e

echo "HOST=${HOST}"
echo "PORT=${PORT}"

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

echo "End, all tests OK:" `date`
```

실행할 수 있게 권한을 준다(붙여넣기로 만든 파일은 실행 권한이 없다).
<!-- run -->
```bash
chmod +x test-em-all.bash
```

## 확인: 빌드, 서비스 4개 실행, 자동 테스트
```bash
cd ~/msa
./gradlew build
```
`BUILD SUCCESSFUL`. 그다음 서비스 4개를 백그라운드로 띄운다.
```bash
java -jar microservices/product-composite-service/build/libs/*.jar &
java -jar microservices/product-service/build/libs/*.jar &
java -jar microservices/recommendation-service/build/libs/*.jar &
java -jar microservices/review-service/build/libs/*.jar &
```
로그가 섞여 나온다. `Started ...Application`이 4번 나오면(10~20초) 다음을 실행한다.
```bash
./test-em-all.bash
```
마지막 줄 `End, all tests OK`. 직접 불러 보기:
```bash
curl -s localhost:7001/product-composite/1 | jq .
curl -s localhost:7001/product-composite/13 -w "%{http_code}\n" -o /dev/null
```
첫째는 상품 1의 합친 결과(추천 3개, 리뷰 3개), 둘째는 `404`.

끝내기(같은 터미널에서):
```bash
kill $(jobs -p)
```
