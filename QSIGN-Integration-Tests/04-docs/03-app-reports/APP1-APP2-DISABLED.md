# app1, app2 비활성화 완료

생성일: 2025-11-18
작업: app1, app2 리소스 절약을 위한 비활성화

---

## ✅ 완료 내용

### values.yaml 수정
```yaml
# App1 - Angular Application (Port 4200) [비활성화]
app1:
  enabled: false  # true → false

# App2 - Angular Application (Port 4201) [비활성화]
app2:
  enabled: false  # true → false
```

### ArgoCD 배포
- **커밋**: `fc81269` - "🔧 app1, app2 비활성화"
- **Sync**: ArgoCD Prune 정책으로 자동 삭제
- **상태**: app1, app2 Deployment pruned ✅

---

## 📊 현재 실행 중인 앱

| 앱 | 상태 | 포트 | Realm | 암호화 | 용도 |
|----|------|------|-------|--------|------|
| app3 | ✅ 실행 중 | 30202 | PQC-realm | DILITHIUM3 (PQC) | PQC 테스트 |
| app4 | ✅ 실행 중 | 30203 | PQC-realm | RS256 (Classical) | Legacy 클라이언트 |
| app6 | ✅ 실행 중 | - | PQC-realm | - | HSM 검증 |
| app7 | ✅ 실행 중 | - | PQC-realm | - | - |
| sso-test-app | ✅ 실행 중 | - | PQC-realm | - | SSO 테스트 |

---

## 🔴 비활성화된 앱

| 앱 | 상태 | 이유 |
|----|------|------|
| app1 | ❌ 비활성화 | Angular 앱 - 사용 안 함 |
| app2 | ❌ 비활성화 | Angular 앱 - 사용 안 함 |

---

## 💾 리소스 절약

**절약된 리소스** (app1 + app2):
```
CPU Requests: 400m (200m × 2)
Memory Requests: 1Gi (512Mi × 2)
CPU Limits: 2000m (1000m × 2)
Memory Limits: 4Gi (2Gi × 2)
Replicas: 2
```

---

## 🔄 재활성화 방법

필요 시 다시 활성화:

```bash
# values.yaml 수정
cd /home/user/QSIGN/Q-APP/k8s/helm/q-app
# enabled: false → true 변경

# Git 커밋
git add values.yaml
git commit -m "app1, app2 재활성화"
git push

# ArgoCD 동기화
argocd app sync q-app
```

---

**작업 완료일**: 2025-11-18
**커밋**: fc81269
**상태**: ✅ **Complete**
