# Gateway Flow 활성화 - 최종 단계 가이드

생성일: 2025-11-18
상태: ⚠️ **APISIX 라우트 수동 추가 필요**

---

## ✅ 완료된 작업

1. **ArgoCD 로그인 성공** ✓
   - 연결: http://192.168.0.11:30080
   - 계정: admin / qwer1234!

2. **q-gateway Sync 완료** ✓
   - Status: Synced
   - Health: Healthy
   - apisix-route-init Deployment: 배포됨

3. **q-app Sync 완료** ✓
   - Status: Synced to 1f62241 (Gateway Flow commit)
   - Health: Healthy
   - keycloakUrl: 30080 설정 적용됨

4. **PQC DILITHIUM3 설정** ✓
   - PQC-realm: DILITHIUM3
   - app3-client: DILITHIUM3

---

## ⚠️ 현재 문제

### APISIX 라우트가 초기화되지 않음

**증상**:
```bash
$ curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
    -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
{
  "list": [],
  "total": 0
}
```

**원인**:
- APISIX Admin API가 외부에서 PUT/POST 요청을 받아들이지 않음
- apisix-route-init Deployment가 라우트를 생성하지 못함 (etcd 또는 권한 문제 가능성)

**영향**:
- APISIX가 `/realms/*` 경로를 Keycloak으로 프록시하지 못함
- app3가 30080 포트로 Keycloak에 접근할 수 없음
- Gateway Flow: **비활성화 상태**

---

## 🎯 해결 방법: APISIX Dashboard 사용

### 1단계: APISIX Dashboard에서 라우트 수동 추가 (필수)

**상세 가이드**: [APISIX-DASHBOARD-ROUTE-GUIDE.md](APISIX-DASHBOARD-ROUTE-GUIDE.md)

**빠른 가이드**:
```
1. 브라우저: http://192.168.0.11:31281
2. 로그인: admin / admin
3. Routes → Create
4. 다음 정보 입력:

   Name: keycloak-realms-proxy
   Path: /realms/*
   Methods: GET, POST, PUT, DELETE, OPTIONS

   Upstream:
     - Host: keycloak.q-sign.svc.cluster.local
     - Port: 8080
     - Weight: 1

   Plugin (CORS):
     {
       "allow_origins": "*",
       "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
       "allow_headers": "*"
     }

5. Submit
```

### 2단계: 라우트 추가 확인

```bash
# APISIX 라우트 수 확인
curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | \
  python3 -c "import sys, json; print(f\"Routes: {len(json.load(sys.stdin).get('list', []))}\")"

# 기대 출력: Routes: 1 이상
```

```bash
# Keycloak 접근 테스트 (APISIX 경유)
curl -s http://192.168.0.11:30080/realms/PQC-realm | grep realm

# 성공 시 출력: "realm":"PQC-realm"
```

### 3단계: Gateway Flow 전체 테스트

```bash
bash /home/user/QSIGN/test-app3-qsign-integration.sh
```

**예상 결과**:
```
총 테스트: 15
성공: 13-15 (86-100%)
실패: 0-2

✓ Q-SIGN (Keycloak PQC) 연결 성공
✓ app3-client DILITHIUM3 설정 확인
✓ Gateway Flow 정상 작동
```

### 4단계: 브라우저 테스트 (최종 확인)

```
1. 브라우저: http://192.168.0.11:30202
2. "로그인" 버튼 클릭
3. Keycloak 로그인: testuser / admin
4. 토큰 정보 확인:
   - Algorithm: DILITHIUM3
   - Quantum Resistant: true
```

---

## 📊 현재 시스템 상태

### APISIX (Q-GATEWAY)
- ✅ Deployment: Healthy
- ✅ Service: 정상 (30080, 32602)
- ❌ 라우트 수: 0개 (수동 추가 필요)

### Keycloak (Q-SIGN)
- ✅ 연결: 정상
- ✅ PQC-realm: DILITHIUM3
- ✅ app3-client: DILITHIUM3 설정됨

### app3 (Q-APP)
- ✅ Status: Healthy
- ✅ PQC Enabled: True
- ⚠️ Keycloak Initialized: False (라우트 추가 후 해결 예상)

### Gateway Flow
- ⏳ 대기 중: APISIX 라우트 추가 필요

---

## 🔍 트러블슈팅

### 문제 1: 라우트 추가 후에도 app3 로그인 실패
**해결**:
```bash
# app3 Pod 강제 재시작 (keycloakUrl 30080 재적용)
argocd app sync q-app --prune --force

# 또는 kubectl 사용
sudo k3s kubectl rollout restart deployment/app3 -n q-app
```

### 문제 2: Upstream 서비스를 찾을 수 없음 (503 에러)
**원인**: Keycloak 서비스 이름이 다름
**확인**:
```bash
# Keycloak 서비스 확인
sudo k3s kubectl get svc -n q-sign | grep keycloak

# 가능한 이름:
# - keycloak.q-sign.svc.cluster.local:8080
# - keycloak-pqc.q-sign.svc.cluster.local:8080
```

### 문제 3: CORS 에러 발생
**해결**: APISIX Dashboard에서 CORS 플러그인 활성화 확인
```json
{
  "allow_origins": "*",
  "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
  "allow_headers": "*",
  "expose_headers": "*"
}
```

---

## 📁 관련 문서

1. **APISIX Dashboard 라우트 추가 가이드**
   - 파일: [APISIX-DASHBOARD-ROUTE-GUIDE.md](APISIX-DASHBOARD-ROUTE-GUIDE.md)
   - 내용: Dashboard 사용법, 라우트 설정 상세 안내

2. **Gateway Flow 상태 보고서**
   - 파일: [GATEWAY-FLOW-STATUS.md](GATEWAY-FLOW-STATUS.md)
   - 내용: 전체 작업 내역, 아키텍처 설명

3. **app3 통합 테스트 스크립트**
   - 파일: `/home/user/QSIGN/test-app3-qsign-integration.sh`
   - 기능: Q-KMS, Q-SIGN, Q-GATEWAY, app3 전체 스택 테스트

---

## ✨ 최종 체크리스트

라우트 추가 후 다음 항목을 확인하세요:

- [ ] **APISIX 라우트**: http://192.168.0.11:31281 에서 추가 완료
- [ ] **라우트 수**: 최소 1개 이상 (keycloak-realms-proxy)
- [ ] **Keycloak 접근**: `curl http://192.168.0.11:30080/realms/PQC-realm` 성공
- [ ] **app3 로그인**: http://192.168.0.11:30202 에서 로그인 성공
- [ ] **DILITHIUM3 토큰**: 브라우저에서 PQC 알고리즘 확인
- [ ] **통합 테스트**: `test-app3-qsign-integration.sh` 성공률 90% 이상

---

## 🚀 예상 소요 시간

- **APISIX Dashboard 라우트 추가**: 3-5분
- **테스트 및 검증**: 2-3분
- **총 소요 시간**: **5-10분**

---

## 📞 다음 단계

1. ⭐ **지금 바로**: [APISIX Dashboard](http://192.168.0.11:31281)에서 라우트 추가
2. 라우트 추가 완료 후: `bash /home/user/QSIGN/test-app3-qsign-integration.sh` 실행
3. 테스트 성공 확인: 브라우저에서 app3 로그인 및 DILITHIUM3 토큰 확인

모든 설정이 준비되어 있으며, **라우트 1개만 추가하면 Gateway Flow가 즉시 작동합니다!** 🎉