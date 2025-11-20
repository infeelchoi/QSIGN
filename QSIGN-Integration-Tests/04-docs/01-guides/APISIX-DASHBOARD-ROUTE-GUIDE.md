# APISIX Dashboard - 라우트 수동 추가 가이드

생성일: 2025-11-18
목적: Gateway Flow 활성화를 위한 Keycloak 라우트 추가

## 📋 개요

APISIX Admin API가 외부에서 접근이 제한되어 있어, **APISIX Dashboard Web UI**를 통해 라우트를 수동으로 추가해야 합니다.

**Dashboard 정보**:
- URL: http://192.168.0.11:31281
- ID: `admin`
- PW: `admin`

---

## 🎯 추가할 라우트 (Gateway Flow 필수)

Gateway Flow를 활성화하려면 **1개의 핵심 라우트**만 추가하면 됩니다:

### 1. Keycloak Realms Proxy ⭐ (필수)
**목적**: app3 등 모든 앱이 APISIX(30080)를 통해 Keycloak에 접근

---

## 📝 APISIX Dashboard 라우트 추가 방법

### 1단계: Dashboard 로그인
```
1. 브라우저에서 http://192.168.0.11:31281 접속
2. Username: admin
3. Password: admin
4. "Login" 버튼 클릭
```

### 2단계: Routes 메뉴 이동
```
1. 왼쪽 메뉴에서 "Routes" 클릭
2. 우측 상단 "Create" 버튼 클릭
```

### 3단계: Keycloak Realms Proxy 라우트 생성

#### Step 1: Basic Information
```
Name: keycloak-realms-proxy
Description: Gateway Flow - Keycloak Realms Proxy for Q-APP
```

#### Step 2: Request Basic Define
```
Path: /realms/*
HTTP Methods: [체크박스 모두 선택]
  ☑ GET
  ☑ POST
  ☑ PUT
  ☑ DELETE
  ☑ OPTIONS
```

#### Step 3: Upstream (Scheme)
```
Algorithm: roundrobin
Nodes 추가:
  - Host: keycloak.q-sign.svc.cluster.local
  - Port: 8080
  - Weight: 1
```

**중요**: Service Discovery는 사용하지 않음 (Manual로 nodes 입력)

#### Step 4: Plugin Config (CORS 추가)
```
1. "Plugins" 탭 클릭
2. "cors" 플러그인 검색
3. "Enable" 버튼 클릭
4. 다음 JSON 입력:

{
  "allow_origins": "*",
  "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
  "allow_headers": "*",
  "expose_headers": "*",
  "max_age": 3600
}

5. "Submit" 클릭
```

#### Step 5: 라우트 생성 완료
```
1. "Next" 클릭하여 모든 단계 진행
2. 마지막 단계에서 "Submit" 클릭
```

---

## ✅ 라우트 생성 확인

### Dashboard에서 확인
```
1. Routes 리스트에서 "keycloak-realms-proxy" 확인
2. Status: "Online" (녹색)
3. Path: "/realms/*" 확인
```

### 명령줄에서 확인
```bash
# APISIX 라우트 수 확인
curl -s "http://192.168.0.11:32602/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | \
  python3 -c "import sys, json; data = json.load(sys.stdin); print(f'Total routes: {len(data.get(\"list\", []))}')"

# 기대 출력: Total routes: 1 이상
```

### Keycloak 접근 테스트
```bash
# APISIX(30080)를 통한 Keycloak 접근
curl -s http://192.168.0.11:30080/realms/PQC-realm | grep -i realm

# 성공 시 출력 예시:
# "realm":"PQC-realm"
```

---

## 🧪 Gateway Flow 전체 테스트

라우트 추가 후 다음 명령으로 통합 테스트를 실행하세요:

```bash
bash /home/user/QSIGN/test-app3-qsign-integration.sh
```

**예상 결과**:
```
총 테스트: 15
성공: 13-15
실패: 0-2
성공률: 86-100%
```

---

## 🔧 선택적 라우트 (추가 기능용)

Gateway Flow의 핵심 기능만 사용한다면 위의 **keycloak-realms-proxy** 하나면 충분합니다.

추가 기능이 필요한 경우 아래 라우트도 추가할 수 있습니다:

### 2. Keycloak Resources (CSS/JS 파일)
```json
{
  "name": "keycloak-resources-direct",
  "uri": "/resources/*",
  "methods": ["GET"],
  "upstream": {
    "type": "roundrobin",
    "nodes": {
      "keycloak.q-sign.svc.cluster.local:8080": 1
    }
  }
}
```

### 3. Vault KMS Route
```json
{
  "name": "vault-kms-route",
  "uri": "/vault/*",
  "upstream": {
    "type": "roundrobin",
    "nodes": {
      "vault.q-sign.svc.cluster.local:8200": 1
    }
  }
}
```

---

## 🐛 트러블슈팅

### 문제 1: Dashboard 로그인 실패
**원인**: 잘못된 계정 정보
**해결**:
- ID: `admin` (소문자)
- PW: `admin`

### 문제 2: Upstream 서비스를 찾을 수 없음
**증상**: 라우트 생성 후 503 Service Unavailable
**원인**: Keycloak 서비스 이름이 다름
**해결**:
```bash
# 정확한 서비스 이름 확인 필요
# 가능한 이름들:
- keycloak.q-sign.svc.cluster.local:8080
- keycloak-pqc.q-sign.svc.cluster.local:8080
- keycloak.qsign-prod.svc.cluster.local:8080
```

### 문제 3: app3 로그인 시 여전히 30181 포트 사용
**원인**: app3 Pod가 구버전 설정 사용 중
**해결**:
```bash
# app3 Pod 재시작 (ArgoCD 통해)
argocd app sync q-app --prune
```

---

## 📊 최종 확인 체크리스트

- [ ] APISIX Dashboard 로그인 성공
- [ ] keycloak-realms-proxy 라우트 추가 완료
- [ ] CORS 플러그인 활성화
- [ ] 라우트 Status: Online (녹색)
- [ ] `curl http://192.168.0.11:30080/realms/PQC-realm` 성공 (200 OK)
- [ ] app3 로그인 테스트 성공 (http://192.168.0.11:30202)
- [ ] DILITHIUM3 토큰 수신 확인

---

## 📚 참고 자료

- APISIX Dashboard 공식 문서: https://apisix.apache.org/docs/dashboard/USER_GUIDE/
- Gateway Flow 상태 보고서: [GATEWAY-FLOW-STATUS.md](GATEWAY-FLOW-STATUS.md)
- app3 통합 테스트 스크립트: `/home/user/QSIGN/test-app3-qsign-integration.sh`

---

**예상 소요 시간**: 5-10분

라우트 추가 완료 후 즉시 Gateway Flow가 작동하며, app3에서 DILITHIUM3 PQC 토큰을 받을 수 있습니다! 🚀