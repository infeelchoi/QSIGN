#!/bin/bash

KEYCLOAK_URL="http://192.168.0.11:30699"
REALM="myrealm"

echo "========================================="
echo "  PQC-SSO 토큰 상세 분석"
echo "========================================="
echo ""

# Get token
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=test123" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

echo "$TOKEN_RESPONSE" | python3 -c "
import sys, json, base64

resp = json.load(sys.stdin)
access_token = resp.get('access_token', '')

if not access_token:
    print('❌ 토큰 없음')
    print(f'에러: {resp.get(\"error\", \"unknown\")}')
    print(f'설명: {resp.get(\"error_description\", \"unknown\")}')
    sys.exit(1)

print('✅ 토큰 획득 성공!')
print('')

parts = access_token.split('.')
if len(parts) < 3:
    print('❌ 잘못된 JWT 형식')
    sys.exit(1)

# Decode header
header_b64 = parts[0]
padding = (4 - len(header_b64) % 4) % 4
header_b64 += '=' * padding

header = json.loads(base64.urlsafe_b64decode(header_b64))

print('📋 JWT 헤더:')
for key, value in header.items():
    print(f'  ├─ {key}: {value}')

alg = header.get('alg', 'N/A')
kid = header.get('kid', 'N/A')

# Decode payload (first part)
payload_b64 = parts[1]
padding = (4 - len(payload_b64) % 4) % 4
payload_b64 += '=' * padding

payload = json.loads(base64.urlsafe_b64decode(payload_b64))

print('')
print('📦 JWT 페이로드 (주요 필드):')
print(f'  ├─ Issuer: {payload.get(\"iss\", \"N/A\")}')
print(f'  ├─ Subject: {payload.get(\"sub\", \"N/A\")}')
print(f'  ├─ Username: {payload.get(\"preferred_username\", \"N/A\")}')
print(f'  └─ Client: {payload.get(\"azp\", \"N/A\")}')

# Signature
sig = parts[2]
sig_len = len(sig)
sig_bytes = sig_len * 3 // 4

print('')
print('🔐 서명 정보:')
print(f'  ├─ Base64 길이: {sig_len} characters')
print(f'  ├─ 예상 바이트: ~{sig_bytes} bytes')
print(f'  ├─ 서명 시작: {sig[:80]}...')
print(f'  └─ 서명 끝: ...{sig[-40:]}')

print('')
print('📊 알고리즘 분석:')
print(f'  Algorithm: {alg}')

if 'DILITHIUM' in alg.upper():
    print(f'  ✅ PQC 알고리즘 (Dilithium)')
    print(f'  ✅ 양자 내성 암호화')
    if sig_bytes > 2000:
        print(f'  ✅ Dilithium 서명 크기 적절 (~3KB)')
elif 'HYBRID' in alg.upper():
    print(f'  ✅ 하이브리드 알고리즘')
    print(f'  ✅ PQC + Classical 조합')
elif alg.startswith('RS'):
    print(f'  ℹ️  클래식 RSA 알고리즘')
    if sig_bytes > 2000:
        print(f'  ⚠️  비정상적으로 큰 서명 - 하이브리드일 가능성')
    else:
        print(f'  ℹ️  표준 RSA 서명 크기')
else:
    print(f'  ℹ️  기타 알고리즘: {alg}')

# Check refresh token
refresh_token = resp.get('refresh_token', '')
if refresh_token:
    print('')
    print('🔄 Refresh Token:')
    ref_parts = refresh_token.split('.')
    if len(ref_parts) >= 3:
        ref_header_b64 = ref_parts[0]
        padding = (4 - len(ref_header_b64) % 4) % 4
        ref_header_b64 += '=' * padding
        ref_header = json.loads(base64.urlsafe_b64decode(ref_header_b64))
        print(f'  └─ Algorithm: {ref_header.get(\"alg\", \"N/A\")}')

print('')
print('========================================')
"
