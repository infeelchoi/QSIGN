#!/bin/bash

################################################################################
# PQC-realm에 모든 Q-APP 클라이언트 생성
# app1-client, app2-client, app3-client, app4-client, app6-client
################################################################################

set -e

KEYCLOAK_URL="http://192.168.0.11:30181"
REALM="PQC-realm"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"

echo "====================================================================="
echo "  PQC-realm에 모든 Q-APP 클라이언트 생성"
echo "====================================================================="
echo ""

# 1. Admin Token 획득
echo "[1/7] Admin 토큰 획득 중..."
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

# 클라이언트 생성 함수
create_client() {
  local CLIENT_ID=$1
  local CLIENT_NAME=$2
  local PORT=$3

  echo "[생성] $CLIENT_ID (포트: $PORT)"

  CLIENT_DATA=$(cat <<EOF
{
  "clientId": "$CLIENT_ID",
  "name": "$CLIENT_NAME",
  "description": "Post-Quantum Cryptography client for $CLIENT_ID",
  "enabled": true,
  "publicClient": true,
  "protocol": "openid-connect",
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "implicitFlowEnabled": false,
  "serviceAccountsEnabled": false,
  "redirectUris": [
    "http://192.168.0.11:$PORT/*",
    "http://192.168.0.11:$PORT/callback"
  ],
  "webOrigins": [
    "http://192.168.0.11:$PORT"
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

  if [ "$HTTP_CODE" == "201" ]; then
    echo "  ✓ $CLIENT_ID 생성 완료"
  elif [ "$HTTP_CODE" == "409" ]; then
    echo "  ℹ️ $CLIENT_ID 이미 존재 (건너뜀)"
  else
    echo "  ✗ $CLIENT_ID 생성 실패 (HTTP $HTTP_CODE)"
    return 1
  fi
}

# 2. app1-client 생성
echo "[2/7] app1-client 생성..."
create_client "app1-client" "App1 Angular Application" "30210"
echo ""

# 3. app2-client 생성
echo "[3/7] app2-client 생성..."
create_client "app2-client" "App2 Angular Application" "30201"
echo ""

# 4. app3-client 생성
echo "[4/7] app3-client 생성..."
create_client "app3-client" "App3 Node.js Application" "30202"
echo ""

# 5. app4 생성 (기존 app4 → app4-client로 통일)
echo "[5/7] app4-client 생성..."
create_client "app4-client" "App4 Node.js Application" "30203"
echo ""

# 6. app6-client 생성
echo "[6/7] app6-client 생성..."
create_client "app6-client" "App6 Luna HSM Test Client" "30205"
echo ""

# 7. 생성된 클라이언트 확인
echo "[7/7] 생성된 클라이언트 확인 중..."
echo ""

CLIENTS=$(curl -s "$KEYCLOAK_URL/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python3 -m json.tool | grep "clientId" | grep "app")

echo "PQC-realm에 등록된 Q-APP 클라이언트:"
echo "$CLIENTS"
echo ""

echo "====================================================================="
echo "✓ 모든 클라이언트 생성 완료!"
echo "====================================================================="
echo ""
echo "생성된 클라이언트:"
echo "  - app1-client (포트: 30210)"
echo "  - app2-client (포트: 30201)"
echo "  - app3-client (포트: 30202)"
echo "  - app4-client (포트: 30203)"
echo "  - app6-client (포트: 30205)"
echo "  - app7-client (포트: 30207) - 이미 생성됨"
echo "  - sso-test-app-client (포트: 30300) - 이미 생성됨"
echo ""
echo "다음 단계: values.yaml에서 앱 활성화 및 ArgoCD sync"
echo ""