# QSIGN 통합 테스트 (QSIGN Integration Tests)

QSIGN 프로젝트의 통합 테스트, 문서, 트러블슈팅 가이드를 체계적으로 관리하는 디렉토리입니다.

## 📁 디렉토리 구조

```
QSIGN-Integration-Tests/
├── README.md                   # 이 문서
├── 01-gateway-flow/           # Gateway Flow 테스트
│   ├── README.md              # Gateway Flow 상세 가이드
│   ├── 문서 (8개)             # 상태 보고서, 트러블슈팅
│   └── 스크립트 (4개)         # 설정 및 테스트 스크립트
├── 02-direct-flow/            # Direct Flow 테스트
│   └── test-scripts...
├── 03-pqc-hybrid/             # PQC Hybrid 테스트
│   └── pqc-test-scripts...
├── 04-docs/                   # 문서 모음
│   ├── 01-guides/             # 가이드 문서 (8개)
│   ├── 02-results/            # 테스트 결과 및 완료 보고서 (5개)
│   ├── 03-app-reports/        # App 수정 보고서 (5개)
│   ├── 04-troubleshooting/    # 트러블슈팅 문서
│   ├── QSIGN-FULL-ARCHITECTURE-FLOW.md
│   └── PQC-HYBRID-SSO-COMPLETE.md
└── 05-scripts/                # 스크립트 모음
    ├── 01-setup/              # 설정 스크립트 (7개)
    ├── 02-tests/              # 테스트 스크립트 (6개)
    └── 03-cleanup/            # 클린업 스크립트 (3개)
```

---

## 🏗️ 새로운 테스트 카테고리 (2025-11-17 추가)

### 🌐 Gateway Flow (01-gateway-flow/)

**Q-APP → Q-GATEWAY (APISIX) → Q-SIGN → Q-KMS** 아키텍처 테스트

| 파일명 | 설명 |
|--------|------|
| [README.md](01-gateway-flow/README.md) | Gateway Flow 통합 가이드 |
| [setup-apisix-pqc-routes-30080.sh](01-gateway-flow/setup-apisix-pqc-routes-30080.sh) | APISIX 라우트 설정 (NodePort 30080) |
| [init-apisix-pqc-routes.sh](01-gateway-flow/init-apisix-pqc-routes.sh) | APISIX 라우트 초기화 (Admin API 9180) |
| [setup-gateway-proxy.sh](01-gateway-flow/setup-gateway-proxy.sh) | Nginx 기반 Gateway (대안) |

**주요 기능:**
- APISIX API Gateway 통합 테스트
- ArgoCD 기반 APISIX 설정 관리
- Rate Limiting, CORS, 모니터링 통합
- SkyWalking APM 연동

**현재 상태**: ✅ **완료 (100%)** - 포트 32602에서 정상 작동 중

**Architecture**: Q-APP (30300) → Q-GATEWAY/APISIX (32602) → Q-SIGN (30181) → Q-KMS (8200)

**주요 문서**:
- [Gateway Flow 성공 보고서](01-gateway-flow/GATEWAY-FLOW-SUCCESS.md) ⭐
- [트러블슈팅 가이드](01-gateway-flow/TROUBLESHOOTING-HTTP-REDIRECT.md)
- [테스트 스크립트](01-gateway-flow/test-gateway-flow.sh)

### 📡 Direct Flow (02-direct-flow/)

**Q-APP → Q-SIGN → Q-KMS** 직접 연결 아키텍처 테스트

**주요 기능:**
- Q-SIGN 직접 접근 테스트 (Port 30181)
- PQC-realm 응답 검증
- SSO 로그인 Flow 테스트

**현재 상태**: ✅ 완전 작동 (100%)

### 🔐 PQC Hybrid (03-pqc-hybrid/)

Post-Quantum Cryptography Hybrid 암호화 테스트

**주요 기능:**
- DILITHIUM3 + RS256 Hybrid Signature
- KYBER1024 + X25519 Hybrid KEM
- Vault Transit Engine PQC 통합

**현재 상태**: ✅ SSO 기본 Flow 완료, Hybrid Token 구현 대기 (50%)

---

## 📚 문서 (04-docs/)

### 🎯 가이드 (guides/)

운영 및 관리를 위한 실무 가이드 문서입니다.

| 파일명 | 설명 |
|--------|------|
| [DELETE-ERROR-POD-GUIDE.md](04-docs/01-guides/DELETE-ERROR-POD-GUIDE.md) | 에러 상태의 Pod를 안전하게 삭제하는 가이드 |
| [Q-APP-SYNC-GUIDE.md](04-docs/01-guides/Q-APP-SYNC-GUIDE.md) | Q-APP ArgoCD 동기화 가이드 |
| [Q-SIGN-FIX-GUIDE.md](04-docs/01-guides/Q-SIGN-FIX-GUIDE.md) | Q-SIGN 문제 해결 종합 가이드 |
| [APISIX-DASHBOARD-ROUTE-GUIDE.md](04-docs/01-guides/APISIX-DASHBOARD-ROUTE-GUIDE.md) | APISIX 대시보드 라우팅 가이드 |
| [PQC-REALM-SETUP-COMPLETE.md](04-docs/01-guides/PQC-REALM-SETUP-COMPLETE.md) | PQC Realm 설정 완료 가이드 |
| [REPLICASET-CLEANUP-GUIDE.md](04-docs/01-guides/REPLICASET-CLEANUP-GUIDE.md) | ReplicaSet 정리 가이드 |
| [KUBERNETES-LOGS-GUIDE.md](04-docs/01-guides/KUBERNETES-LOGS-GUIDE.md) | Kubernetes 로그 확인 종합 가이드 |
| [NODE-LOGGING-GUIDE.md](04-docs/NODE-LOGGING-GUIDE.md) | ⭐ QSIGN 노드별 로그 확인 가이드 (App5→APISIX→Keycloak→Vault) |

### 🔍 트러블슈팅 (troubleshooting/)

발생 가능한 문제와 해결 방법을 문서화한 트러블슈팅 가이드입니다.

| 파일명 | 설명 |
|--------|------|
| [Q-SIGN-ARGOCD-TROUBLESHOOT.md](04-docs/04-troubleshooting/Q-SIGN-ARGOCD-TROUBLESHOOT.md) | ArgoCD 관련 트러블슈팅 |
| [Q-SIGN-FINAL-FIX.md](04-docs/04-troubleshooting/Q-SIGN-FINAL-FIX.md) | 최종 수정 사항 문서 |
| [Q-SIGN-PENDING-FIX.md](04-docs/04-troubleshooting/Q-SIGN-PENDING-FIX.md) | Pending 상태 Pod 문제 해결 |
| [Q-SIGN-RESTORE-COMPLETE.md](04-docs/04-troubleshooting/Q-SIGN-RESTORE-COMPLETE.md) | 복구 완료 보고서 |

### ✅ 테스트 결과 (results/)

통합 테스트 실행 결과 및 배포 완료 보고서입니다.

| 파일명 | 설명 |
|--------|------|
| [QSIGN-COMPLETE-SUCCESS.md](04-docs/02-results/QSIGN-COMPLETE-SUCCESS.md) | QSIGN 전체 성공 보고서 |
| [QSIGN-DEPLOYMENT-COMPLETE.md](04-docs/02-results/QSIGN-DEPLOYMENT-COMPLETE.md) | QSIGN 배포 완료 보고서 |
| [QSIGN-INTEGRATION-TEST-RESULT.md](04-docs/02-results/QSIGN-INTEGRATION-TEST-RESULT.md) | 통합 테스트 결과 상세 보고서 |
| [FINAL-REPORT.md](04-docs/02-results/FINAL-REPORT.md) | 최종 종합 보고서 |

### 📱 App 보고서 (app-reports/)

개별 App 수정 및 배포 보고서입니다.

| 파일명 | 설명 |
|--------|------|
| [APP1-APP2-DISABLED.md](04-docs/03-app-reports/APP1-APP2-DISABLED.md) | App1, App2 비활성화 보고서 |
| [APP4-FIX-REPORT.md](04-docs/03-app-reports/APP4-FIX-REPORT.md) | App4 수정 보고서 |
| [APP5-CONFIG-FIX-REPORT.md](04-docs/03-app-reports/APP5-CONFIG-FIX-REPORT.md) | App5 설정 수정 보고서 |
| [APP5-DEPLOYMENT-REPORT.md](04-docs/03-app-reports/APP5-DEPLOYMENT-REPORT.md) | App5 배포 보고서 |
| [APP5-ERROR-FIX-REPORT.md](04-docs/03-app-reports/APP5-ERROR-FIX-REPORT.md) | App5 에러 수정 보고서 |

### 🏛️ 아키텍처 문서

| 파일명 | 설명 |
|--------|------|
| [QSIGN-FULL-ARCHITECTURE-FLOW.md](04-docs/QSIGN-FULL-ARCHITECTURE-FLOW.md) | QSIGN 전체 아키텍처 및 Flow 비교 |
| [PQC-HYBRID-SSO-COMPLETE.md](04-docs/PQC-HYBRID-SSO-COMPLETE.md) | PQC Hybrid SSO 구현 완료 보고서 |

---

## 🔧 스크립트 (scripts/)

### ⚙️ 설정 스크립트 (setup/)

시스템 설정 및 초기화 스크립트입니다.

| 파일명 | 설명 | 사용법 |
|--------|------|--------|
| [fix-keycloak-frontend-url.sh](05-scripts/01-setup/fix-keycloak-frontend-url.sh) | Keycloak Frontend URL 수정 | `./fix-keycloak-frontend-url.sh` |
| [create-all-app-clients.sh](05-scripts/01-setup/create-all-app-clients.sh) | 전체 App 클라이언트 생성 | `./create-all-app-clients.sh` |
| [create-app7-client.sh](05-scripts/01-setup/create-app7-client.sh) | App7 클라이언트 생성 | `./create-app7-client.sh` |
| [create-pqc-realm-client.sh](05-scripts/01-setup/create-pqc-realm-client.sh) | PQC Realm 클라이언트 생성 | `./create-pqc-realm-client.sh` |
| [create-sso-client.sh](05-scripts/01-setup/create-sso-client.sh) | SSO 클라이언트 생성 | `./create-sso-client.sh` |
| [setup-apisix-gateway.sh](05-scripts/01-setup/setup-apisix-gateway.sh) | APISIX Gateway 설정 | `./setup-apisix-gateway.sh` |
| [restart-keycloak.sh](05-scripts/01-setup/restart-keycloak.sh) | Keycloak 재시작 | `./restart-keycloak.sh` |

**주요 기능:**
- Keycloak 클라이언트 자동 생성
- APISIX Gateway 라우팅 설정
- QSIGN 통합을 위한 환경 설정

### 🧪 테스트 스크립트 (tests/)

QSIGN 시스템의 다양한 통합 테스트 스크립트입니다.

| 파일명 | 설명 | 사용법 |
|--------|------|--------|
| [test-qsign-flow.sh](05-scripts/02-tests/test-qsign-flow.sh) | 기본 QSIGN 플로우 테스트 | `./test-qsign-flow.sh` |
| [test-qsign-flow-updated.sh](05-scripts/02-tests/test-qsign-flow-updated.sh) | 업데이트된 QSIGN 플로우 테스트 | `./test-qsign-flow-updated.sh` |
| [test-full-qsign-flow.sh](05-scripts/02-tests/test-full-qsign-flow.sh) | 전체 QSIGN 플로우 통합 테스트 | `./test-full-qsign-flow.sh` |
| [test-pqc-hybrid-flow.sh](05-scripts/02-tests/test-pqc-hybrid-flow.sh) | PQC 하이브리드 모드 테스트 | `./test-pqc-hybrid-flow.sh` |
| [test-app3-qsign-integration.sh](05-scripts/02-tests/test-app3-qsign-integration.sh) | App3 통합 테스트 | `./test-app3-qsign-integration.sh` |
| [test-qsign-integration.sh](05-scripts/02-tests/test-qsign-integration.sh) | QSIGN 통합 테스트 | `./test-qsign-integration.sh` |

**테스트 커버리지:**
- ✅ Keycloak 인증 플로우
- ✅ APISIX Gateway 라우팅
- ✅ Q-SIGN API 통합
- ✅ PQC (Post-Quantum Cryptography) 하이브리드 암호화
- ✅ End-to-End 서명 및 검증
- ✅ App별 개별 통합 테스트

### 🧹 클린업 스크립트 (cleanup/)

시스템 정리 및 유지보수 스크립트입니다.

| 파일명 | 설명 | 사용법 |
|--------|------|--------|
| [cleanup-all-replicasets.sh](05-scripts/03-cleanup/cleanup-all-replicasets.sh) | 모든 오래된 ReplicaSet 정리 | `./cleanup-all-replicasets.sh` |
| [cleanup-replicasets.sh](05-scripts/03-cleanup/cleanup-replicasets.sh) | ReplicaSet 선택적 정리 | `./cleanup-replicasets.sh` |
| [list-old-replicasets.sh](05-scripts/03-cleanup/list-old-replicasets.sh) | 오래된 ReplicaSet 목록 조회 | `./list-old-replicasets.sh` |

**주요 기능:**
- 오래된 ReplicaSet 자동 감지
- 안전한 정리 프로세스
- ArgoCD 동기화 후 남은 리소스 관리

### 📊 로그 수집 스크립트

통합 로그 수집 및 분석 스크립트입니다.

| 파일명 | 설명 | 사용법 |
|--------|------|--------|
| ⭐ [collect-qsign-logs.sh](05-scripts/collect-qsign-logs.sh) | **QSIGN 전체 로그 수집** | `./collect-qsign-logs.sh` |

**주요 기능:**
- App5, APISIX, Keycloak, Vault 로그 자동 수집
- Pod 상태, 서비스 정보, 이벤트 수집
- APISIX 라우트 및 Vault 상태 조회
- 자동 압축 및 요약 리포트 생성
- 옵션: `-t <N>` (로그 라인 수), `-o <DIR>` (출력 디렉토리)

**예제:**
```bash
# 기본 설정 (500줄)
./collect-qsign-logs.sh

# 1000줄 수집
./collect-qsign-logs.sh -t 1000

# 특정 디렉토리에 저장
./collect-qsign-logs.sh -o /var/log/qsign
```

---

## 🚀 빠른 시작

### 1. Direct Flow 테스트 (권장 - 작동 중)

Q-SIGN 직접 접근 테스트:

```bash
# Q-SIGN 상태 확인
curl http://192.168.0.11:30181/realms/PQC-realm

# Q-APP에서 SSO 로그인
# http://192.168.0.11:30300 접속
# "Login with Keycloak" → testuser / Test1234!
```

### 2. Gateway Flow 설정 (진행 중)

APISIX Gateway를 통한 테스트:

```bash
# ArgoCD UI에서 q-gateway 확인
# https://192.168.0.11:30443

# APISIX 라우트 설정
cd QSIGN-Integration-Tests/gateway-flow
./setup-apisix-pqc-routes-30080.sh

# 상세 가이드
cat 01-gateway-flow/README.md
```

### 3. 전체 시스템 테스트

전체 QSIGN 시스템을 테스트하려면:

```bash
cd QSIGN-Integration-Tests/scripts/tests
./test-full-qsign-flow.sh
```

### 4. PQC 하이브리드 테스트

양자 내성 암호화 하이브리드 모드를 테스트하려면:

```bash
cd QSIGN-Integration-Tests/scripts/tests
./test-pqc-hybrid-flow.sh
```

### 5. Keycloak 설정 수정

Keycloak Frontend URL을 수정해야 하는 경우:

```bash
cd QSIGN-Integration-Tests/scripts/setup
./fix-keycloak-frontend-url.sh
```

---

## 📋 테스트 체크리스트

통합 테스트 실행 전 확인 사항:

- [ ] K3s 클러스터가 정상 실행 중
- [ ] ArgoCD가 정상 동작 중
- [ ] Keycloak이 정상 실행 중 (포트 9180)
- [ ] APISIX Gateway가 정상 실행 중 (포트 9080, 9443)
- [ ] Q-SIGN 서비스가 배포됨
- [ ] 필요한 네임스페이스가 생성됨 (`q-sign`, `keycloak`, `apisix`)

---

## 🐛 트러블슈팅

문제가 발생하면 다음 순서로 확인하세요:

1. **Pod 상태 확인**
   ```bash
   sudo k3s kubectl get pods -n q-sign
   ```

2. **서비스 상태 확인**
   ```bash
   sudo k3s kubectl get svc -n q-sign
   ```

3. **로그 확인**
   ```bash
   sudo k3s kubectl logs -n q-sign <pod-name>
   ```

4. **트러블슈팅 문서 참조**
   - [04-docs/04-troubleshooting/](04-docs/04-troubleshooting/)의 관련 문서 확인

### Gateway Flow 주요 이슈 (2025-11-17) - ✅ 해결 완료

#### Issue: HTTP → HTTPS Redirect (307) - **해결됨**
**증상**: `curl http://192.168.0.11:30080/` → 307 Redirect to HTTPS

**원인**: 포트 30080은 APISIX가 아니라 **ArgoCD HTTP**가 사용 중
- APISIX 실제 HTTP 포트: **32602**
- APISIX Admin API 포트: **30282**

**해결**:
1. ✅ APISIX 실제 포트 확인 (ArgoCD UI → q-gateway → Service)
2. ✅ Q-APP keycloakUrl을 `http://192.168.0.11:32602`로 업데이트
3. ✅ Git commit & push → ArgoCD 자동 동기화
4. ✅ 통합 테스트 실행: 5/5 통과

**상세**:
- [Gateway Flow 성공 보고서](01-gateway-flow/GATEWAY-FLOW-SUCCESS.md) ⭐
- [트러블슈팅 가이드](01-gateway-flow/TROUBLESHOOTING-HTTP-REDIRECT.md)

---

## 📊 테스트 결과 확인

테스트 실행 후 결과는 [04-docs/02-results/](04-docs/02-results/)에 문서화됩니다:

- **성공 케이스**: `QSIGN-COMPLETE-SUCCESS.md`
- **배포 상태**: `QSIGN-DEPLOYMENT-COMPLETE.md`
- **상세 결과**: `QSIGN-INTEGRATION-TEST-RESULT.md`

---

## 🔗 관련 프로젝트

- **QSIGN 메인**: 양자 내성 서명 플랫폼
- **Q-ADMIN**: QSIGN 관리 콘솔
- **Q-APP**: QSIGN 사용자 애플리케이션
- **Q-SSL**: 양자 내성 TLS/SSL 구현

---

## 📝 버전 정보

- **버전**: 1.4.0 (노드별 로깅 가이드 추가)
- **마지막 업데이트**: 2025-11-19
- **작성자**: QSIGN Team
- **주요 변경사항 (v1.4.0)**:
  - 📊 **QSIGN 노드별 로그 확인 가이드 추가** (NODE-LOGGING-GUIDE.md)
    - App5 → APISIX → Keycloak → Vault 전체 흐름 로그 확인 방법
    - 컴포넌트별 상세 로그 명령어 및 필터링 기법
    - 문제별 로그 확인 가이드 (로그인 실패, JWT 발급, 라우팅 문제 등)
    - 실시간 통합 모니터링 방법 (tmux/screen)
  - 🔧 **통합 로그 수집 스크립트 추가** (collect-qsign-logs.sh)
    - 전체 QSIGN 컴포넌트 로그 자동 수집
    - Pod 상태, 서비스, 이벤트 정보 수집
    - APISIX 라우트 및 Vault 상태 조회
    - 자동 압축 및 요약 리포트 생성
- **이전 변경사항**:
  - 🔢 폴더 구조 넘버링 체계 도입 (01-, 02-, 03-...) (v1.3.0)
  - 📝 Kubernetes 로그 확인 종합 가이드 추가 (v1.3.0)
  - 📁 문서 및 스크립트 체계적 재구성 (v1.2.0)
  - 📱 App 보고서 디렉토리 추가 (04-docs/03-app-reports/)
  - 🧹 클린업 스크립트 디렉토리 추가 (05-scripts/03-cleanup/)
  - 📚 가이드 문서 확대 (9개)
  - 🧪 테스트 스크립트 확대 (6개)
  - ⚙️ 설정 스크립트 확대 (7개)
  - 🌐 Gateway Flow 문서 통합 (12개)
  - ✨ Gateway Flow 테스트 카테고리 추가 (v1.1.0)
  - ✨ Direct Flow 테스트 카테고리 추가 (v1.1.0)
  - ✨ PQC Hybrid 테스트 카테고리 추가 (v1.1.0)

---

## 🤝 기여 가이드

새로운 테스트나 문서를 추가할 때:

1. **Gateway Flow 스크립트**는 `01-gateway-flow/`에 추가
2. **Direct Flow 스크립트**는 `02-direct-flow/`에 추가
3. **PQC Hybrid 스크립트**는 `03-pqc-hybrid/`에 추가
4. **일반 테스트 스크립트**는 `05-scripts/02-tests/`에 추가
5. **설정 스크립트**는 `05-scripts/01-setup/`에 추가
6. **클린업 스크립트**는 `05-scripts/03-cleanup/`에 추가
7. **가이드 문서**는 `04-docs/01-guides/`에 추가
8. **트러블슈팅 문서**는 `04-docs/04-troubleshooting/`에 추가
9. **테스트 결과**는 `04-docs/02-results/`에 추가
10. **App 관련 보고서**는 `04-docs/03-app-reports/`에 추가
11. **아키텍처 문서**는 `04-docs/`에 추가
12. 이 README를 업데이트하여 새 파일 정보 추가

---

## 📄 라이선스

QSIGN 프로젝트 라이선스를 따릅니다.

---

**QSIGN Integration Tests v1.0.0**
*Quantum-Safe Signature Platform Testing Suite*
