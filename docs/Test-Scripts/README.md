# 테스트 스크립트

이 디렉토리는 PQC 및 하이브리드 서명 테스트를 위한 스크립트를 포함합니다.

## 스크립트 목록

### 하이브리드 토큰 테스트
- **test-hybrid-token.sh**: 하이브리드 토큰 생성 및 분석
- **final-hybrid-demo-v2.py**: 하이브리드 서명 데모 (Python)

### PQC 검증 분석
- **check-pqc-verification.sh**: PQC 서명 검증 위치 확인
- **analyze-pqc-verification-location.sh**: 상세 검증 위치 분석
- **detailed-token-analysis.sh**: 토큰 상세 분석

### SSO 테스트
- **analyze-pqc-sso-token.sh**: pqc-sso keycloak 토큰 분석

## 사용 방법

```bash
# 하이브리드 데모 실행
python3 final-hybrid-demo-v2.py

# 토큰 분석
./detailed-token-analysis.sh

# PQC 검증 확인
./check-pqc-verification.sh
```

## 작성일

2025-11-16
