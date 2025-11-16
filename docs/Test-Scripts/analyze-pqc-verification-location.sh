#!/bin/bash

echo "========================================="
echo "  PQC 서명 검증 위치 상세 분석"
echo "========================================="
echo ""

KEYCLOAK_URL="http://192.168.0.12:30180"
REALM="myrealm"

echo "## 1️⃣ Keycloak JWKS 공개키 확인"
echo ""

JWKS=$(curl -s "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/certs")

echo "$JWKS" | python3 -c "
import sys, json, base64

try:
    jwks = json.load(sys.stdin)
    keys = jwks.get('keys', [])
    
    print(f'총 {len(keys)}개의 공개키 발견')
    print()
    
    for i, key in enumerate(keys, 1):
        alg = key.get('alg', 'N/A')
        use = key.get('use', 'N/A')
        kid = key.get('kid', 'N/A')
        kty = key.get('kty', 'N/A')
        
        print(f'키 #{i}:')
        print(f'  ├─ Type: {kty}')
        print(f'  ├─ Algorithm: {alg}')
        print(f'  ├─ Use: {use}')
        print(f'  └─ Key ID: {kid[:50]}...')
        
        # RSA 키인 경우
        if kty == 'RSA':
            n = key.get('n', '')
            e = key.get('e', '')
            print(f'     ├─ Modulus (n): {n[:40]}...')
            print(f'     └─ Exponent (e): {e}')
        
        print()
except Exception as e:
    print(f'Error: {e}')
" 2>/dev/null

echo ""
echo "## 2️⃣ 실제 토큰 분석"
echo ""

# Get a token
TOKEN_RESP=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=testpass123" \
  -d "grant_type=password" \
  -d "client_id=app3-pqc-client")

ACCESS_TOKEN=$(echo "$TOKEN_RESP" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -n "$ACCESS_TOKEN" ]; then
    echo "✅ 토큰 생성 성공"
    echo ""
    
    echo "$ACCESS_TOKEN" | python3 << 'PYTHON'
import base64, json, sys

token = sys.stdin.read().strip()
parts = token.split('.')

# Header
header_b64 = parts[0] + '=' * ((4 - len(parts[0]) % 4) % 4)
header = json.loads(base64.urlsafe_b64decode(header_b64))

# Signature
signature = parts[2]

print("토큰 헤더:")
print(f"  ├─ Algorithm: {header.get('alg', 'N/A')}")
print(f"  └─ Key ID: {header.get('kid', 'N/A')}")
print()

alg = header.get('alg', '')
kid = header.get('kid', '')

print("서명 정보:")
print(f"  ├─ Signature Length: {len(signature)} chars")
print(f"  └─ Estimated Bytes: ~{len(signature) * 3 // 4}")
print()

if 'DILITHIUM' in alg.upper():
    print("🔐 Dilithium 서명 사용 중!")
    print()
    print("검증 위치 분석:")
    print("  ┌─────────────────────────────────────────┐")
    print("  │  서명 생성: Keycloak PQC Provider       │")
    print("  │  ├─ Bouncy Castle PQC 라이브러리         │")
    print("  │  ├─ Dilithium 개인키로 서명              │")
    print("  │  └─ JWT에 서명 포함                      │")
    print("  └─────────────────────────────────────────┘")
    print()
    print("  ┌─────────────────────────────────────────┐")
    print("  │  공개키 배포: JWKS 엔드포인트            │")
    print("  │  ├─ /realms/{realm}/certs                │")
    print("  │  └─ Dilithium 공개키 게시 (예상)         │")
    print("  └─────────────────────────────────────────┘")
    print()
    print("  ┌─────────────────────────────────────────┐")
    print("  │  서명 검증: 클라이언트 측                │")
    print("  │  ├─ JWKS에서 공개키 다운로드              │")
    print("  │  ├─ Bouncy Castle PQC 라이브러리 필요     │")
    print("  │  └─ Dilithium 공개키로 서명 검증          │")
    print("  └─────────────────────────────────────────┘")
    print()
    print("❓ Q-KMS 역할:")
    print("  ├─ 현재: 사용되지 않음 (VAULT_ENABLED이지만)")
    print("  ├─ Keycloak이 자체 Bouncy Castle로 서명")
    print("  └─ Q-KMS는 키 저장용으로만 설정됨")
    print()
else:
    print(f"ℹ️  Algorithm: {alg}")
PYTHON
fi

echo ""
echo "## 3️⃣ Keycloak 환경 변수 재확인"
echo ""
echo 'qwer1234!' | sudo -S kubectl get deployment keycloak-pqc -n q-sign -o yaml 2>&1 | grep -A 5 "VAULT" | grep -v "password" | head -20

echo ""
echo "## 4️⃣ 정리: 현재 PQC 서명/검증 위치"
echo ""
echo "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓"
echo "┃  현재 구조 (Keycloak 자체 처리)        ┃"
echo "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛"
echo ""
echo "1. 서명 생성:"
echo "   Location: Keycloak-PQC Pod"
echo "   Library: Bouncy Castle PQC"
echo "   Algorithm: DILITHIUM3"
echo "   Key Storage: Keycloak 내부"
echo ""
echo "2. 공개키 배포:"
echo "   Endpoint: /realms/myrealm/certs (JWKS)"
echo "   Format: JWK (JSON Web Key)"
echo ""
echo "3. 서명 검증:"
echo "   Location: 클라이언트 애플리케이션"
echo "   Required: Bouncy Castle PQC 라이브러리"
echo "   Process: JWKS → 공개키 → Dilithium 검증"
echo ""
echo "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓"
echo "┃  Q-KMS 역할 (현재)                     ┃"
echo "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛"
echo ""
echo "현재 상태:"
echo "  ❌ Keycloak이 Q-KMS를 서명에 사용하지 않음"
echo "  ❌ VAULT_ENABLED=true지만 실제 미사용"
echo "  ❌ ML-DSA-87 API 미구현"
echo ""
echo "Q-KMS 활용 방안:"
echo "  ① Q-KMS에서 Dilithium 키 생성/저장"
echo "  ② Keycloak이 Q-KMS API 호출해서 서명"
echo "  ③ 클라이언트가 Q-KMS API로 검증"
echo ""
echo "========================================="
