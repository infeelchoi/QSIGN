# 하이브리드 서명 시스템

이 디렉토리는 RSA + Dilithium 하이브리드 서명 시스템 관련 문서를 포함합니다.

## 문서 목록

- **HYBRID_SIGNATURE_FINAL_REPORT.md**: 하이브리드 서명 시스템 최종 리포트
  - Keycloak RSA + Q-KMS ML-DSA-87 통합
  - 듀얼 서명 JWT 아키텍처
  - 구현 로드맵

- **final-hybrid-summary.md**: 하이브리드 토큰 테스트 요약
  - keycloak-pqc vs keycloak(pqc-sso) 비교
  - PQC 지원 현황

## 핵심 개념

### 하이브리드 서명

RSA (Classic) + Dilithium (PQC) 양쪽 서명을 사용하여:
- 양자 내성: Dilithium으로 미래 위협 대응
- 호환성: RSA로 레거시 시스템 지원
- 다층 보안: 두 서명 모두 검증 필요

## 작성일

2025-11-16
