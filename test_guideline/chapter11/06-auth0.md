# 06. (선택) Auth0 스크립트

> 시작: 교수님 `Chapter10` 상태 / 결과: 교수님 `Chapter11`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

교재 마지막의 "외부 OpenID Connect 제공자(Auth0)로 바꿔 보기"에 쓰는 스크립트다. 본 실습(test-em-all)과는 관계없다.
- **이미 값을 채운 `auth0/env.bash`가 있으면 덮어쓰지 않는다.** 아래 `env.bash`는 빈칸 틀이다.
- `env.bash`에는 비밀값이 들어가므로 git에 올리지 않는다. 교수님 코드의 `auth0/.gitignore`는 `ml.*`만 제외하므로, git을 쓴다면 `.gitignore`에 `env.bash`를 직접 추가하는 것이 안전하다.

## 6-1. `auth0/.gitignore` (새 파일)
```bash
mkdir -p auth0
code auth0/.gitignore
```
붙여넣기 (새 파일)
```
ml.*
```

## 6-2. `auth0/env.bash` (새 파일)
```bash
code auth0/env.bash
```
붙여넣기 (새 파일)
```bash
#!/usr/bin/env bash

TENANT=
MGM_CLIENT_ID=
MGM_CLIENT_SECRET=
USER_EMAIL=
USER_PASSWORD=
```

실행할 수 있게 권한을 준다(붙여넣기로 만든 파일은 실행 권한이 없다).
<!-- run -->
```bash
chmod +x auth0/env.bash
```

## 6-3. `auth0/reset-tenant.bash` (새 파일)
```bash
code auth0/reset-tenant.bash
```
붙여넣기 (새 파일)
```bash
#!/usr/bin/env bash

source env.bash

CLIENT_REDIRECT_URI=https://my.redirect.uri
API_NAME=product-composite
API_URL=https://localhost:8443/product-composite

set -e

AT=$(curl -s --request POST \
  --url https://$TENANT/oauth/token \
  --header 'content-type: application/json' \
  --data "{\"client_id\":\"$MGM_CLIENT_ID\",\"client_secret\":\"$MGM_CLIENT_SECRET\",\"audience\":\"https://$TENANT/api/v2/\",\"grant_type\":\"client_credentials\"}" | jq -r .access_token)

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients?fields=name | jq -r '.[] | select(.name == "reader") | .name') ]
then
  echo "Delete reader client app..."
  readerId=$(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients | jq -r '.[] | select(.name == "reader") | .client_id')
  curl -s -H "Authorization: Bearer $AT" -X DELETE https://$TENANT/api/v2/clients/$readerId
else
  echo "Reader client app already deleted"
fi

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients?fields=name | jq -r '.[] | select(.name == "writer") | .name') ]
then
  echo "Delete writer client app..."
  writerId=$(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients | jq -r '.[] | select(.name == "writer") | .client_id')
  curl -s -H "Authorization: Bearer $AT" -X DELETE https://$TENANT/api/v2/clients/$writerId
else
  echo "Writer client app already deleted"
fi

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" "https://$TENANT/api/v2/resource-servers" | jq -r ".[] | select(.name == \"$API_NAME\") | .name") ]
then
  echo "Delete API $API_NAME ($API_URL)..."
  apiID=$(curl -s -H "Authorization: Bearer $AT" "https://$TENANT/api/v2/resource-servers" | jq -r ".[] | select(.name == \"$API_NAME\") | .id")
  curl -s -H "Authorization: Bearer $AT" -X DELETE https://$TENANT/api/v2/resource-servers/$apiID
else
  echo "API $API_NAME ($API_URL) already deleted"
fi

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" --get --data-urlencode "email=$USER_EMAIL" https://$TENANT/api/v2/users-by-email | jq -r ".[] | select(.email == \"$USER_EMAIL\") | .email") ]
then
  userID=$(curl -s -H "Authorization: Bearer $AT" --get --data-urlencode "email=$USER_EMAIL" https://$TENANT/api/v2/users-by-email | jq -r ".[] | select(.email == \"$USER_EMAIL\") | .user_id")
  echo "Delete user with email $USER_EMAIL..."
  curl -s -H "Authorization: Bearer $AT" -X DELETE https://$TENANT/api/v2/users/$userID
else
  echo "User with email $USER_EMAIL already deleted"
fi
```

실행할 수 있게 권한을 준다(붙여넣기로 만든 파일은 실행 권한이 없다).
<!-- run -->
```bash
chmod +x auth0/reset-tenant.bash
```

## 6-4. `auth0/setup-tenant.bash` (새 파일)
```bash
code auth0/setup-tenant.bash
```
붙여넣기 (새 파일)
```bash
#!/usr/bin/env bash

source env.bash

CLIENT_REDIRECT_URI=https://my.redirect.uri
API_NAME=product-composite
API_URL=https://localhost:8443/product-composite

set -e

AT=$(curl -s --request POST \
  --url https://$TENANT/oauth/token \
  --header 'content-type: application/json' \
  --data "{\"client_id\":\"$MGM_CLIENT_ID\",\"client_secret\":\"$MGM_CLIENT_SECRET\",\"audience\":\"https://$TENANT/api/v2/\",\"grant_type\":\"client_credentials\"}" | jq -r .access_token)

echo "Update the tenant, set its default connection to a user dictionary..."
curl -s -H "Authorization: Bearer $AT" -X PATCH  -H "Content-Type: application/json" -d '{"default_directory":"Username-Password-Authentication"}' https://$TENANT/api/v2/tenants/settings | jq .default_directory

createClientBody='"callbacks":["https://my.redirect.uri"],"app_type":"non_interactive","grant_types":["authorization_code","implicit","refresh_token","client_credentials","password","http://auth0.com/oauth/grant-type/password-realm"],"oidc_conformant":true,"token_endpoint_auth_method":"client_secret_post"'
if [ ! -z $(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients?fields=name | jq -r '.[] | select(.name == "reader") | .name') ]
then
  echo "Reader client app already exists"
else
  echo "Creates reader client app..."
  curl -s -H "Authorization: Bearer $AT" -X POST -H "Content-Type: application/json" -d "{\"name\":\"reader\",$createClientBody}" https://$TENANT/api/v2/clients | jq .
fi
READER_CLIENT_ID=$(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients | jq -r '.[] | select(.name == "reader") | .client_id')
READER_CLIENT_SECRET=$(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients | jq -r '.[] | select(.name == "reader") | .client_secret')

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients?fields=name | jq -r '.[] | select(.name == "writer") | .name') ]
then
  echo "Writer client app already exists"
else
  echo "Creates writer client app..."
  curl -s -H "Authorization: Bearer $AT" -X POST -H "Content-Type: application/json" -d "{\"name\":\"writer\",$createClientBody}" https://$TENANT/api/v2/clients | jq .
fi
WRITER_CLIENT_ID=$(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients | jq -r '.[] | select(.name == "writer") | .client_id')
WRITER_CLIENT_SECRET=$(curl -s -H "Authorization: Bearer $AT" https://$TENANT/api/v2/clients | jq -r '.[] | select(.name == "writer") | .client_secret')

sleep 1

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" "https://$TENANT/api/v2/resource-servers" | jq -r ".[] | select(.name == \"$API_NAME\") | .name") ]
then
  echo "API $API_NAME ($API_URL) already exists"
else
  echo "Creates API $API_NAME ($API_URL)..."
  curl -s -H "Authorization: Bearer $AT" -X POST -H "Content-Type: application/json" -d "{\"name\": \"$API_NAME\", \"identifier\": \"$API_URL\", \"scopes\": [{ \"value\": \"product:read\", \"description\": \"Read product information\"}, {\"value\": \"product:write\", \"description\": \"Update product information\"}]}" https://$TENANT/api/v2/resource-servers | jq .
fi

if [ ! -z $(curl -s -H "Authorization: Bearer $AT" --get --data-urlencode "email=$USER_EMAIL" https://$TENANT/api/v2/users-by-email | jq -r ".[] | select(.email == \"$USER_EMAIL\") | .email") ]
then
  echo "User with email $USER_EMAIL already exists"
else
  echo "Creates user with email $USER_EMAIL..."
  curl -s -H "Authorization: Bearer $AT" -X POST  -H "Content-Type: application/json" -d "{\"email\": \"$USER_EMAIL\",  \"connection\": \"Username-Password-Authentication\", \"password\": \"$USER_PASSWORD\"}" https://$TENANT/api/v2/users | jq .
fi

clientGrantsCount=$(curl -s -H "Authorization: Bearer $AT" --get --data-urlencode "audience=$API_URL" --data-urlencode "client_id=$READER_CLIENT_ID" https://$TENANT/api/v2/client-grants | jq length)
if [[ "$clientGrantsCount" -ne 0 ]]
then
  echo "Client grant for the reader app to access the $API_NAME API already exists"
else
  echo "Create client grant for the reader app to access the $API_NAME API..."
  curl -s -H "Authorization: Bearer $AT" -X POST  -H "Content-Type: application/json" -d "{\"client_id\":\"$READER_CLIENT_ID\",\"audience\":\"$API_URL\",\"scope\":[\"product:read\"]}" https://$TENANT/api/v2/client-grants | jq .
  echo
fi

clientGrantsCount=$(curl -s -H "Authorization: Bearer $AT" --get --data-urlencode "audience=$API_URL" --data-urlencode "client_id=$WRITER_CLIENT_ID" https://$TENANT/api/v2/client-grants | jq length)
if [[ "$clientGrantsCount" -ne 0 ]]
then
  echo "Client grant for the writer app to access the $API_NAME API already exists"
else
  echo "Create client grant for the writer app to access the $API_NAME API..."
  curl -s -H "Authorization: Bearer $AT" -X POST  -H "Content-Type: application/json" -d "{\"client_id\":\"$WRITER_CLIENT_ID\",\"audience\":\"$API_URL\",\"scope\":[\"product:read\",\"product:write\"]}" https://$TENANT/api/v2/client-grants | jq .
  echo
fi

echo
echo "Auth0 - OAuth2 settings:"
echo
echo export TENANT=$TENANT
echo export WRITER_CLIENT_ID=$WRITER_CLIENT_ID
echo export WRITER_CLIENT_SECRET=$WRITER_CLIENT_SECRET
echo export READER_CLIENT_ID=$READER_CLIENT_ID
echo export READER_CLIENT_SECRET=$READER_CLIENT_SECRET
```

실행할 수 있게 권한을 준다(붙여넣기로 만든 파일은 실행 권한이 없다).
<!-- run -->
```bash
chmod +x auth0/setup-tenant.bash
```

## 확인: 빌드, 컴포즈, 자동 테스트
```bash
cd ~/msa
./gradlew build
docker compose build
./test-em-all.bash start
```
처음에 `ACCESS_TOKEN=eyJ...`, 마지막 줄 `End, all tests OK`.

## 토큰으로 직접 호출
```bash
ACCESS_TOKEN=$(curl -k https://writer:secret-writer@localhost:8443/oauth2/token -d grant_type=client_credentials -d scope="product:read product:write" -s | jq -r .access_token)
curl -k https://localhost:8443/product-composite/1 -s -o /dev/null -w "%{http_code}\n"
curl -k https://localhost:8443/product-composite/1 -H "Authorization: Bearer $ACCESS_TOKEN" -s | jq .productId
```
`401`(토큰 없음), `1`.
```bash
READER_TOKEN=$(curl -k https://reader:secret-reader@localhost:8443/oauth2/token -d grant_type=client_credentials -d scope="product:read" -s | jq -r .access_token)
curl -k -X DELETE https://localhost:8443/product-composite/1 -H "Authorization: Bearer $READER_TOKEN" -s -o /dev/null -w "%{http_code}\n"
curl -k https://localhost:8443/eureka/api/apps -H "accept:application/json" -s -o /dev/null -w "%{http_code}\n"
curl -k https://u:p@localhost:8443/eureka/api/apps -H "accept:application/json" -s | jq -r '.applications.application[].name'
```
`403`(reader는 삭제 권한 없음), `401`(Eureka 계정 없음), 그리고 앱 6개(AUTH-SERVER 포함).

**Swagger UI 로그인**(https://localhost:8443/openapi/swagger-ui.html → Authorize)은 교수님 코드 그대로면 `code_challenge` 오류가 난다. 고치는 법은 [troubleshooting.md](troubleshooting.md) 맨 아래(교수님 코드와 다름, 선택).

끝내기:
```bash
docker compose down
```
