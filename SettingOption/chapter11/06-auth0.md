# 06. (선택) Auth0을 외부 OpenID Connect 제공자로 쓰기 — 테넌트 준비

> 교재 "Auth0을 OpenID Connect 제공자로 사용" 절의 앞부분. 01~05의 로컬 인가 서버 대신 **외부 서비스 Auth0**이 토큰을 발급하게 바꾸는 선택 실습이다. Auth0 계정과 테넌트가 필요하다.
> 이 문서는 **Auth0 쪽 준비(앱, API, 사용자 만들기)** 까지다. **15~20장(3부)은 Auth0을 쓰지 않는다.** 3부는 01에서 만든 로컬 인가 서버(auth-server)를 쿠버네티스에 그대로 배포해서 토큰을 받는다. 그래서 이 문서는 건너뛰어도 3부 진행에 지장이 없다.
> 이후 composite/게이트웨이를 Auth0 토큰으로 검증하게 바꾸는 단계는 교재를 보며 이어서 추가한다.

## 1. Auth0에서 미리 할 것
**계정 만들기**: 브라우저에서 https://auth0.com → **Sign up** → 이메일(또는 Google/GitHub)로 가입한다. 무료(Free) 요금제로 충분하다. 가입이 끝나면 테넌트를 만드는 화면이 나온다.

| 할 일 | 위치 |
|---|---|
| 테넌트 만들기 | 가입 후 첫 화면. 이름은 자유(예: `dev-ml-0808`), 지역은 테넌트 주소에 들어간다(일본이면 `.jp.`) |
| 관리 API용 앱 만들기 | **Applications → APIs → Auth0 Management API → API Explorer** 탭. 토큰이 보이면 **API Explorer Application** 이 만들어진 것 (Save는 토큰 유효 기간만 바꾸는 버튼이라 안 눌러도 된다) |
| 테넌트 주소(Domain) 확인 | 같은 화면 위 **Identifier** `https://<도메인>/api/v2/`의 `<도메인>` 부분. 예: `dev-ml-0808.jp.auth0.com` |
| 관리 앱 Client ID/Secret | **Applications → Applications → API Explorer Application → Settings** |

## 2. 폴더와 비밀값 파일
`auth0` 폴더는 저장소 루트(`docker-compose.yml`과 같은 높이)에 만든다. **`env.bash`에는 비밀값이 들어가므로 `.gitignore`부터 만든다.** (교재의 `auth0/.gitignore`는 `ml.*`만 제외해서 `env.bash`가 그대로 커밋된다.)
```bash
mkdir -p auth0
echo "env.bash" > auth0/.gitignore
code auth0/env.bash
```
붙여넣기 (새 파일) — 오른쪽 값만 자기 것으로. `=` 양옆에 공백 없이.
```bash
#!/usr/bin/env bash

TENANT=dev-ml-0808.jp.auth0.com
MGM_CLIENT_ID=관리용-앱-Client-ID
MGM_CLIENT_SECRET=관리용-앱-Client-Secret
USER_EMAIL=테스트용-이메일
USER_PASSWORD=테스트용-비밀번호
```
| 변수 | 뜻 |
|---|---|
| `TENANT` | 테넌트 **도메인 전체** (`https://` 없이). 스크립트가 `https://$TENANT/oauth/token`처럼 쓴다 |
| `MGM_CLIENT_ID`, `MGM_CLIENT_SECRET` | API Explorer Application의 값. 스크립트가 이걸로 관리 API 토큰을 받아 설정을 만든다 |
| `USER_EMAIL`, `USER_PASSWORD` | 스크립트가 만들 **테스트 사용자**(브라우저 로그인용). 비밀번호는 대소문자, 숫자, 특수문자를 섞어 넉넉하게(약하면 Auth0이 거절) |

확인:
```bash
git check-ignore -v auth0/env.bash
```
`auth0/.gitignore:1:env.bash	auth0/env.bash`가 나오면 커밋되지 않는다.

## 3. setup-tenant.bash (새 파일)
```bash
code auth0/setup-tenant.bash
```
붙여넣기 (새 파일, 교재 그대로)
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
| 하는 일 (순서대로) | Auth0 화면에서 보이는 곳 |
|---|---|
| 관리 API 토큰 받기 (`MGM_CLIENT_ID/SECRET`) | - |
| 기본 로그인 방식을 사용자 DB(`Username-Password-Authentication`)로 | Settings → General |
| 앱 `reader`, `writer` 만들기 (콜백 `https://my.redirect.uri`) | Applications → Applications |
| API `product-composite` 만들기 (식별자 `https://localhost:8443/product-composite`, scope `product:read`, `product:write`) | Applications → APIs |
| 사용자 만들기 (`USER_EMAIL`) | User Management → Users |
| reader에 `product:read`, writer에 `product:read`+`product:write` 허용 | APIs → product-composite → Machine To Machine Applications |
| 마지막에 `export TENANT=...`, `WRITER_CLIENT_ID=...` 등 출력 | - |

이미 있는 것은 건너뛰므로 **여러 번 실행해도 된다.**

## 4. reset-tenant.bash (새 파일)
위에서 만든 앱, API, 사용자를 지운다(테넌트와 관리 앱은 남는다). 처음부터 다시 하고 싶을 때만 쓴다.
```bash
code auth0/reset-tenant.bash
```
붙여넣기 (새 파일, 교재 그대로)
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

## 5. 실행
```bash
cd ~/msa/auth0
chmod +x setup-tenant.bash reset-tenant.bash
./setup-tenant.bash
```
마지막에 이런 줄이 나오면 된다(값은 사람마다 다름).
```
Auth0 - OAuth2 settings:

export TENANT=dev-ml-0808.jp.auth0.com
export WRITER_CLIENT_ID=...
export WRITER_CLIENT_SECRET=...
export READER_CLIENT_ID=...
export READER_CLIENT_SECRET=...
```
이 5줄은 다음 단계(Auth0 토큰 받기)에서 쓴다. **비밀값이니 채팅이나 GitHub에 올리지 않는다.** 필요할 때 `./setup-tenant.bash`를 다시 실행하면 같은 값이 다시 출력된다.

## 6. 막히면
| 증상 | 원인 / 해결 |
|---|---|
| `jq: error ... null` 이나 아무 것도 안 만들어짐 | 관리 토큰을 못 받음. `TENANT`가 도메인 전체인지(`.jp.` 포함), `MGM_CLIENT_ID/SECRET`이 **API Explorer Application** 것인지 확인 |
| `429 Too Many Requests` | Auth0 무료 요금제 호출 제한. 잠시 뒤 다시 실행 (이미 만든 것은 건너뜀) |
| 사용자 생성 응답에 `PasswordStrengthError` | `USER_PASSWORD`를 더 복잡하게 바꾸고 다시 실행 |

다음 장: [../chapter12/README.md](../chapter12/README.md)
