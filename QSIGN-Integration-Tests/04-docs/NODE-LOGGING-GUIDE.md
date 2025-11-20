# QSIGN 노드별 로그 확인 가이드

## 📋 개요

QSIGN 인증 흐름의 각 노드별 로그 확인 방법을 설명합니다.

### 인증 흐름
```
App5 (30204)
  ↓ OAuth2 PKCE
Q-Gateway APISIX (32602)
  ↓ /realms/* 라우팅
Keycloak (30181)
  ↓ DILITHIUM3 JWT 서명
Q-KMS Vault (30820)
  ↓ Transit Engine
Luna HSM
```

---

## 1️⃣ App5 (Angular Frontend) - 포트 30204

### 기본 로그 확인
```bash
# Pod 상태 확인
kubectl get pods -n q-app -l app=app5

# 실시간 로그 확인
kubectl logs -f -n q-app deployment/app5

# 최근 100줄 로그
kubectl logs -n q-app deployment/app5 --tail=100

# 특정 Pod 로그
kubectl logs -n q-app <app5-pod-name>
```

### 주요 로그 필터링

#### OAuth2 인증 흐름
```bash
# 로그인 시도 확인
kubectl logs -n q-app deployment/app5 --tail=200 | grep -i "login\|auth\|token"

# PKCE 흐름 확인
kubectl logs -n q-app deployment/app5 --tail=200 | grep -i "pkce\|code_challenge"

# Keycloak 리다이렉트 확인
kubectl logs -n q-app deployment/app5 --tail=200 | grep -i "redirect\|callback"
```

#### Angular 빌드 및 에러
```bash
# Angular 빌드 로그
kubectl logs -n q-app deployment/app5 --tail=500 | grep -i "compiled\|error\|warning"

# HTTP 에러 확인
kubectl logs -n q-app deployment/app5 | grep -E "HTTP [4-5][0-9][0-9]"
```

### Pod 이벤트 확인
```bash
# Pod 상태 및 이벤트
kubectl describe pod -n q-app <app5-pod-name>

# 최근 이벤트만
kubectl get events -n q-app --field-selector involvedObject.name=<app5-pod-name> --sort-by='.lastTimestamp'
```

### 브라우저 DevTools 로그
```bash
# App5 접속
http://192.168.0.11:30204

# 브라우저 Console에서 확인:
# - Network 탭: XHR 요청 (토큰 발급, API 호출)
# - Console 탭: JavaScript 에러
# - Application 탭: LocalStorage (토큰 저장)
```

---

## 2️⃣ Q-Gateway APISIX - 포트 32602

### 기본 로그 확인
```bash
# APISIX Pod 확인
kubectl get pods -n qsign-prod -l app.kubernetes.io/name=apisix

# APISIX 로그 (실시간)
kubectl logs -f -n qsign-prod deployment/apisix

# 에러 로그만 확인
kubectl logs -n qsign-prod deployment/apisix --tail=200 | grep -i "error\|fail\|warn"
```

### 라우팅 로그

#### 특정 경로 라우팅 확인
```bash
# /realms/* 라우팅 로그
kubectl logs -n qsign-prod deployment/apisix --tail=500 | grep "/realms/"

# /vault/* 라우팅 로그
kubectl logs -n qsign-prod deployment/apisix --tail=500 | grep "/vault/"

# HTTP 상태 코드별 필터링
kubectl logs -n qsign-prod deployment/apisix | grep -E "HTTP/[0-9.]+ (200|404|500)"
```

#### 업스트림 상태 확인
```bash
# Keycloak 업스트림 상태
kubectl logs -n qsign-prod deployment/apisix | grep -i "upstream\|backend\|192.168.0.11:30181"
```

### APISIX Admin API로 라우트 확인
```bash
# 모든 라우트 목록
curl -s "http://192.168.0.11:30282/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | python3 -m json.tool

# Keycloak Realms 라우트 (ID: 4)
curl -s "http://192.168.0.11:30282/apisix/admin/routes/4" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | python3 -m json.tool

# 라우트 히트 카운트 확인
curl -s "http://192.168.0.11:30282/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | \
  python3 -c "import sys, json; routes = json.load(sys.stdin)['list']['list']; \
  [print(f\"{r['value']['name']}: {r['value']['uri']}\") for r in routes]"
```

### APISIX Dashboard 로그
```bash
# Dashboard Pod 로그
kubectl logs -n qsign-prod deployment/apisix-dashboard --tail=100
```

### APISIX Route Init 로그
```bash
# 라우트 초기화 로그
kubectl logs -n qsign-prod deployment/apisix-route-init --tail=100

# 라우트 생성 성공 여부
kubectl logs -n qsign-prod deployment/apisix-route-init | grep "✅\|❌"
```

### Access Log 활성화 (선택)
```yaml
# APISIX ConfigMap 수정
kubectl edit configmap apisix -n qsign-prod

# access_log 섹션 추가:
nginx_config:
  http:
    access_log: /dev/stdout
    access_log_format: '$remote_addr - [$time_local] "$request" $status'
```

---

## 3️⃣ Keycloak PQC - 포트 30181

### 기본 로그 확인
```bash
# Keycloak Pod 확인
kubectl get pods -n q-sign -l app=keycloak-pqc

# 실시간 로그
kubectl logs -f -n q-sign deployment/keycloak-pqc

# 최근 100줄
kubectl logs -n q-sign deployment/keycloak-pqc --tail=100
```

### PQC 관련 로그

#### DILITHIUM3 서명
```bash
# Dilithium3 서명 프로바이더 로그
kubectl logs -n q-sign deployment/keycloak-pqc --tail=500 | grep -i "dilithium"

# 서명 생성 로그
kubectl logs -n q-sign deployment/keycloak-pqc --tail=500 | grep "서명"

# JWT 토큰 발급
kubectl logs -n q-sign deployment/keycloak-pqc --tail=500 | grep -i "jwt\|token"
```

#### Vault 연동 로그
```bash
# Vault 연동 상태
kubectl logs -n q-sign deployment/keycloak-pqc --tail=200 | grep -i "vault"

# Vault 인증 성공/실패
kubectl logs -n q-sign deployment/keycloak-pqc | grep -E "Vault authentication|HTTP 403|HTTP 200"

# Vault Transit Engine 서명
kubectl logs -n q-sign deployment/keycloak-pqc | grep -i "transit\|sign"
```

#### Luna HSM 연동 로그
```bash
# HSM 연동 상태
kubectl logs -n q-sign deployment/keycloak-pqc --tail=200 | grep -i "hsm\|luna"

# HSM 서명 로그
kubectl logs -n q-sign deployment/keycloak-pqc | grep "Luna HSM"
```

### Realm 및 클라이언트 로그

#### PQC-realm 로그
```bash
# PQC-realm 관련
kubectl logs -n q-sign deployment/keycloak-pqc | grep "PQC-realm"

# 클라이언트 인증
kubectl logs -n q-sign deployment/keycloak-pqc | grep "app5-client\|app3-client"
```

#### 사용자 인증 로그
```bash
# 로그인 시도
kubectl logs -n q-sign deployment/keycloak-pqc --tail=500 | grep -i "login\|authentication"

# 토큰 발급
kubectl logs -n q-sign deployment/keycloak-pqc --tail=500 | grep -i "token endpoint\|grant_type"
```

### 에러 로그
```bash
# ERROR 레벨만
kubectl logs -n q-sign deployment/keycloak-pqc | grep "ERROR"

# WARN 레벨 포함
kubectl logs -n q-sign deployment/keycloak-pqc | grep -E "ERROR|WARN"

# 예외 스택 트레이스
kubectl logs -n q-sign deployment/keycloak-pqc --tail=1000 | grep -A 10 "Exception"
```

### Keycloak Admin Console 로그
```bash
# Admin 콘솔 접속
http://192.168.0.11:30181

# Server Info → Providers에서 확인:
# - signature: dilithium3 활성화 여부
# - keys: dilithium3 키 프로바이더 확인

# Events → Login Events:
# - 사용자 로그인 이벤트
# - 토큰 발급 이벤트
```

---

## 4️⃣ Q-KMS Vault - 포트 30820

### 기본 로그 확인
```bash
# Vault Pod 확인
kubectl get pods -n q-kms -l app.kubernetes.io/name=q-kms

# 실시간 로그
kubectl logs -f -n q-kms deployment/q-kms

# 최근 200줄
kubectl logs -n q-kms deployment/q-kms --tail=200
```

### Vault 상태 확인
```bash
# Vault 상태 API
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/sys/health" | python3 -m json.tool

# Seal 상태 확인
kubectl logs -n q-kms deployment/q-kms | grep -i "seal\|unseal"

# 초기화 상태
kubectl logs -n q-kms deployment/q-kms | grep -i "initialized"
```

### Transit Engine 로그

#### 마운트 확인
```bash
# Transit Engine 마운트 로그
kubectl logs -n q-kms deployment/q-kms | grep -i "transit"

# 마운트 API 확인
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/sys/mounts" | python3 -m json.tool | grep -A 5 "transit"
```

#### 키 관리
```bash
# dilithium-key 정보
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/transit/keys/dilithium-key" | python3 -m json.tool

# 모든 Transit 키 목록
curl -s -X LIST -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/transit/keys" | python3 -m json.tool
```

#### 서명 작업 로그
```bash
# 서명 요청 로그
kubectl logs -n q-kms deployment/q-kms --tail=500 | grep -i "sign\|signature"

# 인증 로그
kubectl logs -n q-kms deployment/q-kms | grep -i "authentication\|token"
```

### Audit 로그 (활성화된 경우)
```bash
# Audit 로그 확인
kubectl logs -n q-kms deployment/q-kms | grep -i "audit"

# Audit Backend 상태
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/sys/audit" | python3 -m json.tool
```

### Vault Metrics
```bash
# Prometheus 메트릭
curl -s "http://192.168.0.11:30820/v1/sys/metrics?format=prometheus"

# 특정 메트릭 필터링
kubectl logs -n q-kms deployment/q-kms | grep -i "metric\|performance"
```

---

## 5️⃣ Luna HSM (Optional)

### HSM 연동 확인
```bash
# Keycloak에서 HSM 연동 로그
kubectl logs -n q-sign deployment/keycloak-pqc | grep -i "luna\|hsm"

# HSM 연결 테스트
kubectl logs -n q-sign deployment/keycloak-pqc | grep "HSM 연결 테스트"

# HSM 서명 작업
kubectl logs -n q-sign deployment/keycloak-pqc | grep "HSM 서명"
```

### HSM 에러 확인
```bash
# DNS 조회 실패
kubectl logs -n q-sign deployment/keycloak-pqc | grep "UnknownHostException: luna-hsm"

# 연결 실패
kubectl logs -n q-sign deployment/keycloak-pqc | grep "HSM 연결 실패\|HSM.*실패"
```

---

## 📊 전체 흐름 통합 로그 확인

### 실시간 통합 모니터링 (tmux/screen 사용)
```bash
# Tmux 세션 시작
tmux new-session -s qsign-logs

# 화면 분할 (Ctrl+B, %)
# 각 패널에서:

# 패널 1: App5
kubectl logs -f -n q-app deployment/app5 --tail=50

# 패널 2: APISIX
kubectl logs -f -n qsign-prod deployment/apisix --tail=50

# 패널 3: Keycloak
kubectl logs -f -n q-sign deployment/keycloak-pqc --tail=50

# 패널 4: Vault
kubectl logs -f -n q-kms deployment/q-kms --tail=50
```

### 통합 로그 수집 스크립트

#### 스크립트 작성
```bash
cat > /tmp/collect-qsign-logs.sh << 'EOF'
#!/bin/bash

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_DIR="/tmp/qsign-logs-$TIMESTAMP"
mkdir -p "$LOG_DIR"

echo "🔍 QSIGN 로그 수집 시작..."

# App5 로그
echo "📱 App5 로그 수집 중..."
kubectl logs -n q-app deployment/app5 --tail=500 > "$LOG_DIR/app5.log" 2>&1

# APISIX 로그
echo "🌐 APISIX 로그 수집 중..."
kubectl logs -n qsign-prod deployment/apisix --tail=500 > "$LOG_DIR/apisix.log" 2>&1
kubectl logs -n qsign-prod deployment/apisix-route-init --tail=200 > "$LOG_DIR/apisix-route-init.log" 2>&1

# Keycloak 로그
echo "🔐 Keycloak 로그 수집 중..."
kubectl logs -n q-sign deployment/keycloak-pqc --tail=1000 > "$LOG_DIR/keycloak.log" 2>&1

# Vault 로그
echo "🔑 Vault 로그 수집 중..."
kubectl logs -n q-kms deployment/q-kms --tail=500 > "$LOG_DIR/vault.log" 2>&1

# Pod 상태
echo "📊 Pod 상태 수집 중..."
kubectl get pods -n q-app -o wide > "$LOG_DIR/pods-q-app.txt"
kubectl get pods -n qsign-prod -o wide > "$LOG_DIR/pods-qsign-prod.txt"
kubectl get pods -n q-sign -o wide > "$LOG_DIR/pods-q-sign.txt"
kubectl get pods -n q-kms -o wide > "$LOG_DIR/pods-q-kms.txt"

# 서비스 정보
echo "🌐 서비스 정보 수집 중..."
kubectl get svc -n q-app > "$LOG_DIR/svc-q-app.txt"
kubectl get svc -n qsign-prod > "$LOG_DIR/svc-qsign-prod.txt"
kubectl get svc -n q-sign > "$LOG_DIR/svc-q-sign.txt"
kubectl get svc -n q-kms > "$LOG_DIR/svc-q-kms.txt"

# 로그 아카이브
echo "📦 로그 압축 중..."
tar -czf "$LOG_DIR.tar.gz" -C /tmp "qsign-logs-$TIMESTAMP"

echo "✅ 로그 수집 완료!"
echo "📁 위치: $LOG_DIR"
echo "📦 압축: $LOG_DIR.tar.gz"
EOF

chmod +x /tmp/collect-qsign-logs.sh
```

#### 실행
```bash
/tmp/collect-qsign-logs.sh
```

---

## 🔍 문제별 로그 확인 가이드

### 1. 로그인 실패 시

#### App5 확인
```bash
# 리다이렉트 URL 확인
kubectl logs -n q-app deployment/app5 | grep -i "redirect\|callback"
```

#### APISIX 확인
```bash
# Keycloak 라우팅 확인
kubectl logs -n qsign-prod deployment/apisix | grep "/realms/PQC-realm"
```

#### Keycloak 확인
```bash
# 인증 실패 로그
kubectl logs -n q-sign deployment/keycloak-pqc --tail=200 | grep -i "failed\|invalid\|denied"
```

### 2. JWT 토큰 발급 실패

#### Keycloak 확인
```bash
# 토큰 엔드포인트 로그
kubectl logs -n q-sign deployment/keycloak-pqc | grep "token endpoint"

# 서명 실패 확인
kubectl logs -n q-sign deployment/keycloak-pqc | grep "서명 실패\|signature fail"
```

#### Vault 확인
```bash
# Transit 서명 요청
kubectl logs -n q-kms deployment/q-kms | grep -i "transit/sign"

# 인증 실패
kubectl logs -n q-kms deployment/q-kms | grep "permission denied\|authentication failed"
```

### 3. APISIX 라우팅 실패

#### 라우트 존재 확인
```bash
curl -s "http://192.168.0.11:30282/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" | \
  python3 -c "import sys, json; routes = json.load(sys.stdin)['list']['list']; \
  print('Total routes:', len(routes)); \
  [print(f'{r[\"value\"][\"id\"]}: {r[\"value\"][\"uri\"]}') for r in routes]"
```

#### 업스트림 상태 확인
```bash
kubectl logs -n qsign-prod deployment/apisix | grep -i "upstream.*fail\|backend.*error"
```

### 4. Vault 연동 문제

#### 토큰 검증
```bash
# Vault 상태
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/sys/health"

# 토큰 유효성
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/auth/token/lookup-self" | python3 -m json.tool
```

#### Keycloak Vault 설정 확인
```bash
# Vault 환경 변수
kubectl describe pod -n q-sign <keycloak-pod-name> | grep -A 5 "VAULT"
```

---

## 🛠️ 유용한 로그 명령어 모음

### 시간 기반 필터링
```bash
# 최근 5분 로그
kubectl logs -n q-sign deployment/keycloak-pqc --since=5m

# 특정 시간 이후
kubectl logs -n q-sign deployment/keycloak-pqc --since-time='2025-11-19T10:00:00Z'
```

### 멀티 Pod 로그
```bash
# 모든 App5 Pod 로그
kubectl logs -n q-app -l app=app5 --all-containers=true --tail=100

# 이전 Pod 로그 (재시작된 경우)
kubectl logs -n q-sign deployment/keycloak-pqc --previous
```

### 로그 저장
```bash
# 파일로 저장
kubectl logs -n q-sign deployment/keycloak-pqc --tail=1000 > keycloak.log

# 타임스탬프 포함
kubectl logs -n q-sign deployment/keycloak-pqc --timestamps=true > keycloak-ts.log
```

### 로그 스트리밍 및 필터링
```bash
# 실시간 + 필터링
kubectl logs -f -n q-sign deployment/keycloak-pqc | grep -i "dilithium"

# 색상 하이라이트 (grep --color)
kubectl logs -f -n q-sign deployment/keycloak-pqc | grep --color -i "error\|warn\|fail"
```

---

## 📚 추가 참고자료

### Kubernetes 로그 관리
- [Kubernetes Logging Architecture](https://kubernetes.io/docs/concepts/cluster-administration/logging/)
- [kubectl logs 공식 문서](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#logs)

### APISIX 로깅
- [APISIX Logging Plugin](https://apisix.apache.org/docs/apisix/plugins/http-logger/)
- [APISIX Admin API](https://apisix.apache.org/docs/apisix/admin-api/)

### Keycloak 로깅
- [Keycloak Logging Configuration](https://www.keycloak.org/server/logging)
- [Keycloak Events](https://www.keycloak.org/docs/latest/server_admin/#user-events)

### Vault 로깅
- [Vault Audit Devices](https://developer.hashicorp.com/vault/docs/audit)
- [Vault Logging](https://developer.hashicorp.com/vault/docs/commands/server#logging)

---

## 🔗 관련 문서
- [QSIGN 아키텍처 문서](../01-architecture/)
- [통합 테스트 가이드](../01-gateway-flow/)
- [문제 해결 가이드](./TROUBLESHOOTING-GUIDE.md)

**버전**: 1.0.0
**최종 업데이트**: 2025-11-19
**작성자**: QSIGN Team
