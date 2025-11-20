# SSO Test App - HSM/Vault 서명 실패 진단 보고서

**작성일**: 2025-11-19
**문제**: SSO Test App 로그인 후 "오류 발생" 메시지
**상태**: 🔍 근본 원인 식별 완료

---

## 📊 문제 요약

SSO Test App에서 로그인 후 에러가 발생하며, Keycloak 로그 분석 결과 **Luna HSM 연결 실패** 및 **Vault Transit 인증 실패**로 인해 로컬 서명으로 대체되고 있음.

### Keycloak 로그 (keycloak-pqc-748dcf4fbd-nnbh6)

```
2025-11-19 00:46:07,269 INFO  [com.pqc.keycloak.crypto.Dilithium3SignatureProvider]
🔐 JWT 서명 시작 (Dilithium3 via Vault + HSM, 데이터 크기: 787 bytes)

2025-11-19 00:46:07,269 WARN  [com.pqc.keycloak.vault.LunaHsmClient]
⚠️  Luna HSM 서명 실패, 로컬 서명으로 대체: java.net.UnknownHostException: luna-hsm

2025-11-19 00:46:07,269 WARN  [com.pqc.keycloak.vault.VaultClient]
⚠️  Luna HSM 서명이 null 반환, Vault Transit으로 대체

2025-11-19 00:46:07,270 ERROR [com.pqc.keycloak.vault.VaultClient]
❌ Vault 서명 실패 (HTTP 403)

2025-11-19 00:46:07,270 WARN  [com.pqc.keycloak.crypto.Dilithium3SignatureProvider]
⚠️ Vault 서명이 null/empty 반환, 로컬 서명으로 대체

2025-11-19 00:46:07,270 INFO  [com.pqc.keycloak.crypto.Dilithium3SignatureProvider]
✅ 로컬 DILITHIUM3 서명 생성 완료 (크기: 3293 bytes)
```

### 3단계 서명 실패 체인

```
1️⃣ Luna HSM 서명 시도 ❌
   ↓ UnknownHostException: luna-hsm

2️⃣ Vault Transit 서명 대체 ❌
   ↓ HTTP 403 Forbidden

3️⃣ 로컬 DILITHIUM3 서명 ✅
   ↓ 성공 (3,293 bytes)
```

**결과**: 로컬 서명은 작동하지만, HSM/Vault 통합이 실패하여 프로덕션 수준의 보안이 제공되지 않음.

---

## 🔍 근본 원인 분석

### 문제 1: Luna HSM 연결 실패 ❌

#### 원인
**DNS 호스트명 불일치 + 네임스페이스 분리 + 포트 불일치**

#### 상세 분석

| 항목 | Keycloak 설정 | 실제 서비스 | 상태 |
|------|--------------|-----------|------|
| **호스트명** | `luna-hsm` | `luna-hsm-simulator` | ❌ 불일치 |
| **네임스페이스** | `q-sign` (Keycloak Pod) | `pqc-sso` (Luna HSM) | ❌ 크로스 네임스페이스 |
| **포트** | `8080` | `8090` (HTTP API) | ❌ 불일치 |
| **DNS 해석** | `luna-hsm` | 존재하지 않음 | ❌ UnknownHostException |

#### Keycloak 설정 위치
**파일**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/templates/keycloak.yaml`

```yaml
# Line 328
- name: LUNA_HSM_URL
  value: "http://luna-hsm:{{ .Values.lunaHsm.service.port }}"
```

**values.yaml**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/values.yaml`

```yaml
# Lines 150-161
lunaHsm:
  enabled: false  # ⚠️ 비활성화됨
  replicaCount: 1

  image:
    repository: nginx  # ⚠️ 실제 Luna HSM 이미지 아님
    tag: "alpine"

  service:
    type: ClusterIP
    port: 8080  # ⚠️ 실제 포트(8090)와 불일치
```

**문제점**:
1. `lunaHsm.enabled: false` - Luna HSM이 Q-SIGN Helm Chart에서 비활성화됨
2. 호스트명이 짧은 이름(`luna-hsm`)으로 설정되어 있어 같은 네임스페이스에서만 해석 가능
3. 실제 Luna HSM은 다른 네임스페이스(`pqc-sso`)에 배포됨

#### 실제 Luna HSM Simulator 서비스
**파일**: `/home/user/QSIGN/keycloak-hsm/deployments/luna-hsm-simulator-deployment.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: luna-hsm-simulator  # ← 실제 서비스 이름
  namespace: pqc-sso        # ← 실제 네임스페이스
spec:
  selector:
    app: luna-hsm-simulator
  ports:
  - port: 8090              # ← HTTP API 포트
    targetPort: 8090
    name: http
  - port: 1792              # ← PKCS11 포트
    targetPort: 1792
    name: pkcs11
  type: ClusterIP
```

#### Java 코드에서의 기본값
**파일**: `/home/user/QSIGN/Q-SIGN/keycloak-pqc-provider/src/main/java/com/pqc/keycloak/vault/LunaHsmClient.java`

```java
// Line 68
this.hsmUrl = System.getenv().getOrDefault("LUNA_HSM_URL", "http://luna-hsm:1792");
```

**환경 변수가 설정되지 않으면**: `http://luna-hsm:1792` 사용
**실제 필요한 값**: `http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090`

---

### 문제 2: Vault Transit HTTP 403 ❌

#### 원인
**Vault 인증 토큰 권한 부족 또는 만료**

#### 상세 분석

| 항목 | 설정 | 상태 |
|------|------|------|
| **Vault 주소** | `http://q-kms.q-kms.svc.cluster.local:8200` | ✅ FQDN 올바름 |
| **Vault 활성화** | `VAULT_ENABLED: true` | ✅ 활성화됨 |
| **Vault 토큰** | `VAULT_DEV_ROOT_TOKEN_ID: root` | ⚠️ 기본값 (개발용) |
| **Transit 키** | `dilithium-key` | ✅ 설정됨 |
| **HTTP 응답** | `403 Forbidden` | ❌ 인증/권한 실패 |

#### Keycloak Vault 설정
**파일**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/templates/keycloak.yaml`

```yaml
# Lines 306-321
- name: VAULT_ENABLED
  value: "true"
- name: VAULT_ADDR
  value: "http://q-kms.q-kms.svc.cluster.local:8200"
{{- if .Values.keycloak.existingVaultSecret }}
- name: VAULT_DEV_ROOT_TOKEN_ID
  valueFrom:
    secretKeyRef:
      name: {{ .Values.keycloak.existingVaultSecret }}
      key: root-token
{{- else }}
- name: VAULT_DEV_ROOT_TOKEN_ID
  value: "root"  # ⚠️ 개발용 기본 토큰
{{- end }}
- name: VAULT_TRANSIT_KEY
  value: "dilithium-key"
- name: VAULT_HSM_ENABLED
  value: "true"
```

**문제점**:
1. **기본 토큰 사용**: `VAULT_DEV_ROOT_TOKEN_ID: "root"`는 개발 환경용
2. **HTTP 403**: Vault Transit Engine 접근 권한 부족
3. **가능한 원인**:
   - Vault가 `dev` 모드가 아닌 프로덕션 모드로 실행 중
   - `root` 토큰이 무효화되었거나 변경됨
   - Transit Engine이 활성화되지 않았거나 정책이 설정되지 않음

---

## 🎯 해결 방안

### 옵션 1: Luna HSM 연결 수정 (권장) ✅

#### 단계 1: Keycloak Deployment YAML 수정

**파일**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/templates/keycloak.yaml`

**변경 전** (Line 328):
```yaml
- name: LUNA_HSM_URL
  value: "http://luna-hsm:{{ .Values.lunaHsm.service.port }}"
```

**변경 후**:
```yaml
- name: LUNA_HSM_URL
  value: "http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090"
```

#### 단계 2: Git Commit 및 Push

```bash
cd /home/user/QSIGN/Q-SIGN
git add helm/q-sign/templates/keycloak.yaml
git commit -m "🔧 Fix Luna HSM URL: Use correct FQDN and port

- 호스트: luna-hsm → luna-hsm-simulator.pqc-sso.svc.cluster.local
- 포트: 8080 → 8090 (HTTP API)
- 네임스페이스: q-sign → pqc-sso (크로스 네임스페이스 FQDN)

Fixes: java.net.UnknownHostException: luna-hsm"

git push
```

#### 단계 3: ArgoCD Sync 및 Pod 재시작

```bash
# Option A: ArgoCD UI에서 Sync (권장)
# q-sign 애플리케이션 → SYNC → Hard Refresh

# Option B: kubectl로 Pod 재시작 (권한 필요)
kubectl rollout restart deployment/keycloak-pqc -n q-sign
```

#### 단계 4: 로그 확인

```bash
# Keycloak 로그 실시간 모니터링
sudo kubectl logs -n q-sign keycloak-pqc-748dcf4fbd-nnbh6 -f | grep -E '(Luna|HSM|서명)'

# 기대 결과:
# ✅ Luna HSM 연결 성공 (HTTP 200)
# ✅ Luna HSM DILITHIUM3 서명 생성 완료 (크기: 3293 bytes)
```

---

### 옵션 2: Vault 인증 수정

#### 단계 1: Vault 상태 확인

**Vault가 `q-kms` 네임스페이스에 있는지 확인**:
```bash
kubectl get svc -n q-kms | grep -i vault
# 또는
kubectl get pods -A | grep -i vault
```

#### 단계 2: Vault 토큰 확인 및 갱신

**Vault가 dev 모드인 경우**:
```bash
# Vault Pod 접속
kubectl exec -it <vault-pod-name> -n q-kms -- sh

# Root 토큰 확인
vault login root
vault token lookup

# Transit Engine 활성화 확인
vault secrets list
vault read transit/keys/dilithium-key
```

**Vault가 프로덕션 모드인 경우**:
```bash
# 새 토큰 생성 (Transit 권한 포함)
vault token create -policy=transit-policy

# 생성된 토큰을 Kubernetes Secret에 저장
kubectl create secret generic vault-token \
  -n q-sign \
  --from-literal=root-token=<NEW_TOKEN>

# values.yaml 수정
# existingVaultSecret: "vault-token"
```

#### 단계 3: Transit Policy 생성 (필요 시)

```bash
# Vault Policy 생성
vault policy write transit-policy - <<EOF
path "transit/sign/dilithium-key" {
  capabilities = ["update"]
}

path "transit/verify/dilithium-key" {
  capabilities = ["update"]
}

path "transit/keys/dilithium-key" {
  capabilities = ["read"]
}
EOF

# Policy가 토큰에 적용되었는지 확인
vault token lookup
```

---

### 옵션 3: 로컬 서명으로 유지 (현재 상태)

**장점**:
- ✅ 이미 작동 중 (로컬 DILITHIUM3 서명 성공)
- ✅ 추가 수정 불필요
- ✅ Keycloak이 정상적으로 JWT 발급

**단점**:
- ❌ HSM 하드웨어 보안 미사용
- ❌ Vault KMS 통합 미활용
- ❌ 프로덕션 보안 수준 미달
- ❌ 키가 메모리에 평문으로 저장됨

**현재 동작**:
```
Keycloak → Luna HSM (실패) → Vault Transit (실패) → 로컬 서명 (성공)
```

**로컬 서명 구현**: Bouncy Castle PQC Provider 사용
**서명 크기**: 3,293 bytes (DILITHIUM3 표준)
**보안 수준**: 소프트웨어 기반 (HSM 미사용)

---

## 📋 권장 조치 사항

### 우선순위 1: Luna HSM 연결 수정 🔴

**이유**:
- 가장 명확한 문제 (DNS + 포트 불일치)
- 수정이 간단함 (YAML 1줄 변경)
- 즉시 테스트 가능

**기대 효과**:
- Luna HSM Simulator를 통한 PQC 서명 작동
- 하드웨어 보안 모듈 시뮬레이션 정상화

### 우선순위 2: Vault Transit 인증 수정 🟡

**이유**:
- Vault 설정이 복잡할 수 있음
- 토큰 및 정책 설정 필요
- Vault가 어떤 모드로 실행 중인지 확인 필요

**기대 효과**:
- Vault Transit Engine을 통한 중앙화된 키 관리
- HSM과 Vault 이중 백업 체계 구축

### 우선순위 3: 전체 플로우 테스트 🟢

**테스트 시나리오**:
1. Luna HSM 수정 후 → SSO Test App 로그인 → 로그 확인
2. Vault 수정 후 → SSO Test App 로그인 → 로그 확인
3. 전체 통합 → 3단계 서명 체인 정상 작동 확인

```
목표 흐름:
Keycloak → Luna HSM (성공 ✅) → JWT 서명 완료
```

또는
```
대체 흐름:
Keycloak → Luna HSM (실패) → Vault Transit (성공 ✅) → JWT 서명 완료
```

---

## 🔧 즉시 실행 가능한 수정 명령어

### 1. Luna HSM URL 수정

```bash
cd /home/user/QSIGN/Q-SIGN

# YAML 파일 수정
sed -i 's|value: "http://luna-hsm:{{ .Values.lunaHsm.service.port }}"|value: "http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090"|' \
  helm/q-sign/templates/keycloak.yaml

# 변경 확인
grep -A 1 "LUNA_HSM_URL" helm/q-sign/templates/keycloak.yaml

# Git commit
git add helm/q-sign/templates/keycloak.yaml
git commit -m "🔧 Fix Luna HSM connection: Use correct FQDN and port"
git push

# ArgoCD Sync
# (ArgoCD UI에서 수동 Sync 또는 자동 Sync 대기)
```

### 2. Keycloak Pod 재시작 (ArgoCD Sync 후)

```bash
# Option A: ArgoCD UI
# q-sign → keycloak-pqc → Actions → Restart

# Option B: kubectl (권한 필요)
kubectl rollout restart deployment/keycloak-pqc -n q-sign

# Pod 상태 확인
kubectl get pods -n q-sign | grep keycloak-pqc
```

### 3. 로그 실시간 모니터링

```bash
# 새 Pod의 로그 확인 (Pod 이름은 변경될 수 있음)
kubectl logs -n q-sign -l app=keycloak-pqc --tail=100 -f | grep -E '(Luna|HSM|Vault|서명|ERROR|WARN)'

# 기대 출력:
# ✅ Luna HSM Client 초기화 (주소: http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090)
# ✅ Luna HSM 연결 성공 (HTTP 200)
# ✅ Luna HSM DILITHIUM3 서명 생성 완료 (크기: 3293 bytes)
```

### 4. SSO Test App 테스트

```bash
# 브라우저에서 접속
open http://192.168.0.11:30300

# 또는 curl로 테스트
curl -v http://192.168.0.11:30300

# 로그인 플로우:
# 1. SSO Test App → Keycloak 리다이렉트
# 2. Keycloak 로그인 (testuser / admin)
# 3. Callback 성공
# 4. 에러 없이 메인 페이지 표시
```

---

## 📊 아키텍처 비교

### 현재 상태 (로컬 서명) ❌

```
┌─────────────┐
│ SSO Test App│
│  (30300)    │
└──────┬──────┘
       │ OAuth2
       ↓
┌─────────────┐
│ Keycloak    │
│  (30181)    │
│             │
│ Luna HSM ❌ │ → UnknownHostException: luna-hsm
│ Vault ❌    │ → HTTP 403 Forbidden
│ Local ✅    │ → DILITHIUM3 서명 (3,293 bytes)
└─────────────┘
```

### 목표 상태 (HSM 서명) ✅

```
┌─────────────┐
│ SSO Test App│
│  (30300)    │
└──────┬──────┘
       │ OAuth2
       ↓
┌─────────────┐     ┌──────────────────┐
│ Keycloak    │     │ Luna HSM         │
│  (30181)    │────→│ Simulator        │
│             │ 8090│ (pqc-sso:8090)   │
│ Luna HSM ✅ │←────│ DILITHIUM3       │
│             │     │ 서명 생성         │
└─────────────┘     └──────────────────┘
       │
       │ (Optional Fallback)
       ↓
┌──────────────┐
│ Vault Transit│
│ (q-kms:8200) │
│ dilithium-key│
└──────────────┘
```

---

## 🧪 검증 체크리스트

### Luna HSM 연결 테스트
- [ ] Keycloak YAML에서 `LUNA_HSM_URL` 수정 확인
- [ ] Git commit 및 push 완료
- [ ] ArgoCD Sync 완료
- [ ] Keycloak Pod 재시작 완료
- [ ] Pod 로그에서 "✅ Luna HSM 연결 성공" 확인
- [ ] SSO Test App 로그인 성공
- [ ] Keycloak 로그에 "Luna HSM DILITHIUM3 서명 생성 완료" 표시

### Vault Transit 테스트 (Optional)
- [ ] Vault Pod 확인 (q-kms 네임스페이스)
- [ ] Vault Transit Engine 활성화 확인
- [ ] `dilithium-key` 존재 확인
- [ ] Vault 토큰 유효성 확인
- [ ] Transit 정책 확인
- [ ] Keycloak 로그에 Vault 403 에러 없음

### 전체 통합 테스트
- [ ] SSO Test App 로그인 → 에러 없음
- [ ] JWT 토큰 발급 성공
- [ ] Keycloak 로그에 "로컬 서명으로 대체" 경고 없음
- [ ] DILITHIUM3 서명 크기 3,293 bytes 확인

---

## 📝 추가 참고 자료

### 관련 파일
- **Keycloak Deployment**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/templates/keycloak.yaml`
- **Keycloak Values**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/values.yaml`
- **Luna HSM Simulator**: `/home/user/QSIGN/keycloak-hsm/deployments/luna-hsm-simulator-deployment.yaml`
- **Luna HSM Client Code**: `/home/user/QSIGN/Q-SIGN/keycloak-pqc-provider/src/main/java/com/pqc/keycloak/vault/LunaHsmClient.java`

### Kubernetes 네임스페이스 구조
| 네임스페이스 | 주요 서비스 | 용도 |
|------------|-----------|------|
| `q-sign` | keycloak-pqc, postgres-qsign | 메인 인증 시스템 |
| `pqc-sso` | luna-hsm-simulator, sso-test-app | HSM 및 테스트 앱 |
| `q-kms` | vault, q-kms | 키 관리 시스템 |
| `q-app` | app3~7 | QSIGN 통합 애플리케이션 |

### DNS 해석 규칙
```
짧은 이름 (luna-hsm):
  → 같은 네임스페이스 내에서만 해석
  → keycloak-pqc (q-sign) → luna-hsm (q-sign에만 존재)
  → pqc-sso의 luna-hsm-simulator는 찾을 수 없음 ❌

FQDN (luna-hsm-simulator.pqc-sso.svc.cluster.local):
  → 클러스터 전체에서 해석 가능
  → keycloak-pqc (q-sign) → luna-hsm-simulator (pqc-sso) ✅
```

---

**결론**: Luna HSM URL을 FQDN(`luna-hsm-simulator.pqc-sso.svc.cluster.local:8090`)으로 수정하면 SSO Test App의 HSM 서명 문제가 해결됩니다. Vault Transit 403 에러는 2차 우선순위로 별도 해결이 필요합니다.
