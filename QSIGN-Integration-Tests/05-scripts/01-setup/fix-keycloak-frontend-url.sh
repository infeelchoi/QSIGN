#!/bin/bash
# Q-SIGN Keycloak Frontend URL 설정 스크립트

set -e

KC_URL="http://192.168.0.11:30181"
ADMIN_USER="admin"
ADMIN_PASS="admin"
REALM="myrealm"
FRONTEND_URL="http://192.168.0.11:30181"

echo "========================================="
echo "Q-SIGN Keycloak Frontend URL 설정"
echo "========================================="
echo ""
echo "Target: $KC_URL"
echo "Realm: $REALM"
echo "Frontend URL: $FRONTEND_URL"
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

# 2. 현재 Realm 설정 가져오기
echo "Step 2: Getting current realm configuration..."
REALM_CONFIG=$(curl -s -X GET "${KC_URL}/admin/realms/${REALM}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "Current Frontend URL:"
echo "$REALM_CONFIG" | python3 -c "import sys,json; d=json.load(sys.stdin); print('  ', d.get('attributes', {}).get('frontendUrl', 'Not set'))" 2>/dev/null || echo "  Unable to parse"
echo ""

# 3. Frontend URL 설정
echo "Step 3: Updating Frontend URL..."
UPDATE_DATA=$(cat <<EOF
{
  "attributes": {
    "frontendUrl": "${FRONTEND_URL}"
  }
}
EOF
)

UPDATE_RESPONSE=$(curl -s -X PUT "${KC_URL}/admin/realms/${REALM}" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "$UPDATE_DATA")

if [ -z "$UPDATE_RESPONSE" ]; then
    echo "✓ Frontend URL updated successfully"
else
    echo "Response: $UPDATE_RESPONSE"
fi
echo ""

# 4. 설정 확인
echo "Step 4: Verifying configuration..."
sleep 2

REALM_INFO=$(curl -s "${KC_URL}/realms/${REALM}")
TOKEN_SERVICE=$(echo "$REALM_INFO" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token-service', ''))" 2>/dev/null)

echo "Token Service URL: $TOKEN_SERVICE"

if echo "$TOKEN_SERVICE" | grep -q "30181"; then
    echo "✓ SUCCESS: Frontend URL correctly configured!"
elif echo "$TOKEN_SERVICE" | grep -q "30699"; then
    echo "⚠ WARNING: Still pointing to 30699"
    echo "  May need Keycloak restart"
else
    echo "? UNKNOWN: $TOKEN_SERVICE"
fi

echo ""
echo "========================================="
echo "Complete"
echo "========================================="
