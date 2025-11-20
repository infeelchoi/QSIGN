# Gateway Flow 활성화 - 권한 문제 해결 가이드

생성일: 2025-11-18
상태: ⚠️ **권한 문제로 인해 수동 조치 필요**

## 현재 상황

### ✅ 완료된 작업
1. **Q-APP values.yaml 수정 완료**
   - keycloakUrl: `30181` → `30080` (Gateway Flow)
   - Git 커밋 및 푸시 완료 (commit: 1f62241)

2. **PQC DILITHIUM3 설정 완료**
   - PQC-realm 기본 알고리즘: DILITHIUM3
   - app3-client 토큰: DILITHIUM3

### ❌ 권한 문제로 인한 차단
1. **kubectl 접근 불가**
   ```
   error: open /etc/rancher/k3s/k3s.yaml: permission denied
   ```

2. **ArgoCD CLI 연결 실패**
   ```
   gRPC connection not ready: context deadline exceeded
   ```

3. **APISIX Admin API 외부 접근 불가**
   ```
   404 Route Not Found (라우트 추가 시도 시)
   ```

### 현재 시스템 상태
- **APISIX 라우트**: 0개 (초기화 안 됨)
- **app3 Pod**: 구버전 실행 중 (keycloakUrl: 30181 사용 중)
- **Gateway Flow**: 비활성화 상태

---

## 🔧 해결 방법

### 방법 1: ArgoCD Web UI 사용 (권장)

#### 1단계: ArgoCD Web UI 접속
```bash
# ArgoCD 서비스 포트 확인 필요
# 일반적으로: http://192.168.0.11:<nodePort>
```

#### 2단계: q-gateway 앱 Sync
```
1. ArgoCD UI → Applications
2. "q-gateway" 클릭
3. "REFRESH" 버튼 클릭
4. "SYNC" 버튼 클릭
5. "SYNCHRONIZE" 클릭
```

**기대 효과**: `apisix-route-init` Deployment가 재시작되어 APISIX 라우트 초기화

#### 3단계: q-app 앱 Sync
```
1. ArgoCD UI → Applications
2. "q-app" 클릭
3. "REFRESH" 버튼 클릭
4. "SYNC" 버튼 클릭
5. "SYNCHRONIZE" 클릭
```

**기대 효과**: app3 Pod가 재시작되어 keycloakUrl: 30080 적용

#### 4단계: 검증
```bash
# 1. APISIX 라우트 확인
curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | grep keycloak

# 2. Keycloak 접근 테스트 (APISIX 경유)
curl -s http://192.168.0.11:30080/realms/PQC-realm | grep realm

# 3. app3 전체 통합 테스트
bash /home/user/QSIGN/test-app3-qsign-integration.sh
```

---

### 방법 2: sudo 권한으로 kubectl 사용

#### 1단계: kubectl 권한 설정
```bash
# 옵션 A: 현재 사용자에게 kubeconfig 접근 권한 부여
sudo chmod 644 /etc/rancher/k3s/k3s.yaml

# 옵션 B: sudo로 kubectl 명령 실행
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
```

#### 2단계: q-gateway route-init 재시작
```bash
sudo k3s kubectl rollout restart deployment/apisix-route-init -n qsign-prod
sudo k3s kubectl rollout status deployment/apisix-route-init -n qsign-prod
```

#### 3단계: APISIX 라우트 확인
```bash
# 30초 대기 (라우트 초기화 시간)
sleep 30

curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | \
  python3 -c "import sys, json; data = json.load(sys.stdin); print(f'Total routes: {len(data.get(\"list\", []))}')"
```

**기대 결과**: `Total routes: 10` 이상

#### 4단계: q-app app3 재시작
```bash
# app3 deployment의 rollout-timestamp가 자동으로 갱신되도록 강제 재배포
sudo k3s kubectl rollout restart deployment/app3 -n q-app
sudo k3s kubectl rollout status deployment/app3 -n q-app
```

#### 5단계: 검증
```bash
# app3 로그에서 Keycloak URL 확인
sudo k3s kubectl logs -n q-app deployment/app3 --tail=20 | grep -i keycloak

# 기대 출력: http://192.168.0.11:30080
```

---

### 방법 3: APISIX 라우트 수동 추가 (임시 방법)

APISIX의 Admin API에 직접 접근하여 라우트를 추가하는 방법입니다. 이 방법은 **임시적**이며, APISIX가 재시작되면 라우트가 사라집니다.

#### Keycloak Realms 프록시 라우트만 추가
```bash
# Keycloak 서비스 이름 확인 필요 (cluster 내부)
KEYCLOAK_SERVICE="keycloak.q-sign.svc.cluster.local:8080"

# APISIX Admin API 접근 (cluster 내부에서)
# 이 방법은 apisix pod 내부에서 실행해야 함
sudo k3s kubectl exec -n qsign-prod deployment/apisix-route-init -it -- sh
```

그 후:
```bash
curl -X PUT "http://apisix:9180/apisix/admin/routes/4" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "keycloak-realms-proxy",
    "uri": "/realms/*",
    "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    "upstream": {
      "type": "roundrobin",
      "scheme": "http",
      "pass_host": "pass",
      "nodes": {
        "keycloak.q-sign.svc.cluster.local:8080": 1
      }
    },
    "plugins": {
      "cors": {
        "allow_origins": "*",
        "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
        "allow_headers": "*"
      }
    },
    "status": 1
  }'
```

---

## 📊 검증 체크리스트

### APISIX 라우트 검증
- [ ] `/realms/*` 라우트 존재
- [ ] `/auth/*` 라우트 존재
- [ ] `/resources/*` 라우트 존재
- [ ] `/vault/*` 라우트 존재
- [ ] 총 라우트 수 10개 이상

### app3 설정 검증
- [ ] app3 Pod가 재시작됨
- [ ] app3 로그에 keycloakUrl: `http://192.168.0.11:30080` 표시
- [ ] app3 health에서 Keycloak 초기화 성공

### Gateway Flow 통합 테스트
- [ ] `http://192.168.0.11:30080/realms/PQC-realm` 접근 성공 (200 OK)
- [ ] app3 로그인 시 Keycloak 리다이렉트 URL이 30080 사용
- [ ] 로그인 후 DILITHIUM3 토큰 수신 확인
- [ ] `/home/user/QSIGN/test-app3-qsign-integration.sh` 성공률 100%

---

## 🔍 트러블슈팅

### 문제 1: APISIX 라우트가 계속 0개
**원인**: `apisix-route-init` Deployment가 실행되지 않음

**해결**:
```bash
# route-init pod 상태 확인
sudo k3s kubectl get pods -n qsign-prod -l app=apisix-route-init

# 로그 확인
sudo k3s kubectl logs -n qsign-prod -l app=apisix-route-init --tail=50

# 강제 재시작
sudo k3s kubectl delete pod -n qsign-prod -l app=apisix-route-init
```

### 문제 2: app3가 여전히 30181 사용
**원인**: Helm chart가 재배포되지 않음

**해결**:
```bash
# app3 deployment annotation 확인
sudo k3s kubectl get deployment app3 -n q-app -o yaml | grep -A5 annotations

# rollout-timestamp가 없거나 오래된 경우 강제 재시작
sudo k3s kubectl rollout restart deployment/app3 -n q-app
```

### 문제 3: Keycloak 서비스 이름 오류
**원인**: `keycloak.q-sign.svc.cluster.local` 서비스가 존재하지 않음

**확인**:
```bash
sudo k3s kubectl get svc -n q-sign | grep keycloak
```

**가능한 서비스 이름**:
- `keycloak.q-sign.svc.cluster.local`
- `keycloak-pqc.q-sign.svc.cluster.local`
- `keycloak.qsign-prod.svc.cluster.local`

---

## 📝 요약

현재 Gateway Flow 활성화는 **80% 완료**되었으나, 시스템 권한 문제로 인해 다음 두 가지 작업이 필요합니다:

1. **APISIX 라우트 초기화** (q-gateway sync 또는 apisix-route-init 재시작)
2. **app3 Pod 재시작** (q-app sync 또는 deployment 재시작)

**권장 방법**: ArgoCD Web UI에서 q-gateway → q-app 순서로 SYNC

**예상 소요 시간**: 5-10분

완료 후 `/home/user/QSIGN/test-app3-qsign-integration.sh`를 실행하여 Gateway Flow가 정상 작동하는지 확인하세요.