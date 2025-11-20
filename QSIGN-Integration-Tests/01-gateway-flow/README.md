# Gateway Flow Integration Tests

QSIGN Gateway Flow 통합 테스트 및 설정 스크립트

## 📁 목차

1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [스크립트 설명](#스크립트-설명)
4. [ArgoCD에서 APISIX 설정 확인](#argocd에서-apisix-설정-확인)
5. [테스트 방법](#테스트-방법)
6. [트러블슈팅](#트러블슈팅)

---

## 개요

Gateway Flow는 Q-APP이 Q-GATEWAY(APISIX)를 통해 Q-SIGN(Keycloak)에 접근하는 아키텍처입니다.

### Direct Flow vs Gateway Flow

```
Direct Flow (현재 작동):
Q-APP (30300) → Q-SIGN (30181) → Q-KMS (8200)

Gateway Flow (설정 중):
Q-APP (30300) → Q-GATEWAY/APISIX (30080) → Q-SIGN (30181) → Q-KMS (8200)
```

### Gateway Flow 장점

- ✅ **중앙 집중식 라우팅**: 모든 트래픽이 APISIX를 통해 관리
- ✅ **Rate Limiting**: APISIX 플러그인으로 요청 제한
- ✅ **CORS 관리**: 중앙에서 CORS 정책 관리
- ✅ **모니터링**: SkyWalking 통합으로 트래픽 가시성
- ✅ **확장성**: 새로운 서비스 추가가 용이

---

## 아키텍처

### 전체 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                      QSIGN Gateway Flow                      │
│           Q-APP → APISIX → Keycloak → Vault                 │
└─────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Q-APP      │         │  Q-GATEWAY   │         │   Q-SIGN     │
│              │         │   (APISIX)   │         │  (Keycloak)  │
│  Angular     │  HTTP   │              │  HTTP   │              │
│  SPA         ├────────>│  Port 30080  ├────────>│  Port 30181  │
│              │         │              │         │              │
│  Port 30300  │         │  Routes:     │         │  PQC-realm   │
│              │         │  /realms/*   │         │  testuser    │
│              │         │  /admin/*    │         │              │
└──────────────┘         └──────────────┘         └──────┬───────┘
                                                          │
                                                          │ Transit
                                                          │ Signature
                                                          ▼
                                                  ┌──────────────┐
                                                  │   Q-KMS      │
                                                  │   (Vault)    │
                                                  │              │
                                                  │  Port 8200   │
                                                  │              │
                                                  │  Transit:    │
                                                  │  - DILITHIUM3│
                                                  │  - KYBER1024 │
                                                  └──────────────┘
```

### APISIX 구성 요소

```
Kubernetes (K3s)
├── Namespace: default (or q-gateway)
│   ├── APISIX Deployment
│   │   ├── Image: apache/apisix:latest
│   │   ├── Port: 9080 (HTTP)
│   │   ├── Admin Port: 9180 (Admin API)
│   │   └── Config: /usr/local/apisix/conf/config.yaml
│   │
│   ├── APISIX Service
│   │   ├── Type: NodePort
│   │   ├── Port: 9080 → NodePort: 30080
│   │   └── Admin Port: 9180 (ClusterIP only)
│   │
│   ├── APISIX Route Init Deployment
│   │   ├── Image: curlimages/curl:8.5.0
│   │   ├── Function: Auto-initialize APISIX routes
│   │   └── Script: /scripts/init-routes.sh
│   │
│   └── etcd Deployment
│       ├── Image: bitnami/etcd:latest
│       └── Port: 2379 (APISIX config storage)
```

### APISIX 라우트 구조

```
APISIX Routes:
├── Route ID: 1 - keycloak-full-proxy
│   URI: /auth/*
│   Upstream: keycloak-pqc:8080
│   Plugin: proxy-rewrite
│
├── Route ID: 2 - vault-kms-route
│   URI: /vault/*
│   Upstream: vault:8200
│   Plugin: proxy-rewrite
│
├── Route ID: 3 - keycloak-resources-direct
│   URI: /resources/*
│   Upstream: keycloak-pqc:8080
│   Plugin: proxy-rewrite
│
├── Route ID: 4 - keycloak-realms-proxy (★ 주요)
│   URI: /realms/*
│   Upstream: keycloak-pqc:8080
│   Plugin: proxy-rewrite
│   Methods: GET, POST, PUT, DELETE, OPTIONS
│
└── App Routes (app1-route, app2-route, app3-route, app4-route, app5-route, web1, web2)
```

---

## 스크립트 설명

### 1. setup-apisix-pqc-routes-30080.sh

**목적**: APISIX NodePort (30080)를 통해 PQC-realm 라우트 설정

```bash
./setup-apisix-pqc-routes-30080.sh
```

**주요 기능**:
- APISIX Admin API: `http://192.168.0.11:30080/apisix/admin`
- Upstream 생성: `q-sign-keycloak` → `192.168.0.11:30181`
- Routes 생성:
  - `/realms/PQC-realm/*` → q-sign-keycloak
  - `/realms/*` → q-sign-keycloak
  - `/admin/*` → q-sign-keycloak

**사용 시나리오**:
- APISIX가 K3s 외부 NodePort로 노출된 경우
- Admin API에 직접 접근 가능한 경우

**현재 이슈**:
- ❌ HTTP → HTTPS 리다이렉트 발생 (307 Temporary Redirect)
- Admin API가 HTTPS로만 접근 가능한 것으로 보임

---

### 2. init-apisix-pqc-routes.sh

**목적**: APISIX Admin API (9180)를 통해 라우트 초기화

```bash
./init-apisix-pqc-routes.sh
```

**주요 기능**:
- APISIX Admin API: `http://192.168.0.11:9180/apisix/admin`
- APISIX 서버 준비 대기 (최대 30회 재시도)
- Keycloak 라우트 자동 생성

**사용 시나리오**:
- Admin API가 별도 포트(9180)로 노출된 경우
- 초기 설정 스크립트

**현재 이슈**:
- ❌ Port 9180이 외부에서 접근 불가 (ClusterIP only)
- 30회 재시도 후 타임아웃

---

### 3. setup-gateway-proxy.sh

**목적**: Nginx 기반 리버스 프록시 설정 (APISIX 대안)

```bash
./setup-gateway-proxy.sh
```

**주요 기능**:
- Nginx 설정 파일 생성: `/tmp/q-gateway-nginx.conf`
- Upstream: `q-sign-keycloak` → `192.168.0.11:30181`
- CORS 헤더 설정
- Health check endpoint: `/health`

**Docker 실행**:
```bash
docker run -d \
  --name q-gateway \
  -p 8888:8888 \
  -v /tmp/q-gateway-nginx.conf:/etc/nginx/nginx.conf:ro \
  --restart unless-stopped \
  nginx:alpine
```

**현재 이슈**:
- ❌ Nginx 설정 에러: `"add_header" directive is not allowed here in /etc/nginx/nginx.conf:48`
- Docker 컨테이너가 재시작 루프에 빠짐
- ✅ 이미 중지됨 (`docker stop q-gateway`)

---

## ArgoCD에서 APISIX 설정 확인

### ArgoCD UI 접속

**URL**: `https://192.168.0.11:30443`

### q-gateway 애플리케이션 확인 단계

#### 1. Applications 리스트에서 q-gateway 선택

#### 2. APP DETAILS 탭
- **Source Repository**: GitLab Helm 저장소 확인
- **Chart Path**: APISIX Helm 차트 경로
- **Values**: Helm values.yaml 확인
  - `apisix.enabled: true`
  - `apisix.service.type: NodePort`
  - `apisix.service.nodePort: 30080`
  - `apisix.service.adminPort: 9180`

#### 3. PARAMETERS 탭
- HTTP → HTTPS 리다이렉트 관련 파라미터 확인:
  - `apisix.redirect.https: true/false`
  - `apisix.ssl.enabled`
  - `apisix.forceSSL`

#### 4. MANIFEST 탭
- **ConfigMap**: `apisix-config`
  - `config.yaml` 확인
  - Redirect 플러그인 설정 확인

- **ConfigMap**: `apisix-route-init-script`
  - `init-routes.sh` 스크립트 확인
  - Keycloak upstream 주소 확인: `keycloak-pqc:8080` vs `192.168.0.11:30181`

- **Deployment**: `apisix-route-init`
  - 라우트 자동 초기화 Job 상태 확인
  - Logs 확인: `kubectl logs -n <namespace> deployment/apisix-route-init`

#### 5. EVENTS 탭
- APISIX Pod 이벤트 확인
- 오류 메시지 확인

#### 6. LOGS 탭
- APISIX Pod 로그 확인
- HTTP/HTTPS 리다이렉트 관련 로그 찾기

### 주요 확인 사항

**HTTP → HTTPS 리다이렉트 원인 찾기:**

1. **APISIX Global 설정**:
```yaml
# ConfigMap: apisix-config
apisix:
  ssl:
    enable: false  # ← true인지 확인
    listen:
      - port: 9443
        enable_http2: true
  # redirect_on_non_idempotent: true  # ← 이런 설정 확인
```

2. **Global Rule 확인**:
```yaml
# APISIX Global Rule로 redirect가 설정되었을 수 있음
plugins:
  redirect:
    http_to_https: true  # ← 이런 설정 확인
```

3. **Ingress/LoadBalancer 확인**:
- APISIX 앞단에 Ingress나 LoadBalancer가 있는지 확인
- Ingress에서 HTTP → HTTPS 리다이렉트하는지 확인

---

## 테스트 방법

### 1. APISIX 상태 확인

```bash
# APISIX가 응답하는지 확인
curl -v http://192.168.0.11:30080/

# 예상 응답: HTTP 200 또는 404 (라우트 없음)
# 문제: HTTP 307 Temporary Redirect → HTTPS
```

### 2. APISIX Admin API 테스트

```bash
# Admin API 직접 접근 (현재 리다이렉트 발생)
curl -v http://192.168.0.11:30080/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"

# 예상: JSON 응답 (현재: 307 Redirect)
```

### 3. Q-SIGN 직접 접근 (우회)

```bash
# APISIX 우회하고 Q-SIGN 직접 접근 (현재 작동)
curl http://192.168.0.11:30181/realms/PQC-realm

# 응답:
{
  "realm": "PQC-realm",
  "public_key": "...",
  "token-service": "http://192.168.0.11:30181/realms/PQC-realm/protocol/openid-connect",
  ...
}
```

### 4. APISIX를 통한 접근 (목표)

```bash
# APISIX를 통해 PQC-realm 접근 (현재 실패)
curl http://192.168.0.11:30080/realms/PQC-realm

# 목표 응답: Q-SIGN Keycloak의 PQC-realm 정보
# 현재 응답: 307 Redirect to HTTPS
```

### 5. Q-APP에서 Gateway Flow 테스트

Q-APP `values.yaml` 수정:
```yaml
# Before (Direct Flow):
global:
  keycloakUrl: "http://192.168.0.11:30181"

# After (Gateway Flow):
global:
  keycloakUrl: "http://192.168.0.11:30080"
```

---

## 트러블슈팅

### Issue 1: HTTP → HTTPS Redirect (307)

**증상**:
```bash
$ curl -v http://192.168.0.11:30080/realms/PQC-realm
< HTTP/1.1 307 Temporary Redirect
< Location: https://192.168.0.11:30080/realms/PQC-realm
```

**원인 가능성**:
1. APISIX Global Rule에 `redirect` 플러그인 설정
2. APISIX ConfigMap에 SSL 강제 설정
3. APISIX 앞단의 Ingress/LoadBalancer가 리다이렉트
4. Kubernetes Service의 annotation에 redirect 설정

**해결 방법**:
1. **ArgoCD에서 확인**:
   - q-gateway → MANIFEST → ConfigMap: `apisix-config`
   - SSL 관련 설정 확인 및 비활성화

2. **Global Rule 확인**:
```bash
# Pod 내부에서 Admin API 접근
kubectl exec -it deployment/apisix -n <namespace> -- \
  curl http://localhost:9180/apisix/admin/global_rules \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
```

3. **ConfigMap 수정 후 재배포**:
```bash
# ArgoCD에서 Sync 또는
kubectl rollout restart deployment/apisix -n <namespace>
```

---

### Issue 2: Admin API Port 9180 접근 불가

**증상**:
```bash
$ curl http://192.168.0.11:9180/apisix/admin/routes
# 30회 재시도 후 타임아웃
```

**원인**:
- Admin API 포트(9180)가 ClusterIP로만 노출됨
- NodePort 설정이 HTTP 포트(9080)만 적용됨

**해결 방법**:

**Option 1**: Pod 내부에서 Admin API 사용
```bash
kubectl exec -it deployment/apisix -n <namespace> -- sh
# Pod 내부에서:
curl http://localhost:9180/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
```

**Option 2**: Port-forward 사용
```bash
kubectl port-forward svc/apisix 9180:9180 -n <namespace>
# 로컬에서:
curl http://localhost:9180/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
```

**Option 3**: apisix-route-init Deployment 활용
- 이미 배포된 `apisix-route-init` Deployment는 Pod 내부에서 Admin API에 접근
- ConfigMap `apisix-route-init-script`의 `init-routes.sh` 수정
- ArgoCD에서 Sync하면 자동으로 라우트 초기화

---

### Issue 3: Upstream 주소 불일치

**증상**:
- APISIX Route Init 스크립트는 `keycloak-pqc:8080`를 upstream으로 사용
- 실제 Q-SIGN은 `192.168.0.11:30181`에 있음

**원인**:
- Helm Chart가 Kubernetes 클러스터 내부 Service를 가정
- 실제 Q-SIGN은 K3s 외부 NodePort로 노출됨

**해결 방법**:

**Option 1**: Q-SIGN을 Kubernetes Service로 접근
```bash
# Q-SIGN Service 확인
kubectl get svc q-sign -n <namespace>

# Service 이름 사용 (ClusterIP)
# Upstream: q-sign:8080 또는 keycloak-pqc:8080
```

**Option 2**: ExternalName Service 생성
```yaml
apiVersion: v1
kind: Service
metadata:
  name: q-sign-external
  namespace: default
spec:
  type: ExternalName
  externalName: 192.168.0.11
  ports:
    - port: 30181
      targetPort: 30181
```

**Option 3**: ConfigMap 수정하여 NodePort 사용
```yaml
# apisix-route-init-script ConfigMap 수정
upstream:
  nodes:
    "192.168.0.11:30181": 1  # ← NodePort 직접 사용
```

---

### Issue 4: Nginx q-gateway 설정 오류

**증상**:
```
nginx: [emerg] "add_header" directive is not allowed here in /etc/nginx/nginx.conf:48
```

**원인**:
- `add_header`가 `http` 블록이 아닌 `server` 블록 외부에 있음

**해결**:
- 이미 Docker 컨테이너 중지됨: `docker stop q-gateway`
- Nginx 대신 APISIX 사용 권장

---

## 다음 단계

### 1. ArgoCD UI에서 q-gateway 설정 확인 (우선순위 1)
- `https://192.168.0.11:30443` 접속
- q-gateway 애플리케이션 선택
- HTTP → HTTPS 리다이렉트 설정 찾기
- ConfigMap `apisix-config` 확인

### 2. HTTP 리다이렉트 비활성화 (우선순위 2)
- APISIX ConfigMap 수정
- SSL 강제 설정 제거
- Global Rule 확인 및 수정

### 3. APISIX Route 확인 및 수정 (우선순위 3)
- `apisix-route-init-script` ConfigMap 확인
- Upstream 주소 수정: `keycloak-pqc:8080` → 올바른 주소
- ArgoCD Sync로 재배포

### 4. Gateway Flow 테스트 (우선순위 4)
- APISIX를 통한 `/realms/PQC-realm` 접근 테스트
- Q-APP `keycloakUrl` 변경
- 전체 SSO Flow 테스트

---

## 참고 자료

- **QSIGN 아키텍처 문서**: `../docs/QSIGN-FULL-ARCHITECTURE-FLOW.md`
- **PQC Hybrid SSO 완료 문서**: `../docs/PQC-HYBRID-SSO-COMPLETE.md`
- **APISIX 공식 문서**: https://apisix.apache.org/docs/
- **ArgoCD 문서**: https://argo-cd.readthedocs.io/

---

**작성일**: 2025-11-17
**버전**: 1.0.0
**상태**: Gateway Flow 설정 진행 중
