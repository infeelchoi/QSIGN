#!/bin/bash
# Q-SIGN Keycloak PQC-realm에 SSO Test App Client 생성 스크립트

set -e

KC_URL="http://192.168.0.11:30181"
ADMIN_USER="admin"
ADMIN_PASS="admin"
REALM="PQC-realm"
CLIENT_ID="sso-test-app-client"
REDIRECT_URI="http://192.168.0.11:30300/*"
WEB_ORIGIN="http://192.168.0.11:30300"

echo "========================================="
echo "PQC-realm에 SSO Test App Client 생성"
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

# 2. PQC-realm 존재 확인
echo "Step 2: Checking if PQC-realm exists..."
REALM_CHECK=$(curl -s -w "\n%{http_code}" -X GET "${KC_URL}/admin/realms/${REALM}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

HTTP_CODE=$(echo "$REALM_CHECK" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PQC-realm exists"
else
    echo "⚠ PQC-realm not found (HTTP $HTTP_CODE)"
    echo "  Creating PQC-realm..."

    # PQC-realm 생성
    REALM_DATA=$(cat <<EOF
{
  "realm": "${REALM}",
  "enabled": true,
  "displayName": "PQC Realm",
  "displayNameHtml": "<b>Post-Quantum Cryptography</b> Realm",
  "sslRequired": "none",
  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "bruteForceProtected": true
}
EOF
)

    CREATE_REALM_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KC_URL}/admin/realms" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "$REALM_DATA")

    REALM_HTTP_CODE=$(echo "$CREATE_REALM_RESPONSE" | tail -n1)

    if [ "$REALM_HTTP_CODE" = "201" ] || [ "$REALM_HTTP_CODE" = "204" ]; then
        echo "✓ PQC-realm created successfully"
    else
        echo "✗ Failed to create PQC-realm (HTTP $REALM_HTTP_CODE)"
        exit 1
    fi
fi
echo ""

# 3. 기존 클라이언트 확인
echo "Step 3: Checking if client already exists..."
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

# 4. Client 설정 데이터
CLIENT_DATA=$(cat <<EOF
{
  "clientId": "${CLIENT_ID}",
  "name": "SSO Test App",
  "description": "Post-Quantum SSO Test Application with DILITHIUM3 + KYBER1024",
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
    "post.logout.redirect.uris": "+",
    "backchannel.logout.session.required": "true",
    "backchannel.logout.revoke.offline.tokens": "false"
  }
}
EOF
)

# 5. Client 생성 또는 업데이트
if [ -n "$CLIENT_UUID" ]; then
    echo "Step 4: Updating existing client..."
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
    echo "Step 4: Creating new client..."
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

# 6. testuser 생성 (없으면)
echo "Step 5: Creating test user (if not exists)..."
EXISTING_USER=$(curl -s -X GET "${KC_URL}/admin/realms/${REALM}/users?username=testuser" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

USER_EXISTS=$(echo "$EXISTING_USER" | python3 -c "import sys,json; print(len(json.load(sys.stdin)) > 0)" 2>/dev/null)

if [ "$USER_EXISTS" = "True" ]; then
    echo "✓ User 'testuser' already exists"
else
    echo "  Creating testuser..."

    USER_DATA=$(cat <<EOF
{
  "username": "testuser",
  "enabled": true,
  "emailVerified": true,
  "firstName": "Test",
  "lastName": "User",
  "email": "testuser@qsign.local",
  "credentials": [{
    "type": "password",
    "value": "admin",
    "temporary": false
  }]
}
EOF
)

    CREATE_USER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KC_URL}/admin/realms/${REALM}/users" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "$USER_DATA")

    USER_HTTP_CODE=$(echo "$CREATE_USER_RESPONSE" | tail -n1)

    if [ "$USER_HTTP_CODE" = "201" ] || [ "$USER_HTTP_CODE" = "204" ]; then
        echo "✓ User 'testuser' created successfully"
    else
        echo "⚠ Failed to create user (HTTP $USER_HTTP_CODE)"
    fi
fi
echo ""

# 7. Client 설정 확인
echo "Step 6: Verifying client configuration..."
sleep 1

CLIENT_INFO=$(curl -s -X GET "${KC_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "$CLIENT_INFO" | python3 -c "
import sys, json
clients = json.load(sys.stdin)
if len(clients) > 0:
    client = clients[0]
    print('✓ Client Configuration:')
    print('  Client ID:', client.get('clientId'))
    print('  Name:', client.get('name'))
    print('  Enabled:', client.get('enabled'))
    print('  Protocol:', client.get('protocol'))
    print('  Public Client:', client.get('publicClient'))
    print('  Redirect URIs:', ', '.join(client.get('redirectUris', [])))
    print('  Web Origins:', ', '.join(client.get('webOrigins', [])))
else:
    print('✗ Client not found!')
" 2>/dev/null

echo ""

# 8. Realm 정보 확인
echo "Step 7: Verifying PQC-realm configuration..."
REALM_INFO=$(curl -s "${KC_URL}/realms/${REALM}")

echo "$REALM_INFO" | python3 -c "
import sys, json
realm = json.load(sys.stdin)
print('✓ Realm Information:')
print('  Realm:', realm.get('realm'))
print('  Public Key:', realm.get('public_key', 'N/A')[:50] + '...')
print('  Token Service:', realm.get('token-service'))
print('  Account Service:', realm.get('account-service'))
print('  OIDC Discovery:', '${KC_URL}/realms/${REALM}/.well-known/openid-configuration')
" 2>/dev/null

echo ""
echo "========================================="
echo "Complete"
echo "========================================="
echo ""
echo "✅ PQC-realm 및 클라이언트 설정 완료!"
echo ""
echo "다음 단계:"
echo "1. Q-APP values.yaml 업데이트 (realm: myrealm → PQC-realm)"
echo "2. ArgoCD에서 q-app Sync"
echo "3. 브라우저에서 SSO 로그인 테스트"
echo ""
echo "로그인 정보:"
echo "  - URL: http://192.168.0.11:30300"
echo "  - Username: testuser"
echo "  - Password: admin"
echo ""
