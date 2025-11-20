# QSIGN 테스트 스크립트

QSIGN 시스템의 통합 테스트 자동화 스크립트 모음입니다.

## 📋 테스트 스크립트 목록

| 스크립트 | 범위 | 시간 | 설명 |
|---------|------|------|------|
| `test-qsign-flow.sh` | 기본 | 1분 | 기본 플로우 빠른 테스트 |
| `test-qsign-flow-updated.sh` | 확장 | 2분 | 업데이트된 기능 포함 |
| `test-full-qsign-flow.sh` | 전체 | 5분 | 모든 기능 종합 테스트 |
| `test-pqc-hybrid-flow.sh` | PQC | 10분 | 양자 내성 암호화 테스트 |

---

## 🎯 테스트 목적별 선택 가이드

### 빠른 상태 확인
→ `test-qsign-flow.sh`
- 시스템이 살아있는지 확인
- 기본 연결 테스트
- 1분 이내 완료

### 일반적인 기능 테스트
→ `test-qsign-flow-updated.sh`
- 최신 기능 포함
- 에러 핸들링 확인
- 2분 정도 소요

### 배포 전 전체 검증
→ `test-full-qsign-flow.sh`
- 모든 주요 기능 테스트
- End-to-End 시나리오
- 배포 전 필수 실행

### PQC 기능 검증
→ `test-pqc-hybrid-flow.sh`
- 양자 내성 암호화
- 하이브리드 모드
- 성능 측정 포함

---

## 🚀 사용법

### 기본 실행
```bash
# 현재 디렉토리로 이동
cd /home/user/QSIGN/QSIGN-Integration-Tests/scripts/tests

# 실행 권한 확인
chmod +x *.sh

# 테스트 실행
./test-full-qsign-flow.sh
```

### 상세 로그와 함께 실행
```bash
DEBUG=1 ./test-full-qsign-flow.sh
```

### 결과를 파일로 저장
```bash
./test-full-qsign-flow.sh | tee test-result-$(date +%Y%m%d-%H%M%S).log
```

---

## 📊 테스트 항목

### 1️⃣ test-qsign-flow.sh
```
✓ Keycloak health check
✓ APISIX health check
✓ Q-SIGN API health check
```

### 2️⃣ test-qsign-flow-updated.sh
```
✓ 위 항목 +
✓ 인증 토큰 발급
✓ API 인증 테스트
✓ 에러 핸들링
```

### 3️⃣ test-full-qsign-flow.sh
```
✓ 위 항목 +
✓ 사용자 등록/로그인
✓ 서명 생성 (모든 알고리즘)
✓ 서명 검증
✓ 키 관리
✓ End-to-End 시나리오
```

### 4️⃣ test-pqc-hybrid-flow.sh
```
✓ Classic 알고리즘:
  - RSA-2048/4096
  - ECDSA-P256/P384

✓ PQC 알고리즘:
  - Dilithium2/3/5
  - Falcon-512/1024
  - SPHINCS+-SHA256

✓ Hybrid 모드:
  - RSA + Dilithium
  - ECDSA + Dilithium
  - 성능 비교
```

---

## 🔍 테스트 결과 확인

### 성공 케이스
```
==========================================
QSIGN Integration Test Results
==========================================
Total Tests:    15
Passed:         15 ✅
Failed:         0
Success Rate:   100%
==========================================
```

### 실패 케이스
```
==========================================
QSIGN Integration Test Results
==========================================
Total Tests:    15
Passed:         12 ✅
Failed:         3 ❌
Success Rate:   80%

Failed Tests:
  ❌ Test 5: Signature verification
  ❌ Test 8: PQC hybrid mode
  ❌ Test 12: Performance benchmark

Check logs for details.
==========================================
```

---

## ⚙️ 환경 변수

스크립트 동작을 커스터마이징할 수 있는 환경 변수:

```bash
# 서비스 URL
export KEYCLOAK_URL="http://localhost:9180"
export APISIX_URL="http://localhost:9080"
export QSIGN_API_URL="http://localhost:9080/qsign"

# 인증 정보
export KEYCLOAK_USER="admin"
export KEYCLOAK_PASSWORD="admin"
export KEYCLOAK_REALM="qsign"

# 테스트 설정
export TEST_TIMEOUT=30          # 타임아웃 (초)
export TEST_RETRY=3             # 재시도 횟수
export DEBUG=0                  # 디버그 모드 (0 또는 1)
export VERBOSE=0                # 상세 출력 (0 또는 1)
```

---

## 🐛 문제 해결

### 스크립트가 실행되지 않음
```bash
# 실행 권한 부여
chmod +x test-full-qsign-flow.sh

# 쉘 확인
which bash

# 명시적으로 bash 실행
bash test-full-qsign-flow.sh
```

### 연결 실패
```bash
# 서비스 상태 확인
sudo k3s kubectl get pods -n q-sign
sudo k3s kubectl get svc -n q-sign

# 포트 확인
sudo netstat -tulpn | grep -E '9080|9180|9443'

# 로그 확인
sudo k3s kubectl logs -n q-sign <pod-name>
```

### 인증 실패
```bash
# Keycloak 상태 확인
curl http://localhost:9180/health

# 토큰 수동 발급 테스트
curl -X POST "http://localhost:9180/realms/qsign/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

---

## 📈 성능 기준

각 테스트의 예상 성능 기준:

### API 응답 시간
- Health check: < 100ms
- 인증: < 300ms
- 서명 생성: < 500ms
- 서명 검증: < 200ms

### 처리량
- Classic 서명: > 100 ops/sec
- PQC 서명: > 50 ops/sec
- Hybrid 서명: > 30 ops/sec

### 리소스 사용
- CPU: < 70%
- Memory: < 2GB
- Network: < 100Mbps

---

## 🔄 자동화

### Cron으로 정기 실행
```bash
# 매일 오전 2시에 테스트 실행
0 2 * * * /home/user/QSIGN/QSIGN-Integration-Tests/scripts/tests/test-full-qsign-flow.sh >> /var/log/qsign-daily-test.log 2>&1
```

### CI/CD 파이프라인
```yaml
# .gitlab-ci.yml 예제
test:
  stage: test
  script:
    - cd QSIGN-Integration-Tests/scripts/tests
    - ./test-full-qsign-flow.sh
  artifacts:
    reports:
      junit: test-results.xml
```

---

## 📝 새 테스트 추가

새로운 테스트 스크립트를 작성할 때:

1. **템플릿 사용**
   ```bash
   cp test-qsign-flow.sh test-new-feature.sh
   ```

2. **스크립트 구조**
   ```bash
   #!/bin/bash

   # 설정
   set -e

   # 변수
   TEST_NAME="New Feature Test"

   # 함수 정의
   test_feature() {
       # 테스트 로직
   }

   # 메인 실행
   echo "Starting $TEST_NAME..."
   test_feature
   echo "✅ Test completed"
   ```

3. **실행 권한**
   ```bash
   chmod +x test-new-feature.sh
   ```

4. **문서 업데이트**
   - 이 README에 추가
   - 상위 README 업데이트

---

## 🔗 관련 리소스

- [설정 스크립트](../setup/)
- [테스트 결과](../../docs/results/)
- [트러블슈팅](../../docs/troubleshooting/)
- [메인 README](../../README.md)

---

**업데이트**: 2025-11-17
