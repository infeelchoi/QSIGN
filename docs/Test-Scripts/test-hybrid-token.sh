#!/bin/bash

KEYCLOAK_URL="http://192.168.0.12:30180"
REALM="myrealm"

echo "========================================="
echo "  하이브리드 토큰 테스트"
echo "========================================="
echo ""

# 1. Check hybrid mode settings
echo "1️⃣ Keycloak-PQC 하이브리드 설정 확인..."
echo ""

# 2. Get JWKS to see available keys
echo "2️⃣ JWKS 키 확인..."
curl -s "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/certs" | python3 -c "
import sys, json
jwks = json.load(sys.stdin)
print(f'총 {len(jwks[\"keys\"])}개의 키 발견:')
for i, key in enumerate(jwks['keys'], 1):
    alg = key.get('alg', 'N/A')
    use = key.get('use', 'N/A')
    kid = key.get('kid', 'N/A')
    kty = key.get('kty', 'N/A')
    print(f'  키 #{i}:')
    print(f'    ├─ Type: {kty}')
    print(f'    ├─ Algorithm: {alg}')
    print(f'    ├─ Use: {use}')
    print(f'    └─ KID: {kid[:50]}...')
"

echo ""
echo "3️⃣ PQC 토큰 생성 (app3-pqc-client)..."
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=testpass123" \
  -d "grant_type=password" \
  -d "client_id=app3-pqc-client")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -n "$ACCESS_TOKEN" ]; then
    echo "✅ 토큰 생성 성공!"
    
    # Decode JWT header
    echo ""
    echo "📋 JWT 헤더 분석:"
    HEADER=$(echo "$ACCESS_TOKEN" | cut -d'.' -f1 | base64 -d 2>/dev/null)
    echo "$HEADER" | python3 -m json.tool
    
    ALG=$(echo "$HEADER" | python3 -c "import sys, json; print(json.load(sys.stdin).get('alg', 'N/A'))" 2>/dev/null)
    
    echo ""
    echo "🔐 서명 알고리즘: $ALG"
    
    # Check for hybrid signature
    echo ""
    echo "4️⃣ 하이브리드 서명 확인..."
    
    # Decode payload
    PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null)
    
    # Check if there's a hybrid_sig field or multiple signatures
    echo "$PAYLOAD" | python3 -c "
import sys, json
try:
    payload = json.load(sys.stdin)
    
    # Check for hybrid signature fields
    if 'hybrid_sig' in payload:
        print('✅ 하이브리드 서명 필드 발견!')
        print(f'   Hybrid Signature: {str(payload[\"hybrid_sig\"])[:100]}...')
    elif 'pqc_sig' in payload:
        print('✅ PQC 서명 필드 발견!')
    elif 'classical_sig' in payload:
        print('✅ Classical 서명 필드 발견!')
    else:
        print('ℹ️  표준 JWT 형식 (헤더에 서명 알고리즘 지정)')
        print(f'   알고리즘: {payload.get(\"alg\", \"N/A\")}')
    
    # Print some payload info
    print('')
    print('토큰 페이로드 정보:')
    print(f'  ├─ Issuer: {payload.get(\"iss\", \"N/A\")}')
    print(f'  ├─ Subject: {payload.get(\"sub\", \"N/A\")}')
    print(f'  ├─ Client: {payload.get(\"azp\", \"N/A\")}')
    print(f'  └─ User: {payload.get(\"preferred_username\", \"N/A\")}')
except Exception as e:
    print(f'❌ 페이로드 파싱 실패: {e}')
"
    
    # Check signature part
    echo ""
    echo "5️⃣ 서명 데이터 분석..."
    SIGNATURE=$(echo "$ACCESS_TOKEN" | cut -d'.' -f3)
    SIG_LENGTH=${#SIGNATURE}
    echo "   서명 길이: $SIG_LENGTH bytes (Base64 인코딩)"
    
    if [ $SIG_LENGTH -gt 500 ]; then
        echo "   ✅ PQC 서명 크기 (Dilithium3는 약 3-4KB)"
    else
        echo "   ℹ️  Classic 서명 크기 (RSA-256는 약 512 bytes)"
    fi
    
else
    echo "❌ 토큰 생성 실패"
fi

# Test with regular client
echo ""
echo "6️⃣ 일반 클라이언트로 테스트 (test-client)..."
TOKEN_RESPONSE2=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=testpass123" \
  -d "grant_type=password" \
  -d "client_id=test-client")

ACCESS_TOKEN2=$(echo "$TOKEN_RESPONSE2" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -n "$ACCESS_TOKEN2" ]; then
    HEADER2=$(echo "$ACCESS_TOKEN2" | cut -d'.' -f1 | base64 -d 2>/dev/null)
    ALG2=$(echo "$HEADER2" | python3 -c "import sys, json; print(json.load(sys.stdin).get('alg', 'N/A'))" 2>/dev/null)
    echo "   알고리즘: $ALG2"
    
    SIGNATURE2=$(echo "$ACCESS_TOKEN2" | cut -d'.' -f3)
    SIG_LENGTH2=${#SIGNATURE2}
    echo "   서명 길이: $SIG_LENGTH2 bytes"
fi

echo ""
echo "========================================="
echo "  테스트 완료"
echo "========================================="
