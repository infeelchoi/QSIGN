# app3 → Q-GATEWAY(APISIX) → Q-SIGN 플로우 설정 가이드

## 📋 개요

현재 **Direct Flow**를 사용 중입니다:
```
app3 (30202) ──▶ Keycloak (30181) ✅ 현재 작동 중
```

**Gateway Flow**로 전환하려면:
```
app3 (30202) ──▶ APISIX (32602) ──▶ Keycloak (내부)
```

---

## 🔍 현재 상태 분석

### ✅ 작동 중인 Direct Flow
- **app3 → Keycloak 직접 연결**: `http://192.168.0.11:30181`
- **장점**: 간단한 구조, 직접 연결
- **단점**: APISIX의 보안/로깅/라우팅 기능 미사용

### ⚠️ Gateway Flow 요구사항
1. **APISIX Keycloak 프록시 라우트 설정**
2. **app3 환경 변수 변경**
3. **APISIX 외부 접근 포트 확인**

---

## 🛠️ 필요한 조치

### 1️⃣ APISIX에 Keycloak 라우트 설정

APISIX가 Keycloak 요청을 프록시하도록 라우트를 추가해야 합니다.

**필요한 APISIX 라우트:**
```json
{
  "uri": "/realms/*",
  "name": "keycloak-realms-proxy",
  "methods": ["GET", "POST"],
  "upstream": {
    "type": "roundrobin",
    "nodes": {
      "keycloak.q-sign.svc.cluster.local:8080": 1
    }
  },
  "plugins": {
    "proxy-rewrite": {
      "regex_uri": ["^/realms/(.*)", "/realms/$1"]
    }
  }
}
```

**APISIX 라우트 설정 스크립트:**
```bash
#!/bin/bash

# APISIX Admin API로 Keycloak 라우트 추가
APISIX_ADMIN_KEY="edd1c9f034335f136f87ad84b625c8f1"

# Keycloak realms 경로 프록시
curl -X PUT "http://apisix.q-sign.svc.cluster.local:9180/apisix/admin/routes/keycloak-realms" \
  -H "X-API-KEY: $APISIX_ADMIN_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "uri": "/realms/*",
    "name": "keycloak-realms-proxy",
    "methods": ["GET", "POST"],
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "keycloak.q-sign.svc.cluster.local:8080": 1
      }
    }
  }'

# Keycloak OIDC 토큰 엔드포인트 프록시
curl -X PUT "http://apisix.q-sign.svc.cluster.local:9180/apisix/admin/routes/keycloak-token" \
  -H "X-API-KEY: $APISIX_ADMIN_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "uri": "/realms/*/protocol/openid-connect/*",
    "name": "keycloak-oidc-proxy",
    "methods": ["GET", "POST"],
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "keycloak.q-sign.svc.cluster.local:8080": 1
      }
    }
  }'
```

---

### 2️⃣ APISIX 외부 접근 포트 확인

**Q-GATEWAY가 사용하는 포트:**
- **Admin API (내부)**: `9180`
- **외부 접근 포트**: `32602` (values.yaml 주석 참고)

**확인 방법:**
```bash
# APISIX 서비스 확인
kubectl get svc -n q-sign apisix -o yaml

# NodePort 확인
kubectl get svc -n q-sign apisix -o jsonpath='{.spec.ports[?(@.name=="http")].nodePort}'
```

---

### 3️⃣ app3 환경 변수 변경 (values.yaml)

**현재 설정 (Direct Flow):**
```yaml
global:
  keycloakUrl: "http://192.168.0.11:30181"          # Keycloak 직접 연결
  keycloakPublicUrl: "http://192.168.0.11:30181"
```

**Gateway Flow 설정 (변경 필요):**
```yaml
global:
  # Gateway Flow: app3 → APISIX → Keycloak
  keycloakUrl: "http://apisix.q-sign.svc.cluster.local:9080"  # 내부: Pod → APISIX
  keycloakPublicUrl: "http://192.168.0.11:32602"              # 외부: 브라우저 → APISIX
```

**변경 방법:**
```bash
cd /home/user/QSIGN/Q-APP

# values.yaml 수정
vim k8s/helm/q-app/values.yaml

# 변경:
# keycloakUrl: "http://192.168.0.11:30181"
# → keycloakUrl: "http://192.168.0.11:32602"

# Git 커밋
git add k8s/helm/q-app/values.yaml
git commit -m "🔧 app3 Gateway Flow 활성화 (APISIX 경유)"
git push

# ArgoCD 배포
argocd app sync q-app
```

---

### 4️⃣ APISIX 서비스 외부 노출 확인

**APISIX가 NodePort로 외부 노출되어 있는지 확인:**

```bash
# APISIX 서비스 타입 확인
kubectl get svc -n q-sign apisix

# 예상 출력:
# NAME     TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)
# apisix   NodePort   10.43.xxx.xxx   <none>        9080:32602/TCP
```

**만약 NodePort가 없다면:**
```bash
# APISIX 서비스를 NodePort로 변경
kubectl patch svc apisix -n q-sign -p '{"spec":{"type":"NodePort"}}'

# 또는 Helm values 수정
```

---

## 🧪 테스트 방법

### **1단계: APISIX 라우트 동작 확인**

```bash
# Keycloak realms 경로 테스트 (APISIX 경유)
curl -I http://192.168.0.11:32602/realms/PQC-realm

# 예상 결과: HTTP 200 또는 302 (Keycloak 응답)
```

### **2단계: app3 로그인 테스트**

```bash
# app3 접속
http://192.168.0.11:30202

# 로그인 클릭 → Keycloak 리다이렉트 URL 확인
# ✅ 올바른 URL: http://192.168.0.11:32602/realms/PQC-realm/...
# ❌ 이전 URL: http://192.168.0.11:30181/realms/PQC-realm/...
```

### **3단계: app3 로그 확인**

```bash
kubectl logs -n q-app -l app=app3 --tail=30

# 예상 로그:
# URL: http://192.168.0.11:32602/realms/PQC-realm  ← APISIX 경유!
```

---

## 📊 Direct Flow vs Gateway Flow 비교

| 항목 | Direct Flow (현재) | Gateway Flow |
|------|-------------------|-------------|
| **경로** | app3 → Keycloak | app3 → APISIX → Keycloak |
| **포트** | 30181 | 32602 |
| **장점** | 간단, 직접 연결 | 보안, 로깅, 라우팅 중앙화 |
| **단점** | APISIX 기능 미사용 | 설정 복잡 |
| **사용 시기** | 개발/테스트 | 프로덕션 |

---

## ⚠️ 주의사항

1. **APISIX 라우트 설정 필수**: Keycloak 경로를 프록시하지 않으면 404 에러 발생
2. **브라우저 캐시**: 기존 Keycloak URL이 캐시되어 있을 수 있으므로 로그아웃 필수
3. **내부/외부 URL 분리**:
   - `keycloakUrl` (내부): Pod에서 APISIX 접근 (`http://apisix.q-sign.svc.cluster.local:9080`)
   - `keycloakPublicUrl` (외부): 브라우저에서 APISIX 접근 (`http://192.168.0.11:32602`)

---

## 🚀 빠른 시작 (Gateway Flow 활성화)

```bash
# 1. APISIX 라우트 설정 (스크립트 작성 필요)
# TODO: APISIX Admin API 접근 가능한 경우

# 2. values.yaml 수정
cd /home/user/QSIGN/Q-APP
vim k8s/helm/q-app/values.yaml

# global.keycloakUrl 변경:
# "http://192.168.0.11:30181" → "http://192.168.0.11:32602"

# 3. Git 커밋 및 배포
git add k8s/helm/q-app/values.yaml
git commit -m "🔧 Gateway Flow 활성화"
git push
argocd app sync q-app

# 4. 테스트
curl -I http://192.168.0.11:32602/realms/PQC-realm
```

---

## 📝 다음 단계

**Direct Flow 유지 권장 (현재 상태):**
- app3 → Keycloak 직접 연결이 이미 정상 작동 중입니다
- DILITHIUM3 PQC 토큰도 정상 발급됩니다
- Gateway Flow는 필요 시 추가로 설정할 수 있습니다

**Gateway Flow가 필요한 경우:**
1. APISIX를 통한 중앙 로깅/모니터링
2. Rate limiting, Authentication 플러그인 사용
3. 여러 백엔드 서비스에 대한 통합 Gateway

---

## 🔗 관련 문서

- Q-GATEWAY 설정: `/home/user/QSIGN/Q-GATEWAY/README.md`
- APISIX 라우트 설정: [APISIX Docs](https://apisix.apache.org/docs/apisix/admin-api/)
- values.yaml 주석 참고: `# Gateway Flow (future): keycloakUrl: "http://192.168.0.11:32602"`