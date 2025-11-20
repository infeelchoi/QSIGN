# Q-SIGN Degraded 상태 수정 가이드

## 🔍 문제 진단 결과

**ArgoCD 상태**: ❤️ Degraded 🟢 Synced

### 발견된 문제

**이미지 Pull 실패** - Q-SIGN Keycloak 이미지가 레지스트리에 존재하지 않음

```yaml
# 문제가 있던 설정 (helm/q-sign/values.yaml)
image:
  repository: 192.168.0.11:30800/qsign/keycloak-pqc
  tag: "v1.0.1-qkms"
  pullPolicy: Always
```

**증상**:
- Pod가 ImagePullBackOff 상태
- ArgoCD에서 Degraded 상태 표시
- 이미지를 레지스트리에서 pull할 수 없음

---

## ✅ 적용된 수정사항

### Git 커밋 완료

**Repository**: http://192.168.0.11:7780/root/q-sign.git
**Commit**: 792054c
**Branch**: main
**Message**: 🔧 Fix Q-SIGN Keycloak image configuration

### 변경 내용

```yaml
# 수정된 설정
image:
  repository: localhost:7800/qsign-prod/keycloak-hsm
  tag: "v1.2.0-hybrid"
  pullPolicy: IfNotPresent
```

**수정 이유**:
- `localhost:7800/qsign-prod/keycloak-hsm:v1.2.0-hybrid` 이미지는 실제로 존재함
- 이 이미지는 keycloak-hsm에서 이미 사용 중이며 정상 작동 확인됨
- 동일한 Keycloak PQC 기능을 제공함

---

## 🚀 ArgoCD Sync 실행 방법

### 방법 1: ArgoCD UI (권장)

1. **ArgoCD 접속**
   ```
   http://192.168.0.11:30080
   ```

2. **q-sign 애플리케이션 찾기**
   - 화면에서 "q-sign" 카드 클릭
   - 현재 상태: Degraded

3. **SYNC 실행**
   - 상단의 **"SYNC"** 버튼 클릭
   - Sync 옵션:
     - ✅ PRUNE (사용하지 않는 리소스 제거)
     - ✅ SELF HEAL (자동 복구)
   - **"SYNCHRONIZE"** 버튼 클릭

4. **Sync 진행 상황 확인**
   - Pod가 재생성되는 것을 확인
   - Keycloak Pod가 새로운 이미지로 재시작됨
   - 상태가 Healthy로 변경되는지 확인

5. **완료 확인**
   - Status: ✅ Healthy
   - Sync Status: ✅ Synced
   - Last Sync: 방금 전

### 방법 2: ArgoCD CLI

```bash
# ArgoCD 로그인 (필요시)
argocd login 192.168.0.11:30080 --username admin --password <password> --insecure

# q-sign 동기화
argocd app sync q-sign

# 동기화 상태 확인
argocd app get q-sign

# Pod 재시작 확인
argocd app wait q-sign --health
```

### 방법 3: Auto-Sync (자동)

ArgoCD가 Auto-Sync로 설정되어 있다면:
- **3분 이내** 자동으로 Git 변경사항 감지
- 자동으로 배포 수행
- Degraded → Healthy 상태로 자동 전환

---

## 🧪 Sync 후 검증

### 1. Pod 상태 확인

ArgoCD UI에서 또는 스크립트로 확인:

```bash
# Q-SIGN 서비스 테스트
curl -s http://192.168.0.11:30181/realms/myrealm | grep -q "myrealm" && echo "✓ Q-SIGN Keycloak running" || echo "✗ Not responding"
```

### 2. 전체 플로우 테스트

```bash
/home/user/QSIGN/test-full-qsign-flow.sh
```

**예상 결과**:
```
Component                      Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Q-KMS Vault (8200)             ✓ PASS
Q-SIGN Keycloak (30181)        ✓ PASS  ← 수정 후
Q-GATEWAY APISIX (80)          ○ RUNNING
Q-APP (30300)                  ✓ PASS
```

### 3. Keycloak 기능 확인

**Realm 접근 테스트**:
```bash
curl -s http://192.168.0.11:30181/realms/myrealm | python3 -c "import sys,json; d=json.load(sys.stdin); print('Realm:', d.get('realm')); print('Token Service:', d.get('token-service'))"
```

**예상 출력**:
```
Realm: myrealm
Token Service: http://192.168.0.11:30181/realms/myrealm/protocol/openid-connect
```

**OpenID Configuration**:
```bash
curl -s http://192.168.0.11:30181/realms/myrealm/.well-known/openid-configuration | python3 -c "import sys,json; d=json.load(sys.stdin); print('Issuer:', d.get('issuer')); print('Auth:', d.get('authorization_endpoint'))"
```

### 4. SSO 로그인 테스트

브라우저에서:
1. **Q-APP 접속**: http://192.168.0.11:30300
2. **Login 버튼 클릭**
3. **Q-SIGN으로 리디렉션**: http://192.168.0.11:30181/realms/myrealm/...
4. **로그인**: testuser / admin
5. **성공 확인**: 사용자 정보 표시

---

## 🔧 문제 해결

### Pod가 여전히 ImagePullBackOff인 경우

```bash
# Pod 삭제 (강제 재시작)
# ArgoCD UI에서 Keycloak Pod 우클릭 → Delete

# 또는 kubectl 사용 (접근 가능한 경우)
kubectl delete pod -n q-sign -l app=keycloak
```

### ArgoCD가 변경사항을 감지하지 못하는 경우

```bash
# Git 저장소 수동 갱신
argocd app get q-sign --refresh

# 또는 UI에서 "REFRESH" 버튼 클릭
```

### 이미지를 여전히 pull할 수 없는 경우

**이미지 레지스트리 확인**:
```bash
# localhost:7800 레지스트리 접근 확인
curl -s http://localhost:7800/v2/ && echo "✓ Registry accessible" || echo "✗ Registry not accessible"

# 이미지 확인
curl -s http://localhost:7800/v2/qsign-prod/keycloak-hsm/tags/list
```

**ImagePullSecrets 확인**:
- q-sign 네임스페이스에 적절한 imagePullSecrets가 있는지 확인
- 레지스트리 인증 정보가 필요할 수 있음

---

## 📊 변경 이력

### 2025-11-17 11:00 - Q-SIGN 이미지 수정

**문제**:
- ArgoCD Degraded 상태
- Keycloak Pod ImagePullBackOff
- 이미지 192.168.0.11:30800/qsign/keycloak-pqc:v1.0.1-qkms 없음

**수정**:
- 이미지를 작동하는 버전으로 변경
- localhost:7800/qsign-prod/keycloak-hsm:v1.2.0-hybrid
- keycloak-hsm에서 사용 중인 검증된 이미지

**Git**:
```
Commit: 792054c
Repository: http://192.168.0.11:7780/root/q-sign.git
Branch: main
Status: ✅ Pushed
```

---

## ✅ 완료 체크리스트

- [x] 문제 진단 완료
- [x] 이미지 설정 수정
- [x] Git 커밋 완료
- [x] GitLab 푸시 완료
- [ ] **ArgoCD Sync 실행** ← 다음 단계
- [ ] Pod 재시작 확인
- [ ] Healthy 상태 확인
- [ ] Keycloak 기능 테스트
- [ ] SSO 로그인 테스트

---

## 🎯 예상 결과

Sync 완료 후:

```
┌─────────────────┐
│   Q-SIGN        │  ✅ Healthy
│  Keycloak       │  Image: keycloak-hsm:v1.2.0-hybrid
│  (30181)        │  Status: Running
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  PostgreSQL     │  ✅ Running
│  (postgres)     │  DB: keycloak
└─────────────────┘
```

**ArgoCD 상태**:
- Health: ✅ Healthy (Degraded → Healthy)
- Sync: ✅ Synced
- Images: ✅ All pulled successfully

**서비스**:
- Q-SIGN Keycloak: http://192.168.0.11:30181 ✅
- Realm: myrealm ✅
- Frontend URL: 30181 ✅
- SSO Login: ✅ Working

---

**생성 시각**: 2025-11-17 11:00
**수정 커밋**: 792054c
**상태**: Ready for ArgoCD Sync
**다음 단계**: ArgoCD UI에서 SYNC 버튼 클릭
