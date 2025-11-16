# QSIGN 문서

QSIGN 프로젝트의 기술 문서 및 테스트 리포트 저장소입니다.

## 📁 디렉토리 구조

```
docs/
├── PQC-Analysis/              # PQC 검증 분석
│   ├── README.md
│   └── PQC_VERIFICATION_FINAL_REPORT.md
│
├── Hybrid-Signature/          # 하이브리드 서명 시스템
│   ├── README.md
│   ├── HYBRID_SIGNATURE_FINAL_REPORT.md
│   └── final-hybrid-summary.md
│
├── Test-Scripts/              # 테스트 스크립트
│   ├── README.md
│   ├── test-hybrid-token.sh
│   ├── final-hybrid-demo-v2.py
│   └── ... (기타 스크립트)
│
├── Q-KMS-Reports/             # Q-KMS 리포트
│   ├── README.md
│   └── keycloak-qkms-test-report.sh
│
├── ACCESS_INFO.md             # 접속 정보
├── FINAL_TEST_REPORT.md       # 최종 테스트 리포트
└── LUNA_SPI_INSTALLATION.md   # Luna SPI 설치 가이드
```

## 📚 주요 문서

### PQC (Post-Quantum Cryptography) 분석
- [PQC 검증 분석](PQC-Analysis/PQC_VERIFICATION_FINAL_REPORT.md)
  - Dilithium 서명 검증 위치 분석
  - JWKS 공개키 문제점
  - 해결 방안 제시

### 하이브리드 서명 시스템
- [하이브리드 서명 리포트](Hybrid-Signature/HYBRID_SIGNATURE_FINAL_REPORT.md)
  - RSA + Dilithium 통합 아키텍처
  - 듀얼 서명 JWT 설계
  - 구현 로드맵

### 테스트 & 검증
- [테스트 스크립트](Test-Scripts/)
  - 하이브리드 토큰 테스트
  - PQC 검증 분석 스크립트

### Q-KMS 연동
- [Q-KMS 테스트](Q-KMS-Reports/keycloak-qkms-test-report.sh)
  - Keycloak-PQC ↔ Q-KMS 연동
  - Vault Transit Engine 테스트

## 🔐 핵심 기술

- **PQC 알고리즘**: DILITHIUM3 (NIST Level 3)
- **하이브리드 서명**: RSA + Dilithium
- **키 관리**: Q-KMS (Vault)
- **인증**: Keycloak + PQC Provider

## 📊 시스템 구성

```
Keycloak-PQC (q-sign)
  ├─ PQC Provider: Bouncy Castle
  ├─ Algorithm: DILITHIUM3
  ├─ Q-KMS 연동: http://q-kms:8200
  └─ Hybrid Mode: Enabled

Q-KMS (q-kms)
  ├─ Vault: 1.21.0
  ├─ Transit Engine: Enabled
  └─ 향후: ML-DSA-87 API
```

## 🎯 프로젝트 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| DILITHIUM3 서명 생성 | ✅ | Keycloak-PQC |
| JWKS 공개키 배포 | ❌ | 개선 필요 |
| Q-KMS ML-DSA-87 API | ❌ | 구현 필요 |
| 하이브리드 서명 | 🔄 | 설계 완료 |

## 📅 최종 업데이트

2025-11-16

---

**문의**: QSIGN 프로젝트 팀
