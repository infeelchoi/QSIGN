#!/bin/bash

echo "========================================="
echo "  상세 토큰 분석 - PQC 검증 위치 확정"
echo "========================================="
echo ""

KEYCLOAK_URL="http://192.168.0.12:30180"
REALM="myrealm"

# Get token
echo "1️⃣ 토큰 생성..."
TOKEN_RESP=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=testpass123" \
  -d "grant_type=password" \
  -d "client_id=app3-pqc-client")

ACCESS_TOKEN=$(echo "$TOKEN_RESP" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ 토큰 생성 실패"
    exit 1
fi

echo "✅ 토큰 생성 성공"
echo ""

# Parse and analyze
echo "2️⃣ 토큰 헤더 분석..."
echo ""

python3 << PYEOF
import base64
import json

token = """$ACCESS_TOKEN"""
parts = token.split('.')

# Header
header_b64 = parts[0] + '=' * ((4 - len(parts[0]) % 4) % 4)
header = json.loads(base64.urlsafe_b64decode(header_b64))

print("JWT 헤더:")
print(json.dumps(header, indent=2))
print()

alg = header.get('alg', '')
kid = header.get('kid', '')

print(f"Algorithm: {alg}")
print(f"Key ID: {kid}")
print()

# Signature
signature = parts[2]
sig_len = len(signature)

print(f"Signature Length: {sig_len} characters")
print(f"Estimated Bytes: ~{sig_len * 3 // 4}")
print()

if 'DILITHIUM' in alg.upper():
    print("=" * 60)
    print("✅ DILITHIUM 서명 확인!")
    print("=" * 60)
    print()
    print("🔍 분석 결과:")
    print()
    print("1. 서명 생성 위치:")
    print("   └─ Keycloak-PQC Pod (Bouncy Castle PQC)")
    print()
    print("2. 공개키 위치 확인 필요:")
    print("   └─ JWKS에 Dilithium 공개키가 있어야 함")
    print()
    print("3. 검증 위치:")
    print("   └─ 클라이언트 (Bouncy Castle PQC 필요)")
    print()
elif 'RS' in alg.upper():
    print(f"ℹ️  RSA 서명: {alg}")
else:
    print(f"❓ 알 수 없는 알고리즘: {alg}")
PYEOF

echo ""
echo "3️⃣ JWKS 재확인 (Dilithium 키 검색)..."
echo ""

curl -s "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/certs" | python3 << 'PYTHON'
import sys, json

jwks = json.load(sys.stdin)
keys = jwks.get('keys', [])

print(f"JWKS에 등록된 키: {len(keys)}개")
print()

dilithium_found = False

for key in keys:
    alg = key.get('alg', 'N/A')
    kid = key.get('kid', 'N/A')
    kty = key.get('kty', 'N/A')
    
    # Check for Dilithium
    if 'dilithium' in alg.lower() or 'dilithium' in kid.lower():
        dilithium_found = True
        print("🔐 Dilithium 공개키 발견!")
        print(f"  ├─ Key ID: {kid}")
        print(f"  ├─ Algorithm: {alg}")
        print(f"  └─ Type: {kty}")
        print()
        
        # Print all fields
        for k, v in key.items():
            if k not in ['kid', 'alg', 'kty']:
                val_str = str(v)[:60] if len(str(v)) > 60 else str(v)
                print(f"     {k}: {val_str}")
        print()

if not dilithium_found:
    print("❌ JWKS에 Dilithium 공개키 없음!")
    print()
    print("등록된 키 목록:")
    for i, key in enumerate(keys, 1):
        print(f"  {i}. {key.get('alg', 'N/A')} - {key.get('kid', 'N/A')[:40]}...")
    print()
    print("⚠️  문제점:")
    print("  - 토큰은 DILITHIUM3로 서명됨")
    print("  - JWKS에는 RSA 키만 존재")
    print("  - 클라이언트가 서명 검증 불가능!")
    print()
    print("🔧 해결 방법:")
    print("  1. Keycloak이 Dilithium 공개키를 JWKS에 추가해야 함")
    print("  2. 또는 별도 PQC 공개키 엔드포인트 필요")
PYTHON

echo ""
echo "4️⃣ Q-KMS 역할 명확화"
echo ""

echo "┌────────────────────────────────────────────────┐"
echo "│  현재 아키텍처                                  │"
echo "├────────────────────────────────────────────────┤"
echo "│                                                │"
echo "│  Keycloak-PQC                                  │"
echo "│  ├─ Bouncy Castle PQC 사용                      │"
echo "│  ├─ DILITHIUM3 서명 생성                        │"
echo "│  ├─ 개인키: Keycloak 내부 저장                  │"
echo "│  └─ 공개키: JWKS 미등록 (문제!)                 │"
echo "│                                                │"
echo "│  Q-KMS                                         │"
echo "│  ├─ VAULT_ENABLED=true (설정만 됨)             │"
echo "│  ├─ 실제로 사용되지 않음                        │"
echo "│  └─ ML-DSA-87 API 미구현                        │"
echo "│                                                │"
echo "│  검증                                          │"
echo "│  ├─ 클라이언트가 JWKS에서 공개키 획득 시도      │"
echo "│  ├─ Dilithium 공개키 없음 → 검증 실패          │"
echo "│  └─ ❌ 현재 구조로는 검증 불가능!               │"
echo "│                                                │"
echo "└────────────────────────────────────────────────┘"

echo ""
echo "5️⃣ 해결 방안"
echo ""

echo "옵션 A: Keycloak JWKS에 Dilithium 공개키 추가"
echo "  ├─ Keycloak PQC Provider 수정"
echo "  ├─ Dilithium 공개키를 JWK 형식으로 변환"
echo "  └─ JWKS 엔드포인트에 추가"
echo ""

echo "옵션 B: Q-KMS 활용"
echo "  ├─ Q-KMS에 ML-DSA-87 API 구현"
echo "  ├─ Keycloak이 Q-KMS로 서명 요청"
echo "  ├─ Q-KMS가 공개키 제공"
echo "  └─ 클라이언트가 Q-KMS API로 검증"
echo ""

echo "========================================="
