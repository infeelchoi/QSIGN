# Q-APP ArgoCD 동기화 가이드

## 변경사항 요약

✅ **Git 커밋 완료**: Q-APP Keycloak URL을 30181로 변경
- **Repository**: http://192.168.0.11:7780/root/q-app.git
- **Commit**: e6eecd1 🔧 Update Q-APP Keycloak URL to Q-SIGN (30181)
- **Branch**: main

## ArgoCD 동기화 방법

### 방법 1: ArgoCD UI (권장)

1. **ArgoCD 접속**
   ```
   http://192.168.0.11:30080
   ```

2. **q-app 애플리케이션 찾기**
   - 화면에서 "q-app" 카드 클릭

3. **동기화 실행**
   - 상단의 **"SYNC"** 버튼 클릭
   - 동기화 옵션 확인
   - **"SYNCHRONIZE"** 버튼 클릭

4. **동기화 완료 확인**
   - Status: Healthy ✓
   - Sync Status: Synced ✓
   - Last Sync: 방금 전

### 방법 2: Auto-Sync (자동)

ArgoCD가 Auto-Sync로 설정되어 있다면:
- **3분 이내** 자동으로 Git 변경사항 감지
- 자동으로 배포 수행

### 방법 3: ArgoCD CLI

```bash
# ArgoCD 로그인 (한 번만 필요)
argocd login 192.168.0.11:30080 --username admin --password <password> --insecure

# q-app 동기화
argocd app sync q-app

# 동기화 상태 확인
argocd app get q-app
```

## 동기화 후 확인사항

### 1. Pod 재시작 확인

동기화 후 다음 Pod들이 재시작됩니다:

```bash
kubectl get pods -n q-app
```

예상 Pod 목록:
- app1-xxx
- app2-xxx
- app3-xxx
- app4-xxx
- app6-xxx
- app7-xxx
- sso-test-app-xxx

### 2. 새로운 설정 확인

Pod 환경변수 확인:

```bash
# 예시: sso-test-app Pod 확인
kubectl get pod sso-test-app-xxx -n q-app -o yaml | grep KEYCLOAK_URL

# 예상 출력:
# - name: KEYCLOAK_URL
#   value: http://192.168.0.11:30181
```

### 3. 전체 플로우 테스트

```bash
/home/user/QSIGN/test-full-qsign-flow.sh
```

예상 결과:
```
Component                      Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Q-KMS Vault (8200)             ✓ PASS
Q-SIGN Keycloak (30181)        ✓ PASS
Q-GATEWAY APISIX (80)          ○ RUNNING
Q-APP (30300)                  ✓ PASS  ← 동기화 후
```

## QSIGN 전체 플로우

동기화 완료 후 다음 플로우가 작동합니다:

```
┌──────────────────────────────────────┐
│  Q-APP (모든 앱)                     │
│  ├─ app1 (30210)                     │
│  ├─ app2 (30201)                     │
│  ├─ app3 (30202)                     │
│  ├─ app4 (30203)                     │
│  ├─ app6 (30205)                     │
│  ├─ app7 (30207)                     │
│  └─ sso-test-app (30300)             │
└──────────┬───────────────────────────┘
           │
           │ Keycloak URL: 30181 ✓
           ↓
┌─────────────────┐
│  Q-GATEWAY      │  (선택사항)
│  APISIX (80)    │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Q-SIGN         │  Post-Quantum Auth
│  Keycloak       │
│  (30181)        │  Frontend URL: ✓ 30181
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Q-KMS          │  HSM Key Management
│  Vault (8200)   │  Status: Unsealed ✓
└─────────────────┘
```

## SSO 로그인 테스트

1. **브라우저에서 앱 접속**
   ```
   http://192.168.0.11:30300  (sso-test-app)
   http://192.168.0.11:30201  (app2)
   http://192.168.0.11:30202  (app3)
   ```

2. **Login 버튼 클릭**
   - Q-SIGN Keycloak 로그인 페이지로 리디렉션 (30181)

3. **인증**
   - Username: `testuser`
   - Password: `admin`

4. **로그인 성공**
   - 앱으로 리디렉션
   - JWT 토큰 발급 (PQC hybrid signature)
   - 사용자 정보 표시

## 문제 해결

### ArgoCD가 변경사항을 감지하지 못하는 경우

```bash
# Git 저장소 수동 갱신
argocd app get q-app --refresh

# 또는 UI에서 "REFRESH" 버튼 클릭
```

### Pod가 재시작되지 않는 경우

```bash
# 수동 재시작
kubectl rollout restart deployment -n q-app

# 특정 앱만 재시작
kubectl rollout restart deployment/sso-test-app -n q-app
```

### Keycloak URL이 여전히 30699인 경우

```bash
# ConfigMap 확인
kubectl get configmap -n q-app

# Secret 확인
kubectl get secret -n q-app

# Helm values 재적용
helm upgrade q-app /home/user/QSIGN/Q-APP/k8s/helm/q-app -n q-app
```

## 완료 체크리스트

- [ ] Git 푸시 완료 (✅ 완료)
- [ ] ArgoCD Sync 실행
- [ ] Pod 재시작 확인
- [ ] 환경변수 확인 (KEYCLOAK_URL=30181)
- [ ] SSO 로그인 테스트
- [ ] 전체 플로우 테스트 스크립트 실행

---

**생성 시각**: 2025-11-17 10:47
**Git Commit**: e6eecd1
**Status**: Ready for ArgoCD Sync
