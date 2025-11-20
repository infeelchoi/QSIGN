#!/bin/bash

################################################################################
# PQC-realm에 app7-client 생성
################################################################################

set -e

KEYCLOAK_URL="http://192.168.0.11:30181"
REALM="PQC-realm"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"

echo "===== PQC-realm에 app7-client 생성 ====="
echo ""

# 1. Admin Token 획득
echo "[1/3] Admin 토큰 획득 중..."
ADMIN_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "grant_type=password" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASSWORD" | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

if [ -z "$ADMIN_TOKEN" ]; then
  echo "ERROR: Admin 토큰 획득 실패"
  exit 1
fi

echo "✓ Admin 토큰 획득 완료"
echo ""

# 2. app7-client 생성
echo "[2/3] app7-client 생성 중..."

CLIENT_DATA=$(cat <<EOF
{
  "clientId": "app7-client",
  "name": "App7 HSM PQC Integration",
  "description": "Post-Quantum Cryptography client for app7",
  "enabled": true,
  "publicClient": true,
  "protocol": "openid-connect",
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "implicitFlowEnabled": false,
  "serviceAccountsEnabled": false,
  "redirectUris": [
    "http://192.168.0.11:30207/*",
    "http://192.168.0.11:30207/callback"
  ],
  "webOrigins": [
    "http://192.168.0.11:30207"
  ],
  "attributes": {
    "pkce.code.challenge.method": "S256",
    "backchannel.logout.session.required": "true",
    "backchannel.logout.revoke.offline.tokens": "false"
  }
}
EOF
)

CREATE_RESPONSE=$(curl -sf -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$CLIENT_DATA")

HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n 1)

if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "409" ]; then
  echo "✓ app7-client 생성 완료 (HTTP $HTTP_CODE)"
else
  echo "ERROR: app7-client 생성 실패 (HTTP $HTTP_CODE)"
  echo "$CREATE_RESPONSE"
  exit 1
fi

echo ""

# 3. 생성된 클라이언트 확인
echo "[3/3] app7-client 확인 중..."

CLIENTS=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool | grep -A 1 "app7-client")

if echo "$CLIENTS" | grep -q "app7-client"; then
  echo "✓ app7-client가 PQC-realm에 성공적으로 생성되었습니다!"
  echo ""
  echo "클라이언트 정보:"
  echo "  Client ID: app7-client"
  echo "  Realm: PQC-realm"
  echo "  Redirect URI: http://192.168.0.11:30207/*"
  echo "  PKCE: S256"
  echo "  Protocol: openid-connect"
else
  echo "ERROR: app7-client 생성 확인 실패"
  exit 1
fi

echo ""
echo "===== 완료 ====="