# QSIGN 스크립트 모음

QSIGN 시스템의 설정, 테스트, 유틸리티 스크립트 모음입니다.

## 📁 디렉토리 구조

```
scripts/
├── README.md           # 이 문서
├── setup/             # 설정 및 초기화 스크립트
│   └── fix-keycloak-frontend-url.sh
└── tests/             # 테스트 스크립트
    ├── test-qsign-flow.sh
    ├── test-qsign-flow-updated.sh
    ├── test-full-qsign-flow.sh
    └── test-pqc-hybrid-flow.sh
```

---

## ⚙️ 설정 스크립트 (setup/)

시스템 초기 설정 및 구성 스크립트입니다.

### fix-keycloak-frontend-url.sh
**Keycloak Frontend URL 자동 수정**

Keycloak의 Frontend URL을 QSIGN 통합에 맞게 자동으로 설정합니다.

**사용법:**
```bash
cd setup
./fix-keycloak-frontend-url.sh
```

**주요 기능:**
- Keycloak Admin API를 통한 설정 변경
- Frontend URL 자동 감지 및 설정
- 설정 검증

**실행 조건:**
- Keycloak이 실행 중이어야 함
- Admin 권한 필요
- 네트워크 연결 필요

---

## 🧪 테스트 스크립트 (tests/)

QSIGN 시스템의 다양한 통합 테스트 스크립트입니다.

### 1. test-qsign-flow.sh
**기본 QSIGN 플로우 테스트**

QSIGN의 기본 동작을 테스트하는 스크립트입니다.

**사용법:**
```bash
cd tests
./test-qsign-flow.sh
```

**테스트 항목:**
- Keycloak 연결 확인
- APISIX Gateway 라우팅
- 기본 API 호출

**예상 실행 시간:** ~1분

---

### 2. test-qsign-flow-updated.sh
**업데이트된 QSIGN 플로우 테스트**

최신 기능을 포함한 업데이트된 테스트 스크립트입니다.

**사용법:**
```bash
cd tests
./test-qsign-flow-updated.sh
```

**테스트 항목:**
- 기본 플로우 + 추가 기능
- 에러 핸들링 검증
- 상태 코드 확인

**예상 실행 시간:** ~2분

---

### 3. test-full-qsign-flow.sh
**전체 QSIGN 플로우 통합 테스트**

QSIGN의 모든 주요 기능을 포괄적으로 테스트합니다.

**사용법:**
```bash
cd tests
./test-full-qsign-flow.sh
```

**테스트 항목:**
- ✅ Keycloak 인증 (OAuth2/OIDC)
- ✅ APISIX Gateway 라우팅
- ✅ Q-SIGN API (CRUD)
- ✅ 서명 생성 (Classic + PQC)
- ✅ 서명 검증
- ✅ End-to-End 시나리오

**예상 실행 시간:** ~5분

**출력:**
- 각 테스트 단계별 상태
- 성공/실패 카운트
- 상세 로그

---

### 4. test-pqc-hybrid-flow.sh
**PQC 하이브리드 암호화 테스트**

양자 내성 암호화(PQC)와 기존 암호화(Classic)의 하이브리드 모드를 테스트합니다.

**사용법:**
```bash
cd tests
./test-pqc-hybrid-flow.sh
```

**테스트 항목:**
- 🔐 Classic 서명 (RSA, ECDSA)
- 🛡️ PQC 서명 (Dilithium, Falcon, SPHINCS+)
- 🔗 Hybrid 서명 (Classic + PQC)
- ✅ 각 모드별 검증
- 📊 성능 측정

**예상 실행 시간:** ~10분

**PQC 알고리즘:**
- **Dilithium3**: NIST 표준 서명 알고리즘
- **Falcon-512**: 격자 기반 서명
- **SPHINCS+-SHA256**: 해시 기반 서명

---

## 🚀 빠른 시작

### 전체 시스템 빠른 테스트
```bash
cd tests
./test-full-qsign-flow.sh
```

### PQC 기능 테스트
```bash
cd tests
./test-pqc-hybrid-flow.sh
```

### Keycloak 설정 수정
```bash
cd setup
./fix-keycloak-frontend-url.sh
```

---

## 📋 테스트 전 체크리스트

스크립트 실행 전 다음 사항을 확인하세요:

- [ ] **K3s 클러스터** 실행 중
  ```bash
  sudo systemctl status k3s
  ```

- [ ] **Keycloak** 실행 중 (포트 9180)
  ```bash
  curl -s http://localhost:9180/health
  ```

- [ ] **APISIX** 실행 중 (포트 9080, 9443)
  ```bash
  curl -s http://localhost:9080/health
  ```

- [ ] **Q-SIGN** Pod 실행 중
  ```bash
  sudo k3s kubectl get pods -n q-sign
  ```

- [ ] **네임스페이스** 존재
  ```bash
  sudo k3s kubectl get ns q-sign keycloak apisix
  ```

---

## 🔧 스크립트 커스터마이징

### 환경 변수 설정

대부분의 테스트 스크립트는 환경 변수를 통해 설정할 수 있습니다:

```bash
# Keycloak 설정
export KEYCLOAK_URL="http://localhost:9180"
export KEYCLOAK_REALM="qsign"
export KEYCLOAK_CLIENT_ID="qsign-client"

# APISIX 설정
export APISIX_URL="http://localhost:9080"
export APISIX_ADMIN_KEY="your-admin-key"

# Q-SIGN 설정
export QSIGN_API_URL="http://localhost:9080/qsign"
```

### 스크립트 수정

스크립트를 직접 수정하려면:

1. 스크립트 백업
   ```bash
   cp test-full-qsign-flow.sh test-full-qsign-flow.sh.bak
   ```

2. 편집기로 열기
   ```bash
   vim test-full-qsign-flow.sh
   ```

3. 필요한 부분 수정

4. 실행 권한 확인
   ```bash
   chmod +x test-full-qsign-flow.sh
   ```

---

## 📊 스크립트 비교

| 스크립트 | 테스트 범위 | 실행 시간 | 용도 |
|---------|------------|----------|------|
| `test-qsign-flow.sh` | 기본 | ~1분 | 빠른 확인 |
| `test-qsign-flow-updated.sh` | 중간 | ~2분 | 일반 테스트 |
| `test-full-qsign-flow.sh` | 전체 | ~5분 | 배포 전 검증 |
| `test-pqc-hybrid-flow.sh` | PQC | ~10분 | PQC 기능 검증 |

---

## 🐛 트러블슈팅

### 스크립트 실행 권한 오류
```bash
chmod +x tests/*.sh
chmod +x setup/*.sh
```

### 연결 오류
```bash
# 서비스 상태 확인
sudo k3s kubectl get svc -A

# 포트 확인
sudo netstat -tulpn | grep -E '9080|9180|9443'
```

### 인증 오류
```bash
# Keycloak 토큰 확인
curl -X POST "http://localhost:9180/realms/qsign/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

---

## 📈 결과 해석

### 성공 케이스
```
✅ All tests passed (10/10)
✅ Performance within limits
✅ No errors detected
```

### 실패 케이스
```
❌ Test failed: Authentication (1/10)
⚠️ Warning: Slow response time
❌ Error: Connection refused
```

---

## 🔄 CI/CD 통합

### GitHub Actions 예제
```yaml
- name: Run QSIGN Tests
  run: |
    cd QSIGN-Integration-Tests/scripts/tests
    ./test-full-qsign-flow.sh
```

### GitLab CI 예제
```yaml
test:
  script:
    - cd QSIGN-Integration-Tests/scripts/tests
    - ./test-full-qsign-flow.sh
```

---

## 📝 로그 및 출력

### 로그 파일 위치
```
/tmp/qsign-test-*.log
/var/log/qsign/test-*.log
```

### 상세 로그 활성화
```bash
export DEBUG=1
./test-full-qsign-flow.sh
```

### 로그 수집
```bash
# 모든 테스트 로그 수집
tar -czf qsign-test-logs-$(date +%Y%m%d).tar.gz /tmp/qsign-test-*.log
```

---

## 🔗 관련 문서

- [테스트 결과](../docs/results/)
- [트러블슈팅 가이드](../docs/troubleshooting/)
- [운영 가이드](../docs/guides/)

---

## 📞 지원

스크립트 관련 문제가 있으면:

1. 로그 확인
2. [트러블슈팅 문서](../docs/troubleshooting/) 참조
3. GitHub Issues에 보고
4. 팀에 문의

---

**업데이트**: 2025-11-17
