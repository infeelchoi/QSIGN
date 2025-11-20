# Q-SIGN ArgoCD 문제 해결 가이드

## 📊 현재 상태

### ✅ 실제 시스템 상태: 정상 작동 중!

```
테스트 결과 (2025-11-17 11:17):

Component                      Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Q-KMS Vault (8200)             ✓ PASS
Q-SIGN Keycloak (30181)        ✓ PASS  ← 정상 작동!
Q-GATEWAY APISIX (80)          ○ RUNNING
Q-APP (30300)                  ✓ PASS
```

**검증 완료**:
- ✅ Q-SIGN Keycloak이 Port 30181에서 응답 중
- ✅ Realm 'myrealm' 접근 가능
- ✅ Frontend URL: http://192.168.0.11:30181 (올바름)
- ✅ Q-APP → Q-SIGN 연결 정상
- ✅ SSO 로그인 플로우 준비 완료

### ⚠️ ArgoCD UI 상태: 문제 표시 가능

**가능한 ArgoCD 상태**:
- 🔄 Progressing (진행 중)
- ❤️ Degraded (품질 저하)
- ⏸️ Pod Pending (대기 중)

**원인**:
1. ArgoCD가 최신 커밋(8b493fb)을 아직 인식하지 못함
2. Pending 상태의 이전 Pod가 삭제되지 않음
3. 새 Pod 생성 시도가 계속 실패 중

---

## 🔍 문제 진단

### Git 커밋 상태

```
✓ Repository: http://192.168.0.11:7780/root/q-sign.git
✓ Branch: main
✓ 최신 커밋:
  - 8b493fb: Remove hostNetwork (최신)
  - 792054c: Fix image configuration
```

### ArgoCD가 보고 있는 커밋 확인

**ArgoCD UI에서**:
1. q-sign 애플리케이션 클릭
2. "APP DETAILS" 또는 상단 정보 확인
3. "LAST SYNC" 또는 "Synced to" 확인
   - ✅ **8b493fb**이면 최신 상태
   - ⚠️ **792054c**이면 REFRESH 필요

---

## 🛠️ 해결 방법

### 방법 1: REFRESH + SYNC (가장 간단) ⭐

**단계**:

1. **ArgoCD UI 접속**
   ```
   http://192.168.0.11:30080
   ```

2. **q-sign 애플리케이션 선택**
   - Applications 화면에서 "q-sign" 카드 클릭

3. **REFRESH 버튼 클릭**
   - 상단 툴바에서 "REFRESH" 버튼 찾기
   - 클릭하여 Git 저장소에서 최신 변경사항 가져오기
   - 커밋이 **8b493fb**로 업데이트되는지 확인

4. **SYNC 버튼 클릭**
   - "SYNC" 버튼 클릭
   - Sync 옵션:
     - ✅ **PRUNE** (사용하지 않는 리소스 제거)
     - ✅ **FORCE** (강제 동기화)
     - ✅ **REPLACE** (리소스 교체)
   - "SYNCHRONIZE" 버튼 클릭

5. **Sync 진행 관찰**
   - 기존 pending Pod 삭제됨
   - 새로운 keycloak-pqc Pod 생성
   - Pod 상태: Pending → Running
   - Health: Progressing → Healthy

6. **완료 확인**
   - Health Status: ✅ **Healthy**
   - Sync Status: ✅ **Synced**
   - Commit: 8b493fb

---

### 방법 2: Pending Pod 수동 삭제

ArgoCD UI를 통한 Pod 삭제:

1. **Resource 화면으로 이동**
   - q-sign 애플리케이션 화면에서
   - 왼쪽 또는 하단의 리소스 트리 보기

2. **Pending Pod 찾기**
   - `keycloak-pqc-XXXXXXX` 형태의 Pod
   - 상태가 "Pending" 또는 "ImagePullBackOff"인 것

3. **Pod 삭제**
   - Pod 우클릭 → **Delete**
   - 또는 Pod 선택 → 상단 Delete 버튼
   - 삭제 확인

4. **새 Pod 생성 대기**
   - Kubernetes가 자동으로 새 Pod 생성
   - Deployment의 replicas 설정에 따라 자동 재생성
   - Pod 상태: Pending → ContainerCreating → Running

5. **검증**
   - Pod Ready: 1/1
   - Status: Running
   - Restarts: 0

---

### 방법 3: 전체 재동기화 (HARD REFRESH)

완전히 새로 시작하기:

1. **ArgoCD UI에서 q-sign 선택**

2. **APP DETAILS 버튼 클릭**
   - 상단 또는 우측 상단의 "APP DETAILS" 버튼

3. **SYNC POLICY 확인**
   - Auto-Sync가 활성화되어 있는지 확인
   - 비활성화되어 있다면 활성화 권장

4. **HARD REFRESH 실행**
   - "REFRESH" 버튼 옆 드롭다운 클릭
   - "Hard Refresh" 선택
   - Git 캐시 무효화 및 완전 재스캔

5. **SYNC with REPLACE**
   - "SYNC" 버튼 클릭
   - Sync Options:
     - ✅ PRUNE
     - ✅ FORCE
     - ✅ **REPLACE**
     - ✅ **DRY RUN** (선택사항 - 먼저 미리보기)
   - "SYNCHRONIZE" 클릭

---

### 방법 4: 애플리케이션 재생성 (최후의 수단)

**⚠️ 주의**: 이 방법은 q-sign 애플리케이션을 완전히 삭제하고 재생성합니다.

1. **애플리케이션 삭제**
   - q-sign 애플리케이션 선택
   - APP DETAILS → DELETE
   - ⚠️ **Cascade** 옵션:
     - ✅ Cascade: 모든 리소스 삭제
     - ⬜ Cascade: ArgoCD 애플리케이션만 삭제 (리소스 유지)
   - 삭제 확인

2. **애플리케이션 재생성**

   ArgoCD UI에서:
   - "NEW APP" 버튼 클릭
   - 설정:
     ```yaml
     Application Name: q-sign
     Project: default
     Sync Policy: Automatic

     Repository URL: http://192.168.0.11:7780/root/q-sign.git
     Revision: main
     Path: helm/q-sign

     Cluster: in-cluster
     Namespace: q-sign
     ```
   - "CREATE" 버튼 클릭

3. **Auto-Sync 활성화**
   - 생성 후 자동으로 sync 시작
   - 또는 수동으로 SYNC 버튼 클릭

---

## 🧪 해결 후 검증

### 1. ArgoCD UI 확인

**예상 결과**:
```
Application: q-sign
Health:      ✅ Healthy
Sync:        ✅ Synced
Commit:      8b493fb (Remove hostNetwork)
Last Sync:   방금 전

Resources:
  ✅ Namespace: q-sign
  ✅ ConfigMap: keycloak-pqc-config
  ✅ Deployment: keycloak-pqc
  ✅ Pod: keycloak-pqc-XXXXXXX (Running 1/1)
  ✅ Service: keycloak-pqc
  ✅ Deployment: postgres-qsign
  ✅ Pod: postgres-qsign-XXXXXXX (Running 1/1)
  ✅ Service: postgres-qsign
```

### 2. 서비스 접근 테스트

```bash
# Keycloak Realm 확인
curl -s http://192.168.0.11:30181/realms/myrealm | python3 -c "import sys,json; d=json.load(sys.stdin); print('Realm:', d.get('realm')); print('Token Service:', d.get('token-service'))"
```

**예상 출력**:
```
Realm: myrealm
Token Service: http://192.168.0.11:30181/realms/myrealm/protocol/openid-connect
```

### 3. 전체 플로우 테스트

```bash
/home/user/QSIGN/test-full-qsign-flow.sh
```

**예상 결과**:
```
Component                      Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Q-KMS Vault (8200)             ✓ PASS
Q-SIGN Keycloak (30181)        ✓ PASS
Q-GATEWAY APISIX (80)          ○ RUNNING
Q-APP (30300)                  ✓ PASS
```

### 4. SSO 로그인 테스트

**브라우저에서**:
1. http://192.168.0.11:30300 접속
2. "Login" 버튼 클릭
3. Q-SIGN Keycloak (30181)로 리디렉션
4. 로그인: testuser / admin
5. Q-APP로 리디렉션 및 사용자 정보 표시

---

## 🔧 여전히 문제가 있는 경우

### Pod가 CrashLoopBackOff인 경우

**Pod 로그 확인**:
- ArgoCD UI → keycloak-pqc Pod → Logs 탭

**일반적인 원인**:
1. **PostgreSQL 연결 실패**
   - postgres-qsign Service 확인
   - DB 연결 정보 확인 (KC_DB_URL)

2. **이미지 Pull 실패**
   - Image: localhost:7800/qsign-prod/keycloak-hsm:v1.2.0-hybrid
   - 레지스트리 접근 확인

3. **Health Check 실패**
   - Health endpoints: /health/live, /health/ready (port 9000)
   - Keycloak 시작 시간 부족 (startupProbe failureThreshold 증가 필요)

### PostgreSQL Pod 문제

**확인사항**:
- postgres-qsign Pod 상태: Running?
- PVC (Persistent Volume Claim) 바인딩: Bound?
- 로그 확인: 초기화 오류?

### Auto-Sync가 작동하지 않는 경우

**수동 Sync 설정**:
1. q-sign APP DETAILS
2. SYNC POLICY 섹션
3. "ENABLE AUTO-SYNC" 버튼 클릭
4. 옵션:
   - ✅ PRUNE RESOURCES
   - ✅ SELF HEAL

---

## 📊 중요 커밋 이력

### 변경사항 요약

**Commit 1: 792054c** (2025-11-17 11:00)
```
🔧 Fix Q-SIGN Keycloak image configuration

문제: ImagePullBackOff
수정:
  - repository: localhost:7800/qsign-prod/keycloak-hsm
  - tag: v1.2.0-hybrid
  - pullPolicy: IfNotPresent
```

**Commit 2: 8b493fb** (2025-11-17 11:10)
```
🔧 Remove hostNetwork from Q-SIGN Keycloak deployment

문제: Pod Pending (hostNetwork 충돌)
수정:
  - hostNetwork: true 제거
  - dnsPolicy: ClusterFirst
```

---

## ✅ 체크리스트

완료 확인:

- [ ] ArgoCD UI에서 q-sign 상태 확인
- [ ] 현재 커밋이 8b493fb인지 확인
- [ ] REFRESH 버튼 클릭하여 최신 커밋 가져오기
- [ ] SYNC 버튼 클릭 (PRUNE + FORCE)
- [ ] Sync 진행 완료 대기
- [ ] Health Status: Healthy 확인
- [ ] Pod 상태: Running (1/1) 확인
- [ ] 서비스 테스트: curl http://192.168.0.11:30181/realms/myrealm
- [ ] 전체 플로우 테스트 실행
- [ ] SSO 로그인 브라우저 테스트

---

## 🎯 예상 최종 상태

### ArgoCD

```
┌─────────────────────────────────────┐
│  q-sign Application                 │
│                                     │
│  Health:      ✅ Healthy            │
│  Sync:        ✅ Synced             │
│  Commit:      8b493fb               │
│  Last Sync:   방금 전               │
│                                     │
│  Resources: 8/8                     │
│  ├─ keycloak-pqc (Running)          │
│  ├─ postgres-qsign (Running)        │
│  └─ ... (All Healthy)               │
└─────────────────────────────────────┘
```

### 서비스

```
Q-SIGN Keycloak
  Port:        30181 ✅
  Status:      Running ✅
  Image:       keycloak-hsm:v1.2.0-hybrid ✅
  Realm:       myrealm ✅
  Frontend:    http://192.168.0.11:30181 ✅
  HostNetwork: false ✅
```

### 플로우

```
Q-APP (30300)
  ↓ OIDC Redirect
Q-SIGN Keycloak (30181)
  ↓ HSM Integration
Q-KMS Vault (8200)

모든 연결: ✅ 정상
```

---

**생성 시각**: 2025-11-17 11:20
**적용 커밋**: 8b493fb
**테스트 상태**: ✅ PASS (모든 컴포넌트 정상)
**ArgoCD 상태**: ⚠️ UI 확인 필요 → REFRESH + SYNC 실행
