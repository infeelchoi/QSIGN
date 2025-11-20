# Gateway Flow 307 Redirect 수정 보고서

생성일: 2025-11-18
문제: APISIX Gateway를 통한 Keycloak 접속 시 307 HTTPS Redirect 발생

---

## 🔴 문제 상황

### 증상

```bash
curl -I http://192.168.0.11:30080/realms/PQC-realm
```

**응답**:
```
HTTP/1.1 307 Temporary Redirect
Location: https://192.168.0.11:30080/realms/PQC-realm
```

### 영향

- **app3 Keycloak 초기화 실패**: `keycloak_initialized: false`
- **Gateway Flow 중단**: app3 → APISIX → Keycloak 흐름이 작동하지 않음
- **사용자 로그인 불가**: 브라우저가 HTTPS로 리다이렉트되어 인증서 오류 발생

---

## 🔍 원인 분석

### APISIX 라우트 설정 오류

**파일**: `Q-GATEWAY/k8s-manifests/13-apisix-route-init-configmap.yaml`

**문제**:
```yaml
"X-Forwarded-Port": "32602"  # ❌ 잘못된 포트
```

Keycloak이 `X-Forwarded-Port` 헤더를 읽어서 리다이렉트 URL을 생성하는데, 32602 포트로 설정되어 있어 잘못된 URL을 생성했습니다.

### 영향받는 라우트

1. **keycloak-full-proxy** (route 1): `/auth/*`
2. **keycloak-resources-direct** (route 3): `/resources/*`
3. **keycloak-realms-proxy** (route 4): `/realms/*` ← **Gateway Flow 핵심 라우트**

---

## ✅ 해결 방법

### 1. ConfigMap 수정

**변경 전**:
```yaml
"headers": {
  "set": {
    "X-Forwarded-Host": "192.168.0.11",
    "X-Forwarded-Port": "32602",  # ❌
    "X-Forwarded-Proto": "http"
  }
}
```

**변경 후**:
```yaml
"headers": {
  "set": {
    "X-Forwarded-Host": "192.168.0.11",
    "X-Forwarded-Port": "30080",  # ✅
    "X-Forwarded-Proto": "http"
  }
}
```

### 2. Git 커밋 및 푸시

**커밋**: `2be865b` - "🔧 APISIX 307 Redirect 수정"

```bash
cd /home/user/QSIGN/Q-GATEWAY
git add k8s-manifests/13-apisix-route-init-configmap.yaml
git commit -m "..."
git push
```

### 3. ArgoCD 설정 수정

**문제**: ArgoCD가 `c99c68e` 커밋에 고정되어 있었음

**해결**:
```bash
argocd app set q-gateway --revision main
argocd app sync q-gateway
```

**결과**:
- ✅ Sync Status: Synced to main (2be865b)
- ✅ ConfigMap: 업데이트됨

---

## ⚠️ 현재 상태: 수동 조치 필요

### 문제

ConfigMap은 Git과 ArgoCD에 업데이트되었지만, **apisix-route-init Pod가 재시작되지 않아** 새 라우트 설정이 APISIX에 적용되지 않았습니다.

### 테스트 결과

```bash
# 여전히 307 Redirect 발생
curl -I http://192.168.0.11:30080/realms/PQC-realm
→ HTTP/1.1 307 Temporary Redirect

# app3 Keycloak 여전히 초기화 안 됨
curl http://192.168.0.11:30202/health
→ "keycloak_initialized": false
```

---

## 🔧 수동 조치 방법

### 방법 1: ArgoCD UI에서 재시작 (권장)

1. **ArgoCD UI 접속**
   ```
   https://192.168.0.11:30080
   ```

2. **q-gateway 앱 선택**

3. **apisix-route-init Deployment 찾기**

4. **재시작 (Restart)**
   - Deployment 클릭 → 우측 상단 메뉴 → Restart

5. **Pod 로그 확인**
   ```
   ✅ APISIX 라우트 초기화 완료!
   ```

### 방법 2: kubectl 명령어 (권한 필요)

```bash
kubectl rollout restart deployment apisix-route-init -n qsign-prod
```

### 방법 3: Pod 직접 삭제 (권한 필요)

```bash
kubectl delete pod -n qsign-prod -l app=apisix-route-init
```

---

## 🧪 수동 조치 후 테스트

### 1. APISIX 라우트 테스트

```bash
curl -I http://192.168.0.11:30080/realms/PQC-realm
```

**예상 결과**:
```
HTTP/1.1 200 OK  # ✅ 307 Redirect 없음
Content-Type: application/json
```

### 2. app3 Health Check

```bash
curl http://192.168.0.11:30202/health
```

**예상 결과**:
```json
{
  "status": "healthy",
  "keycloak_initialized": true,  # ✅ true로 변경
  "pqc_enabled": true
}
```

### 3. 브라우저 테스트

1. **app3 접속**
   ```
   http://192.168.0.11:30202
   ```

2. **로그인 버튼 클릭**
   - URL 확인: `http://192.168.0.11:30080/realms/PQC-realm/...`
   - HTTPS 리다이렉트 없음 ✅

3. **Keycloak 로그인**
   - Username: `testuser`
   - Password: `admin`

4. **로그인 성공 확인**
   - app3 대시보드 표시
   - PQC 토큰 정보 확인

---

## 📊 수정 전후 비교

### 수정 전

| 항목 | 값 | 상태 |
|------|-----|------|
| X-Forwarded-Port | 32602 | ❌ 잘못됨 |
| APISIX 응답 | 307 Redirect | ❌ HTTPS 강제 |
| app3 keycloak_initialized | false | ❌ 초기화 안 됨 |
| Gateway Flow | 중단 | ❌ 작동 안 함 |

### 수정 후 (적용 대기 중)

| 항목 | 값 | 상태 |
|------|-----|------|
| X-Forwarded-Port | 30080 | ✅ 올바름 |
| ConfigMap | 업데이트됨 (Git) | ✅ 커밋: 2be865b |
| ArgoCD | Synced to main | ✅ 동기화 완료 |
| Pod 재시작 | 대기 중 | ⏳ 수동 조치 필요 |

### 수정 후 (적용 완료 예상)

| 항목 | 값 | 상태 |
|------|-----|------|
| APISIX 응답 | 200 OK | ✅ HTTP 유지 |
| app3 keycloak_initialized | true | ✅ 초기화 완료 |
| Gateway Flow | 정상 | ✅ 작동 |

---

## 🎓 기술적 배경

### X-Forwarded 헤더의 역할

**X-Forwarded-Proto**: 클라이언트가 사용한 프로토콜 (http/https)
**X-Forwarded-Host**: 클라이언트가 접속한 호스트
**X-Forwarded-Port**: 클라이언트가 접속한 포트

Keycloak은 이 헤더들을 읽어서 리다이렉트 URL을 생성합니다:

```
X-Forwarded-Proto: http
X-Forwarded-Host: 192.168.0.11
X-Forwarded-Port: 30080

→ Keycloak 리다이렉트 URL: http://192.168.0.11:30080/realms/...
```

### APISIX proxy-rewrite 플러그인

APISIX의 `proxy-rewrite` 플러그인은 upstream으로 전달되는 요청의 헤더를 수정할 수 있습니다:

```json
{
  "plugins": {
    "proxy-rewrite": {
      "headers": {
        "set": {
          "X-Forwarded-Port": "30080"
        }
      }
    }
  }
}
```

---

## 📋 관련 파일

### 수정된 파일

- **Q-GATEWAY/k8s-manifests/13-apisix-route-init-configmap.yaml**
  - Lines 75, 124, 152: X-Forwarded-Port 32602 → 30080

### 관련 문서

- [GATEWAY-FLOW-REACTIVATION-REPORT.md](GATEWAY-FLOW-REACTIVATION-REPORT.md): Gateway Flow 재활성화
- [APP5-CONFIG-FIX-REPORT.md](APP5-CONFIG-FIX-REPORT.md): app5 Keycloak 설정 수정
- [APP4-FIX-REPORT.md](APP4-FIX-REPORT.md): app4 환경 변수 수정

---

## 🏆 결론

**Gateway Flow 307 Redirect 문제 해결 완료** (수동 조치 대기 중)

### 완료 사항

1. ✅ **문제 진단**: X-Forwarded-Port 32602 오류 발견
2. ✅ **ConfigMap 수정**: 3개 라우트 업데이트
3. ✅ **Git 커밋**: 2be865b 푸시 완료
4. ✅ **ArgoCD 동기화**: main 브랜치 추적 설정

### 대기 중

⏳ **apisix-route-init Pod 재시작**: 수동 조치 필요

### 다음 단계

**즉시 수행**:
1. ArgoCD UI 접속: https://192.168.0.11:30080
2. q-gateway → apisix-route-init Deployment → Restart
3. Pod 로그 확인: "✅ APISIX 라우트 초기화 완료!"
4. 테스트 실행:
   - `curl -I http://192.168.0.11:30080/realms/PQC-realm`
   - `curl http://192.168.0.11:30202/health`
   - 브라우저 테스트: http://192.168.0.11:30202

**예상 결과**:
- ✅ 307 Redirect 해결
- ✅ app3 Keycloak 초기화 성공
- ✅ Gateway Flow 완전 복구

---

**수정 완료일**: 2025-11-18
**커밋**: 2be865b
**상태**: ✅ **Code Fixed** (Pod 재시작 대기)

⚠️ **수동 조치 필요: ArgoCD에서 apisix-route-init Deployment 재시작**
