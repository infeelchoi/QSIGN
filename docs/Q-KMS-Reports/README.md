# Q-KMS 테스트 리포트

이 디렉토리는 Q-KMS 연동 및 테스트 관련 문서를 포함합니다.

## 문서 목록

- **keycloak-qkms-test-report.sh**: Keycloak-PQC ↔ Q-KMS 연동 테스트
  - Q-KMS Vault 상태 확인
  - Dilithium 키 검증
  - 토큰 생성 및 검증

## Q-KMS 정보

**Vault 버전**: 1.21.0
**Transit Engine**: 활성화
**현재 키**: dilithium-key (RSA-4096)

**향후 작업**:
- ML-DSA-87 검증 API 추가
- liboqs 라이브러리 통합

## 사용 방법

```bash
./keycloak-qkms-test-report.sh
```

## 작성일

2025-11-16
