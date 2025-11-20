#!/bin/bash
# Q-SIGN Keycloak SSO Test App Client 생성 스크립트

set -e

KC_URL="http://192.168.0.11:30181"
ADMIN_USER="admin"
ADMIN_PASS="admin"
REALM="myrealm"
CLIENT_ID="sso-test-app-client"
REDIRECT_URI="http://192.168.0.11:30300/*"
WEB_ORIGIN="http://192.168.0.11:30300"

echo "========================================="
echo "Q-SIGN Keycloak Client 생성"
echo "========================================="
echo ""
echo "Target: $KC_URL"
echo "Realm: $REALM"
echo "Client ID: $CLIENT_ID"
echo ""

# 1. Admin 토큰 획득
echo "Step 1: Getting admin token..."
TOKEN_RESPONSE=$(curl -s -X POST "${KC_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASS}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "ERROR: Failed to get admin token"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo "✓ Admin token obtained"
echo ""

# 2. 기존 클라이언트 확인
echo "Step 2: Checking if client already exists..."
EXISTING_CLIENT=$(curl -s -X GET "${KC_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

CLIENT_EXISTS=$(echo "$EXISTING_CLIENT" | python3 -c "import sys,json; print(len(json.load(sys.stdin)) > 0)" 2>/dev/null)

if [ "$CLIENT_EXISTS" = "True" ]; then
    echo "⚠ Client already exists: $CLIENT_ID"
    echo "  Updating existing client..."
    CLIENT_UUID=$(echo "$EXISTING_CLIENT" | python3 -c "import sys,json; print(json.load(sys.stdin)[0].get('id', ''))" 2>/dev/null)
else
    echo "✓ Client does not exist, will create new one"
    CLIENT_UUID=""
fi
echo ""

# 3. Client 설정 데이터
CLIENT_DATA=$(cat <<EOF
{
  "clientId": "${CLIENT_ID}",
  "name": "SSO Test App",
  "description": "Post-Quantum SSO Test Application",
  "enabled": true,
  "protocol": "openid-connect",
  "publicClient": true,
  "directAccessGrantsEnabled": true,
  "standardFlowEnabled": true,
  "implicitFlowEnabled": false,
  "serviceAccountsEnabled": false,
  "authorizationServicesEnabled": false,
  "redirectUris": [
    "${REDIRECT_URI}"
  ],
  "webOrigins": [
    "${WEB_ORIGIN}"
  ],
  "attributes": {
    "pkce.code.challenge.method": "S256",
    "post.logout.redirect.uris": "+"
  }
}
EOF
)

# 4. Client 생성 또는 업데이트
if [ -n "$CLIENT_UUID" ]; then
    echo "Step 3: Updating existing client..."
    UPDATE_RESPONSE=$(curl -s -X PUT "${KC_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "$CLIENT_DATA")

    if [ -z "$UPDATE_RESPONSE" ]; then
        echo "✓ Client updated successfully"
    else
        echo "Response: $UPDATE_RESPONSE"
    fi
else
    echo "Step 3: Creating new client..."
    CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KC_URL}/admin/realms/${REALM}/clients" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "$CLIENT_DATA")

    HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "204" ]; then
        echo "✓ Client created successfully (HTTP $HTTP_CODE)"
    else
        echo "⚠ Unexpected response code: $HTTP_CODE"
        echo "Response: $RESPONSE_BODY"
    fi
fi
echo ""

# 5. Client 설정 확인
echo "Step 4: Verifying client configuration..."
sleep 1

CLIENT_INFO=$(curl -s -X GET "${KC_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "$CLIENT_INFO" | python3 -c "
import sys, json
clients = json.load(sys.stdin)
if len(clients) > 0:
    client = clients[0]
    print('Client ID:', client.get('clientId'))
    print('Name:', client.get('name'))
    print('Enabled:', client.get('enabled'))
    print('Protocol:', client.get('protocol'))
    print('Public Client:', client.get('publicClient'))
    print('Redirect URIs:', ', '.join(client.get('redirectUris', [])))
    print('Web Origins:', ', '.join(client.get('webOrigins', [])))
    print('')
    print('✓ Client configuration verified!')
else:
    print('✗ Client not found!')
" 2>/dev/null

echo ""
echo "========================================="
echo "Complete"
echo "========================================="
echo ""
echo "다음 단계:"
echo "1. 브라우저에서 Q-APP 접속: http://192.168.0.11:30300"
echo "2. 'Login' 버튼 클릭"
echo "3. 로그인 정보 입력: testuser / admin"
echo "4. SSO 로그인 성공 확인"
echo ""
