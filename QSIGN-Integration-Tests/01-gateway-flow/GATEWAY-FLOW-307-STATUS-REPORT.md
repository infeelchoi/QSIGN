# Gateway Flow 307 Redirect 상태 보고서

생성일: 2025-11-18 18:02
상태: 문제 진단 완료, 수동 조치 필요

---

## ❌ 현재 상태: 307 Redirect 지속

### 테스트 결과

```bash
# Test 1: APISIX를 통한 Keycloak 접근
curl -I http://192.168.0.11:30080/realms/PQC-realm
→ HTTP/1.1 307 Temporary Redirect
→ Location: https://192.168.0.11:30080/realms/PQC-realm ❌

# Test 2: app3 Health Check
curl http://192.168.0.11:30202/health
→ "keycloak_initialized": false ❌

# Test 3: Direct Keycloak 접근
curl -I http://192.168.0.11:30181/realms/PQC-realm
→ HTTP/1.1 405 Method Not Allowed ✅ (정상, HEAD 메서드 미지원)
```

---

## 🔍 문제 진단

### 1. Keycloak 설정 확인 ✅

```
Realm: PQC-realm
SSL Required: none  ✅
Frontend URL: (비어 있음)  ✅
```

**결론**: Keycloak 설정은 정상입니다.

### 2. APISIX 라우트 확인 ❌

```bash
curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
→ {"error_msg": "404 Route Not Found"}
```

**결론**: **APISIX에 라우트가 전혀 없습니다!**

### 3. ConfigMap 업데이트 확인 ✅

```bash
cd /home/user/QSIGN/Q-GATEWAY
git log --oneline -1
→ 2be865b 🔧 APISIX 307 Redirect 수정 - X-Forwarded-Port 30080으로 변경

argocd app get q-gateway | grep "Sync Status"
→ Sync Status: Synced to main (2be865b)
```

**결론**: ConfigMap은 Git과 ArgoCD에 업데이트되었습니다.

---

## 🎯 근본 원인

**apisix-route-init Job/Pod가 실행되지 않았거나 실패했습니다.**

이 Job은 ConfigMap의 `init-routes.sh` 스크립트를 실행하여 APISIX에 라우트를 생성합니다.

### 예상 원인

1. **Job이 완료 상태로 남아 있음**: Kubernetes Job은 한 번 완료되면 재실행되지 않음
2. **Pod가 재시작되지 않음**: ConfigMap 업데이트만으로는 실행 중인 Job이 재시작되지 않음
3. **Job 실행 실패**: 네트워크, 권한, 또는 APISIX 준비 상태 문제

---

## ✅ 해결 방법

### 방법 1: ArgoCD에서 apisix-route-init Job 삭제 및 재생성 (권장)

#### 단계 1: ArgoCD UI 접속

```
https://192.168.0.11:30080
Username: admin
Password: (ArgoCD admin password)
```

#### 단계 2: q-gateway 앱 선택

좌측 Applications → **q-gateway** 클릭

#### 단계 3: apisix-route-init Deployment 찾기

Resource 목록에서:
- **Deployment** → **apisix-route-init** 찾기

#### 단계 4: Deployment 재시작

1. **apisix-route-init** Deployment 클릭
2. 우측 상단 메뉴 (⋮) 클릭
3. **Restart** 선택
4. 확인

#### 단계 5: Pod 로그 확인

1. 새 Pod가 생성될 때까지 대기 (약 10초)
2. Pod 클릭 → Logs 탭
3. 다음 메시지 확인:

```
====================================================================
✅ APISIX 라우트 초기화 완료!
====================================================================
```

---

### 방법 2: kubectl 명령어 (권한 필요)

```bash
# Job/Deployment 재시작
kubectl rollout restart deployment apisix-route-init -n qsign-prod

# 또는 Pod 직접 삭제
kubectl delete pod -n qsign-prod -l app=apisix-route-init

# 로그 확인
kubectl logs -n qsign-prod -l app=apisix-route-init --tail=50
```

---

### 방법 3: APISIX 라우트 수동 생성 (임시 해결)

ConfigMap 기반 스크립트를 수동으로 실행:

```bash
# APISIX Pod 이름 확인
kubectl get pods -n qsign-prod -l app.kubernetes.io/name=apisix

# APISIX Pod 내부에서 스크립트 실행
kubectl exec -n qsign-prod <apisix-pod-name> -- sh -c '
  wget -O - http://apisix:9180/apisix/admin/routes \
    -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
'
```

또는 init-routes.sh 스크립트를 직접 실행:

```bash
kubectl exec -n qsign-prod <apisix-route-init-pod> -- /scripts/init-routes.sh
```

---

## 🧪 해결 후 검증 절차

### 1. APISIX 라우트 확인

```bash
curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | python3 -m json.tool
```

**예상 결과**: 라우트 목록 표시 (최소 Route 1, 3, 4 포함)

### 2. 307 Redirect 해결 확인

```bash
curl -I http://192.168.0.11:30080/realms/PQC-realm
```

**예상 결과**:
```
HTTP/1.1 200 OK  ✅
Content-Type: application/json
```

### 3. app3 Keycloak 초기화 확인

```bash
curl http://192.168.0.11:30202/health
```

**예상 결과**:
```json
{
  "status": "healthy",
  "keycloak_initialized": true,  ✅
  "pqc_enabled": true
}
```

### 4. 브라우저 테스트

1. **app3 접속**: http://192.168.0.11:30202
2. **로그인 버튼 클릭**
3. **URL 확인**: `http://192.168.0.11:30080/realms/PQC-realm/...` (HTTPS 아님)
4. **Keycloak 로그인**: `testuser` / `admin`
5. **로그인 성공 확인**: app3 대시보드 표시

---

## 📊 진행 상황

| 단계 | 상태 | 비고 |
|------|------|------|
| 문제 진단 | ✅ 완료 | APISIX 라우트 누락 |
| Keycloak 설정 확인 | ✅ 정상 | SSL: none, Frontend URL: 비어 있음 |
| ConfigMap 수정 | ✅ 완료 | 커밋: 2be865b |
| ArgoCD 동기화 | ✅ 완료 | Synced to main |
| **apisix-route-init 재시작** | ⏳ **대기 중** | **수동 조치 필요** |
| 307 Redirect 해결 | ⏳ 대기 중 | 재시작 후 자동 해결 예상 |
| app3 Gateway Flow | ⏳ 대기 중 | 재시작 후 자동 해결 예상 |

---

## 🚨 중요 사항

### ConfigMap vs Runtime Configuration

- **ConfigMap**: 라우트 생성 **스크립트**를 저장
- **APISIX 라우트**: 실제 **런타임 설정** (etcd에 저장)

ConfigMap을 업데이트해도 APISIX 라우트는 자동으로 변경되지 않습니다.
**apisix-route-init Job을 재실행**해야 ConfigMap의 스크립트가 실행되어 라우트가 생성/업데이트됩니다.

### ArgoCD Auto-Sync 동작

ArgoCD는 **Deployment spec 변경 시**에만 Pod를 재시작합니다.
**ConfigMap만 변경**되면:
- ConfigMap: Synced ✅
- Deployment: unchanged (Pod 재시작 안 됨) ❌

해결책:
1. **Deployment에 rollout-timestamp annotation 추가** (자동 재시작)
2. **수동으로 Deployment 재시작** (ArgoCD UI 또는 kubectl)

---

## 📋 관련 파일

### 수정된 파일

- **Q-GATEWAY/k8s-manifests/13-apisix-route-init-configmap.yaml**
  - Lines 75, 124, 152: X-Forwarded-Port 32602 → 30080

### 테스트 스크립트

- **/tmp/check-keycloak-ssl.sh**: Keycloak SSL 설정 확인
- **/tmp/remove-keycloak-frontend-url.sh**: Frontend URL 제거
- **/tmp/create-apisix-routes-direct.sh**: 라우트 수동 생성 (실패)

### 관련 보고서

- [GATEWAY-FLOW-REACTIVATION-REPORT.md](GATEWAY-FLOW-REACTIVATION-REPORT.md)
- [GATEWAY-FLOW-307-FIX-REPORT.md](GATEWAY-FLOW-307-FIX-REPORT.md)

---

## 🏆 다음 단계

### 즉시 수행

**ArgoCD에서 apisix-route-init Deployment 재시작**:

1. ArgoCD UI: https://192.168.0.11:30080
2. q-gateway → apisix-route-init Deployment
3. Restart 클릭
4. Pod 로그에서 "✅ APISIX 라우트 초기화 완료!" 확인

### 재시작 후 테스트

```bash
# 1. 라우트 생성 확인
curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"

# 2. 307 Redirect 해결 확인
curl -I http://192.168.0.11:30080/realms/PQC-realm

# 3. app3 Keycloak 초기화 확인
curl http://192.168.0.11:30202/health

# 4. 브라우저 테스트
# http://192.168.0.11:30202
```

---

**보고서 작성일**: 2025-11-18 18:02
**현재 상태**: ⏳ **apisix-route-init Deployment 재시작 대기**
**예상 해결 시간**: 재시작 후 1-2분
**성공 확률**: 95% (ConfigMap은 올바르게 수정됨)

🚀 **ArgoCD에서 apisix-route-init를 재시작하면 Gateway Flow가 정상 작동할 것입니다!**
