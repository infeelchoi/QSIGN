# Gateway Flow HTTP → HTTPS 리다이렉트 문제 해결 가이드

**작성일**: 2025-11-17
**이슈**: APISIX가 모든 HTTP 요청을 HTTPS로 307 리다이렉트

---

## 🔍 문제 상황

### 증상
```bash
$ curl -v http://192.168.0.11:30080/realms/PQC-realm
< HTTP/1.1 307 Temporary Redirect
< Location: https://192.168.0.11:30080/realms/PQC-realm
```

### 영향
- Q-APP이 APISIX (Port 30080)를 통해 Q-SIGN에 접근 불가
- Admin API도 동일하게 리다이렉트 발생
- Gateway Flow 테스트 중단

---

## 🎯 해결 방법

### 방법 1: ArgoCD UI에서 직접 수정 (권장)

#### Step 1: ArgoCD 접속
```
URL: https://192.168.0.11:30443
```

#### Step 2: q-gateway 애플리케이션 선택

#### Step 3: MANIFEST 탭 이동

#### Step 4: ConfigMap 'apisix-config' 찾기

**찾아야 할 설정:**

```yaml
# ConfigMap: apisix-config
data:
  config.yaml: |
    apisix:
      # SSL 설정 확인
      ssl:
        enable: true    # ← false로 변경
        listen:
          - port: 9443
            enable_http2: true

      # 또는 이런 설정 찾기
      node_listen:
        - port: 9080
          enable_http2: false
        # - port: 9443   # ← HTTPS 포트 주석 처리
        #   enable_http2: true
        #   enable_http3: false

    # Plugin 설정 확인
    plugin_attr:
      redirect:
        http_to_https: true   # ← false로 변경 또는 제거
```

#### Step 5: EDIT 버튼 클릭 후 수정

**수정할 내용:**
```yaml
apisix:
  ssl:
    enable: false   # SSL 비활성화
```

또는 redirect 플러그인 제거:
```yaml
plugin_attr:
  # redirect:       # ← 주석 처리 또는 삭제
  #   http_to_https: true
```

#### Step 6: SAVE 후 ArgoCD SYNC

```bash
# ArgoCD에서 자동 Sync 또는
# CLI를 통해:
argocd app sync q-gateway
```

---

### 방법 2: Global Rules 확인 및 제거

APISIX Global Rule에 redirect 플러그인이 설정되어 있을 수 있습니다.

#### APISIX Pod 내부에서 확인 (kubectl 접근 필요)

```bash
# APISIX Pod 찾기
kubectl get pods -A | grep apisix

# Pod 내부 접속
kubectl exec -it <apisix-pod-name> -n <namespace> -- sh

# Global Rules 확인
curl -s http://localhost:9180/apisix/admin/global_rules \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | jq

# Redirect Global Rule 삭제 (ID 확인 후)
curl -X DELETE http://localhost:9180/apisix/admin/global_rules/<rule-id> \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"
```

---

### 방법 3: APISIX Upstream에서 SSL 제거

#### ConfigMap 'apisix-route-init-script' 수정

```yaml
# ConfigMap: apisix-route-init-script
data:
  init-routes.sh: |
    # ...

    # Upstream 설정에서 scheme을 http로 설정
    create_route "keycloak-realms-proxy" '{
      "name": "keycloak-realms-proxy",
      "uri": "/realms/*",
      "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
      "upstream": {
        "type": "roundrobin",
        "scheme": "http",    # ← https가 아닌 http 사용
        "pass_host": "pass",
        "nodes": {
          "keycloak-pqc:8080": 1
        }
      },
      "status": 1
    }'
```

---

### 방법 4: Kubernetes Service Annotation 확인

APISIX Service에 SSL redirect annotation이 있을 수 있습니다.

#### ArgoCD MANIFEST에서 Service 확인

```yaml
# Service: apisix
metadata:
  annotations:
    # 이런 annotation 찾아서 제거
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "false"
```

---

## 🔧 테스트 방법

### 1. HTTP 접근 테스트

```bash
# APISIX를 통한 HTTP 접근 (리다이렉트 없어야 함)
curl -v http://192.168.0.11:30080/

# 예상 응답: HTTP 200 또는 404 (라우트 없음)
# 문제 응답: HTTP 307 Redirect
```

### 2. PQC-realm 접근 테스트

```bash
# APISIX를 통한 PQC-realm 접근
curl http://192.168.0.11:30080/realms/PQC-realm

# 예상 응답:
{
  "realm": "PQC-realm",
  "public_key": "...",
  "token-service": "http://192.168.0.11:30080/realms/PQC-realm/protocol/openid-connect",
  ...
}
```

### 3. Admin API 테스트

```bash
# Admin API 접근 (리다이렉트 없어야 함)
curl http://192.168.0.11:30080/apisix/admin/routes \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1"

# 예상: JSON 응답
```

---

## 🚨 현재 권한 문제

### kubectl 접근 불가

```bash
$ export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
$ kubectl get pods -A
error: permission denied
```

**해결**:
- ArgoCD UI를 통한 수정 (권장)
- 또는 관리자 권한으로 kubectl 실행

### crictl 접근 불가

```bash
$ crictl ps
(No output - permission issue)
```

**해결**:
- `sudo crictl ps` 사용
- 또는 ArgoCD UI 사용

---

## 💡 임시 우회 방법

### Direct Flow 사용 (현재 작동 중)

APISIX를 우회하고 Q-SIGN에 직접 연결:

```yaml
# Q-APP values.yaml
global:
  keycloakUrl: "http://192.168.0.11:30181"  # APISIX 우회
```

**장점**:
- ✅ 즉시 작동
- ✅ 추가 설정 불필요
- ✅ 낮은 레이턴시

**단점**:
- ❌ Gateway 기능 없음 (Rate limiting, CORS 중앙 관리)
- ❌ SkyWalking 모니터링 없음

---

## 📋 체크리스트

APISIX HTTP 리다이렉트 문제 해결:

- [ ] ArgoCD UI 접속 (`https://192.168.0.11:30443`)
- [ ] q-gateway 애플리케이션 선택
- [ ] MANIFEST 탭에서 ConfigMap `apisix-config` 확인
- [ ] `apisix.ssl.enable: false` 설정
- [ ] Redirect plugin 제거 또는 `http_to_https: false`
- [ ] ArgoCD SYNC
- [ ] HTTP 접근 테스트: `curl http://192.168.0.11:30080/`
- [ ] PQC-realm 테스트: `curl http://192.168.0.11:30080/realms/PQC-realm`
- [ ] Q-APP에서 Gateway Flow 테스트

---

## 📚 참고 자료

- **APISIX SSL 문서**: https://apisix.apache.org/docs/apisix/tutorials/how-to-secure-apis/
- **APISIX Redirect Plugin**: https://apisix.apache.org/docs/apisix/plugins/redirect/
- **Gateway Flow README**: [README.md](./README.md)

---

## 🎯 예상 결과

### 수정 전 (현재)
```bash
$ curl -I http://192.168.0.11:30080/realms/PQC-realm
HTTP/1.1 307 Temporary Redirect
Location: https://192.168.0.11:30080/realms/PQC-realm
```

### 수정 후 (목표)
```bash
$ curl -I http://192.168.0.11:30080/realms/PQC-realm
HTTP/1.1 200 OK
Content-Type: application/json
```

---

**작성자**: QSIGN Team
**버전**: 1.0.0
**상태**: 해결 진행 중 - ArgoCD UI 접근 필요
