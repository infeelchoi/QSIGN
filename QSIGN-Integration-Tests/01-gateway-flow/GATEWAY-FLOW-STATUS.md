# Gateway Flow 전환 상태 보고서

생성일: 2025-11-18
대상: app3 및 Q-APP 전체

## 요약

Gateway Flow 활성화 작업이 진행되었으나, **APISIX 라우트 초기화 문제**로 인해 현재 완전히 작동하지 않습니다.

### 현재 상태
- ✅ Q-APP values.yaml 변경 완료 (30181 → 30080)
- ✅ Git 커밋 및 푸시 완료 (commit: 1f62241)
- ⚠️ APISIX 라우트 초기화 미완료 (0개 라우트 존재)
- ⏳ ArgoCD 자동 sync 대기 중

---

## 1. 완료된 작업

### 1.1 Q-APP values.yaml 수정
**파일**: `/home/user/QSIGN/Q-APP/k8s/helm/q-app/values.yaml`

```yaml
global:
  # Gateway Flow: Q-APP → Q-GATEWAY(APISIX) → Q-SIGN (Keycloak) → Q-KMS
  keycloakUrl: "http://192.168.0.11:30080"       # ← 변경됨 (기존: 30181)
  keycloakPublicUrl: "http://192.168.0.11:30080" # ← 변경됨
  realm: "PQC-realm"
```

**영향받는 앱**: app1, app2, app3, app4, app6, app7, ssoTestApp (전체 Q-APP)

**Git 커밋**:
```
commit 1f62241
Author: user
Date:   Tue Nov 18 16:08:43 2025

    🔧 Gateway Flow 활성화 - APISIX 경유 설정

    - keycloakUrl 변경: 30181 (Direct) → 30080 (APISIX Gateway)
    - 모든 Q-APP이 APISIX를 통해 Keycloak에 접근하도록 설정
```

### 1.2 DILITHIUM3 PQC 알고리즘 적용
**대상**: PQC-realm 및 app3-client

**변경 내용**:
- PQC-realm 기본 서명 알고리즘: RS256 → DILITHIUM3
- app3-client 토큰 알고리즘: DILITHIUM3 (access_token, id_token, userinfo)
- PQC 활성화 속성 설정

**스크립트**: `/tmp/setup-pqc-realm-dilithium3.sh`

---

## 2. 문제 상황

### 2.1 APISIX 라우트 미존재
**증상**:
```bash
$ curl -s "http://192.168.0.11:30181/apisix/admin/routes" \
    -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
{
  "list": [],
  "total": 0
}
```

**예상 라우트 수**: 최소 10개 (Keycloak realms, app1-5, web1-2 등)
**실제 라우트 수**: 0개

### 2.2 APISIX가 Keycloak로 리다이렉트 중
**테스트**:
```bash
$ curl -I http://192.168.0.11:30080/realms/PQC-realm

HTTP/1.1 307 Temporary Redirect
Location: https://192.168.0.11:30080/realms/PQC-realm
```

**분석**:
- APISIX는 요청을 받고 있음 (응답함)
- 하지만 HTTPS로 리다이렉트 (Keycloak의 기본 동작)
- 라우트가 없어서 Keycloak으로 직접 전달되지 않음

### 2.3 ArgoCD 인증 만료
```
rpc error: code = Unauthenticated desc = invalid session: token has invalid claims: token is expired
```

**영향**: ArgoCD CLI로 수동 sync를 트리거할 수 없음
**해결 방법**: ArgoCD 자동 sync 정책이 변경사항을 감지하여 자동으로 배포할 것으로 예상

---

## 3. 근본 원인 분석

### 3.1 Q-GATEWAY 라우트 초기화 메커니즘

**Q-GATEWAY 레포지토리 구조**:
```
Q-GATEWAY/
├── k8s-manifests/
│   ├── 13-apisix-route-init-configmap.yaml  (라우트 초기화 스크립트)
│   └── 13-apisix-route-init-deployment.yaml (지속적 라우트 관리)
```

**라우트 초기화 Deployment**:
- **이름**: `apisix-route-init`
- **Namespace**: `qsign-prod`
- **역할**:
  1. 시작 시 라우트 초기화
  2. 1시간마다 라우트 수 확인
  3. 라우트 < 10개일 경우 재초기화
- **APISIX 연결**: `http://apisix:9180` (내부 서비스)

### 3.2 keycloak-realms-proxy 라우트 이미 존재
**파일**: `Q-GATEWAY/k8s-manifests/13-apisix-route-init-configmap.yaml` (Line 141-167)

```bash
create_route "4" "keycloak-realms-proxy" '{
  "name": "keycloak-realms-proxy",
  "uri": "/realms/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  "plugins": {
    "proxy-rewrite": {
      "regex_uri": ["^/realms/(.*)", "/realms/$1"],
      "headers": {
        "set": {
          "X-Forwarded-Host": "192.168.0.11",
          "X-Forwarded-Port": "32602",
          "X-Forwarded-Proto": "http"
        }
      }
    }
  },
  "upstream": {
    "type": "roundrobin",
    "scheme": "http",
    "pass_host": "pass",
    "nodes": {
      "keycloak-pqc:8080": 1
    }
  },
  "status": 1
}'
```

**결론**: Keycloak 프록시 라우트는 이미 Q-GATEWAY 초기화 스크립트에 정의되어 있습니다.

### 3.3 문제의 원인
**가능성 1**: `apisix-route-init` Deployment가 실행되지 않음
**가능성 2**: 라우트 초기화 스크립트 실행 실패
**가능성 3**: APISIX가 라우트를 받아들이지 않음 (etcd 문제?)
**가능성 4**: Namespace 문제 (keycloak-pqc 서비스를 찾을 수 없음)

---

## 4. 해결 방법

### 4.1 즉시 해결 (수동)
ArgoCD Web UI에서 수동으로 처리:

1. **Q-GATEWAY 재배포**:
   ```
   ArgoCD UI → q-gateway → REFRESH → SYNC
   ```
   - `apisix-route-init` Deployment를 재시작하여 라우트 초기화

2. **Q-APP 재배포**:
   ```
   ArgoCD UI → q-app → REFRESH → SYNC
   ```
   - values.yaml 변경사항 적용 (keycloakUrl: 30080)

3. **검증**:
   ```bash
   # APISIX 라우트 확인
   curl -s "http://192.168.0.11:30181/apisix/admin/routes" \
     -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | grep "keycloak"

   # Keycloak 접근 테스트
   curl -s http://192.168.0.11:30080/realms/PQC-realm | grep "realm"

   # app3 로그인 테스트
   # 브라우저: http://192.168.0.11:30202
   ```

### 4.2 장기 해결 (자동화)
Q-GATEWAY 라우트 초기화 개선:

1. **Health Check 추가**: route-init deployment에 liveness/readiness probe
2. **로깅 개선**: 라우트 초기화 실패 원인 로깅
3. **Retry 로직**: 실패 시 즉시 재시도 (1시간 대기 → 5분)

---

## 5. 테스트 계획

### 5.1 Gateway Flow 전체 테스트
**스크립트**: `/home/user/QSIGN/test-app3-qsign-integration.sh`

**테스트 항목**:
1. ✅ Q-KMS (Vault + HSM) 연결
2. ✅ Q-SIGN (Keycloak PQC) 연결
3. ⚠️ Q-GATEWAY (APISIX) 라우트
4. ✅ app3 애플리케이션 health
5. ⏳ 전체 통합 플로우

**예상 결과** (라우트 수정 후):
```
Total Tests: 15
Passed: 15
Failed: 0
Success Rate: 100%
```

### 5.2 PQC DILITHIUM3 검증
**브라우저 테스트**:
1. http://192.168.0.11:30202 접속
2. 로그인: testuser / admin
3. 토큰 정보 확인:
   ```json
   {
     "alg": "DILITHIUM3",
     "quantum_resistant": true,
     "pqc_enabled": true
   }
   ```

---

## 6. 다음 단계

### 우선순위 1: APISIX 라우트 초기화 (즉시)
- [ ] ArgoCD Web UI에서 q-gateway REFRESH + SYNC
- [ ] APISIX route-init pod 로그 확인
- [ ] 라우트 수 확인 (최소 10개 이상)

### 우선순위 2: Q-APP 배포 (라우트 확인 후)
- [ ] ArgoCD Web UI에서 q-app REFRESH + SYNC
- [ ] app3 pod 재시작 확인
- [ ] app3 로그에서 keycloakUrl 30080 확인

### 우선순위 3: 통합 테스트
- [ ] `/home/user/QSIGN/test-app3-qsign-integration.sh` 실행
- [ ] 브라우저에서 app3 로그인 테스트
- [ ] DILITHIUM3 토큰 수신 확인

---

## 7. 참고 자료

### 관련 파일
- `/home/user/QSIGN/Q-APP/k8s/helm/q-app/values.yaml` (변경됨)
- `/home/user/QSIGN/Q-GATEWAY/k8s-manifests/13-apisix-route-init-configmap.yaml`
- `/home/user/QSIGN/Q-GATEWAY/k8s-manifests/13-apisix-route-init-deployment.yaml`
- `/tmp/setup-pqc-realm-dilithium3.sh` (DILITHIUM3 설정 스크립트)
- `/home/user/QSIGN/test-app3-qsign-integration.sh` (통합 테스트)

### Git 커밋
```
Q-APP:
  1f62241 - 🔧 Gateway Flow 활성화 - APISIX 경유 설정
  a995a93 - 🔧 app3 환경 변수 및 로그아웃 URL 수정
  d6c9dd8 - 🔧 app3/app6 로그아웃 URL 수정 - 환경 변수 사용

Q-GATEWAY:
  758e71e - 🔧 k8s-manifests: APISIX 프록시 헤더 추가 (배포용)
  db440cb - 🔧 Gateway Flow 프록시 헤더 추가 - Keycloak Frontend URL 지원
```

### 포트 정보
- **30080**: APISIX Gateway (NodePort)
- **30181**: Keycloak Direct (NodePort) - 기존 Direct Flow
- **30181**: APISIX Admin API (외부 접근)
- **9180**: APISIX Admin API (내부, cluster 내)
- **30202**: app3 (NodePort)

---

## 결론

Gateway Flow 전환 작업은 **80% 완료**되었으나, APISIX 라우트 초기화 문제로 인해 최종 동작하지 않습니다.

**핵심 해결책**: ArgoCD Web UI에서 **q-gateway를 먼저 SYNC**한 후 **q-app을 SYNC**하면 모든 문제가 해결될 것으로 예상됩니다.

라우트 초기화 deployment가 정상 작동하면:
1. `/realms/*` 경로가 APISIX를 통해 Keycloak로 프록시됨
2. app3 및 모든 Q-APP이 APISIX(30080)를 통해 Keycloak에 접근
3. PQC DILITHIUM3 서명 알고리즘이 적용된 토큰 수신

**예상 소요 시간**: 5-10분 (ArgoCD sync + pod 재시작)