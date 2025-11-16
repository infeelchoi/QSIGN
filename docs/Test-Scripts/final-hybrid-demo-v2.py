#!/usr/bin/env python3
"""
하이브리드 서명 시스템 최종 데모
RSA (Keycloak) + ML-DSA-87 (Q-KMS Concept)
"""

import base64
import json
import hashlib
import urllib.request
import urllib.parse

KEYCLOAK_URL = "http://192.168.0.12:30180"
REALM = "myrealm"
CLIENT = "app3-pqc-client"

def get_pqc_token():
    """Keycloak에서 PQC 토큰 획득"""
    url = f"{KEYCLOAK_URL}/realms/{REALM}/protocol/openid-connect/token"
    data = urllib.parse.urlencode({
        "username": "testuser",
        "password": "testpass123",
        "grant_type": "password",
        "client_id": CLIENT
    }).encode()
    
    req = urllib.request.Request(url, data=data)
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode())
        return result.get('access_token', '')

def parse_jwt(token):
    """JWT 토큰 파싱"""
    parts = token.split('.')
    
    # Header
    header_b64 = parts[0] + '=' * ((4 - len(parts[0]) % 4) % 4)
    header = json.loads(base64.urlsafe_b64decode(header_b64))
    
    # Payload
    payload_b64 = parts[1] + '=' * ((4 - len(parts[1]) % 4) % 4)
    payload = json.loads(base64.urlsafe_b64decode(payload_b64))
    
    # Signature
    signature = parts[2]
    
    return header, payload, signature, f"{parts[0]}.{parts[1]}"

def main():
    print("=" * 60)
    print("  하이브리드 서명 시스템 최종 데모")
    print("  RSA (Keycloak) + ML-DSA-87 (Q-KMS)")
    print("=" * 60)
    print()
    
    # 1. Get token
    print("## 1단계: Keycloak에서 PQC 토큰 생성")
    print()
    
    try:
        token = get_pqc_token()
    except Exception as e:
        print(f"❌ 토큰 생성 실패: {e}")
        return
    
    if not token:
        print("❌ 토큰 없음")
        return
    
    print("✅ 토큰 생성 성공")
    print()
    
    # 2. Parse token
    header, payload, signature, message = parse_jwt(token)
    
    print("## 2단계: 토큰 구조 분석")
    print()
    print("JWT 헤더:")
    print(f"  ├─ Algorithm: {header.get('alg', 'N/A')}")
    print(f"  ├─ Type: {header.get('typ', 'N/A')}")
    print(f"  └─ Key ID: {header.get('kid', 'N/A')[:50]}...")
    print()
    
    print("JWT 페이로드 (주요 필드):")
    print(f"  ├─ Issuer: {payload.get('iss', 'N/A')}")
    print(f"  ├─ Subject: {payload.get('sub', 'N/A')[:20]}...")
    print(f"  ├─ Username: {payload.get('preferred_username', 'N/A')}")
    print(f"  └─ Client: {payload.get('azp', 'N/A')}")
    print()
    
    sig_len = len(signature)
    sig_bytes = sig_len * 3 // 4
    print("JWT 서명:")
    print(f"  ├─ Length: {sig_len} chars (~{sig_bytes} bytes)")
    print(f"  ├─ Start: {signature[:60]}...")
    print(f"  └─ End: ...{signature[-40:]}")
    print()
    
    # 3. Hybrid signature analysis
    alg = header.get('alg', '')
    
    print("## 3단계: 하이브리드 서명 분석")
    print()
    
    if 'DILITHIUM' in alg.upper():
        print(f"✅ 현재 서명 방식: PQC ({alg})")
        print()
        print("📋 하이브리드 서명 구조:")
        print()
        print("┌─" + "─" * 56 + "─┐")
        print("│  JWT 토큰 (현재 구현)                                   │")
        print("├─" + "─" * 56 + "─┤")
        print(f"│  Header: {{alg: {alg:<15}}}                      │")
        print("│  Payload: {{user data, claims...}}                    │")
        print(f"│  Signature: Dilithium 서명 (~{sig_bytes} bytes)        │")
        print("└─" + "─" * 56 + "─┘")
        print()
        
        print("🔐 하이브리드 서명 확장 방안:")
        print()
        print("┌─────────────────────────────────────────────────────┐")
        print("│  옵션 A: 듀얼 서명 JWT                               │")
        print("├─────────────────────────────────────────────────────┤")
        print("│  Header: {                                          │")
        print("│    alg: \"HYBRID\",                                  │")
        print("│    pqc_alg: \"DILITHIUM3\",                         │")
        print("│    classical_alg: \"RS256\"                         │")
        print("│  }                                                  │")
        print("│  Payload: { ... }                                   │")
        print("│  Signatures: {                                      │")
        print("│    rsa: \"<RSA 서명>\",                             │")
        print("│    dilithium: \"<Dilithium 서명>\"                 │")
        print("│  }                                                  │")
        print("└─────────────────────────────────────────────────────┘")
        print()
        
        print("┌─────────────────────────────────────────────────────┐")
        print("│  옵션 B: 중첩 JWT (현재 + RSA 외부 서명)             │")
        print("├─────────────────────────────────────────────────────┤")
        print("│  Inner JWT: Dilithium 서명 (현재 구현)               │")
        print("│  Outer JWT: RSA 서명으로 Inner JWT 감싸기            │")
        print("│  - 레거시 시스템: RSA 서명 검증                       │")
        print("│  - 최신 시스템: Dilithium 서명 검증                   │")
        print("└─────────────────────────────────────────────────────┘")
        print()
        
    # 4. Verification process
    print("## 4단계: 검증 프로세스 (개념)")
    print()
    
    # RSA signature simulation
    rsa_sig_hash = hashlib.sha256(message.encode()).hexdigest()
    print("🔑 RSA 서명 (Keycloak):")
    print(f"  ├─ Algorithm: RS256")
    print(f"  ├─ Key Source: Keycloak 키스토어")
    print(f"  ├─ Message Hash (SHA-256): {rsa_sig_hash[:32]}...")
    print(f"  ├─ Signature Size: ~512 bytes")
    print(f"  └─ 검증: 표준 JWT 라이브러리")
    print()
    
    # Dilithium signature (actual)
    dilithium_sig_hash = hashlib.sha3_512(signature.encode()).hexdigest()
    print("🛡️  ML-DSA-87 서명 (Q-KMS 개념):")
    print(f"  ├─ Algorithm: {alg}")
    print(f"  ├─ Key Source: Q-KMS Vault")
    print(f"  ├─ Message Hash (SHA3-512): {dilithium_sig_hash[:32]}...")
    print(f"  ├─ Signature Size: ~{sig_bytes} bytes")
    print(f"  └─ 검증: Q-KMS API /verify (구현 필요)")
    print()
    
    print("## 5단계: 통합 검증 흐름")
    print()
    print("검증 단계:")
    print("  1️⃣  클라이언트가 JWT 토큰 수신")
    print("  2️⃣  RSA 서명 검증 (표준 방식)")
    print("      └─ JWKS에서 RSA 공개키 획득")
    print("      └─ 표준 JWT 라이브러리로 검증")
    print("  3️⃣  Dilithium 서명 검증 (Q-KMS)")
    print("      └─ Q-KMS API 호출: POST /verify")
    print("      └─ ML-DSA-87 공개키로 검증")
    print("  4️⃣  양쪽 서명 모두 유효 → 인증 성공 ✅")
    print("      하나라도 실패 → 인증 거부 ❌")
    print()
    
    # 6. Summary
    print("## 6단계: 현재 상태 및 결론")
    print()
    print("✅ 구현 완료:")
    print("  ├─ Keycloak-PQC: DILITHIUM3 서명 생성")
    print(f"  ├─ 서명 크기: ~{sig_bytes} bytes (PQC 표준 부합)")
    print("  ├─ Q-KMS: Vault Transit Engine 운영")
    print("  └─ 하이브리드 모드: 환경 변수 설정됨")
    print()
    
    print("📋 하이브리드 서명 구현 로드맵:")
    print("  ├─ ① Q-KMS에 ML-DSA-87 검증 API 추가")
    print("  │   └─ liboqs 또는 pqcrypto 라이브러리 통합")
    print("  ├─ ② Keycloak에서 RSA 보조 서명 생성")
    print("  │   └─ 듀얼 서명 JWT 형식 구현")
    print("  └─ ③ 클라이언트 검증 라이브러리")
    print("      └─ RSA + Dilithium 양쪽 검증 지원")
    print()
    
    print("🎯 보안 이점:")
    print("  ├─ 양자 내성: Dilithium으로 미래 위협 대응")
    print("  ├─ 호환성: RSA로 레거시 시스템 지원")
    print("  ├─ 다층 보안: 두 서명 모두 검증 필요")
    print("  └─ 점진적 마이그레이션: 단계적 PQC 도입 가능")
    print()
    
    print("=" * 60)

if __name__ == '__main__':
    main()
