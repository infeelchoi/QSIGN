#!/bin/bash

echo "========================================="
echo "  PQC 서명 검증 위치 분석"
echo "========================================="
echo ""

KEYCLOAK_URL="http://192.168.0.12:30180"
REALM="myrealm"
QKMS_POD="q-kms-7cd77c4595-2r5z7"

echo "## 1️⃣ Q-KMS API 엔드포인트 확인"
echo ""
echo "Q-KMS Vault 기본 엔드포인트:"
curl -s http://192.168.0.11:30820/v1/sys/health | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f'  ✅ Vault Version: {data.get(\"version\", \"N/A\")}')
print(f'  ✅ Status: Initialized, Unsealed')
"

echo ""
echo "Q-KMS 사용 가능한 API 경로:"
echo "  ├─ /v1/sys/health - Health check"
echo "  ├─ /v1/transit/keys - Transit keys list"
echo "  ├─ /v1/transit/sign/<key> - RSA 서명"
echo "  ├─ /v1/transit/verify/<key> - RSA 검증"
echo "  └─ ❌ /api/pqc/* - ML-DSA-87 API (미구현)"

echo ""
echo "## 2️⃣ Keycloak JWKS 공개키 확인"
echo ""
curl -s "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/certs" | python3 << 'PYTHON'
import sys, json

jwks = json.load(sys.stdin)
keys = jwks.get('keys', [])

print(f"총 {len(keys)}개의 공개키:")
print()

for i, key in enumerate(keys, 1):
    alg = key.get('alg', 'N/A')
    use = key.get('use', 'N/A')
    kid = key.get('kid', 'N/A')
    kty = key.get('kty', 'N/A')
    
    print(f"키 #{i}:")
    print(f"  ├─ Type (kty): {kty}")
    print(f"  ├─ Algorithm (alg): {alg}")
    print(f"  ├─ Use: {use}")
    print(f"  ├─ Key ID: {kid[:50]}...")
    
    # Check for PQC specific fields
    if 'dilithium' in kid.lower() or 'pqc' in kid.lower():
        print(f"  ├─ 🔐 PQC 키 발견!")
        if 'x' in key:
            print(f"  └─ Public Key (x): {key['x'][:60]}...")
    else:
        print(f"  └─ ℹ️  Classic 키 (RSA)")
    print()
PYTHON

echo ""
echo "## 3️⃣ Keycloak Pod에서 PQC Provider 확인"
echo ""
echo 'qwer1234!' | sudo -S kubectl exec -n q-sign keycloak-pqc-d4859fdd9-mvk4s -- ls -la /opt/keycloak/providers/ 2>&1 | grep -v "password" | grep -E "pqc|dilithium|bouncy" || echo "  Provider 파일 목록 조회 필요"

echo ""
echo "## 4️⃣ 현재 PQC 서명 검증 프로세스"
echo ""
echo "📋 현재 구조:"
echo ""
echo "┌────────────────────────────────────────────────────────┐"
echo "│  Keycloak-PQC (q-sign)                                 │"
echo "├────────────────────────────────────────────────────────┤"
echo "│  1. PQC Provider (Bouncy Castle)                       │"
echo "│     ├─ Dilithium 키 생성                                │"
echo "│     ├─ JWT 토큰 서명 (DILITHIUM3)                       │"
echo "│     └─ 공개키를 JWKS에 게시                             │"
echo "│                                                         │"
echo "│  2. 서명 검증                                           │"
echo "│     ├─ 클라이언트가 JWKS에서 공개키 다운로드             │"
echo "│     ├─ Bouncy Castle PQC 라이브러리로 검증              │"
echo "│     └─ ✅ Keycloak 내부에서 검증 수행                   │"
echo "└────────────────────────────────────────────────────────┘"
echo ""
echo "┌────────────────────────────────────────────────────────┐"
echo "│  Q-KMS (q-kms)                                          │"
echo "├────────────────────────────────────────────────────────┤"
echo "│  1. Vault Transit Engine                               │"
echo "│     ├─ RSA-4096 키 저장                                 │"
echo "│     ├─ RSA 서명/검증 API 제공                           │"
echo "│     └─ ❌ Dilithium 네이티브 미지원                     │"
echo "│                                                         │"
echo "│  2. 확장 필요                                           │"
echo "│     ├─ ML-DSA-87 서명 API 추가                          │"
echo "│     ├─ ML-DSA-87 검증 API 추가                          │"
echo "│     └─ liboqs 라이브러리 통합                           │"
echo "└────────────────────────────────────────────────────────┘"

echo ""
echo "## 5️⃣ Q-KMS Pod 내부 확인"
echo ""
echo "설치된 패키지:"
echo 'qwer1234!' | sudo -S kubectl exec -n q-kms $QKMS_POD -- dpkg -l | grep -E "python|crypto" 2>&1 | head -10 | grep -v "password" || echo "  패키지 목록 조회 필요"

echo ""
echo "========================================="
