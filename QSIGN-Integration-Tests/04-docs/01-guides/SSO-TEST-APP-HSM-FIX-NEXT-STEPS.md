# SSO Test App - Luna HSM 수정 완료 및 다음 단계

**작성일**: 2025-11-19
**커밋**: 59e09e4
**상태**: ✅ Luna HSM URL 수정 완료, 🔄 배포 대기 중

---

## ✅ 완료된 작업

### 1. 문제 진단 ✅
- **근본 원인**: Luna HSM 호스트명, 네임스페이스, 포트 불일치
- **진단 보고서**: [SSO-TEST-APP-HSM-VAULT-ERROR-DIAGNOSIS.md](../02-results/SSO-TEST-APP-HSM-VAULT-ERROR-DIAGNOSIS.md)

### 2. 코드 수정 ✅
**파일**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/templates/keycloak.yaml`

**변경 내용** (Line 328):
```yaml
# 변경 전
- name: LUNA_HSM_URL
  value: "http://luna-hsm:{{ .Values.lunaHsm.service.port }}"

# 변경 후
- name: LUNA_HSM_URL
  value: "http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090"
```

### 3. Git Commit 및 Push ✅
```bash
Commit: 59e09e4
Message: 🔧 Fix Luna HSM connection: Use correct FQDN and port

변경된 파일: Q-SIGN/helm/q-sign/templates/keycloak.yaml
Push 완료: http://192.168.0.11:7780/root/q-sign.git
```

---

## 🔄 다음 단계 (사용자 실행 필요)

### 단계 1: ArgoCD Sync 실행

ArgoCD가 새 변경사항을 감지하고 배포하도록 합니다.

#### Option A: ArgoCD Auto-Sync 대기 (자동)
```
ArgoCD가 자동으로 Git 변경사항을 감지하고 Sync합니다.
대기 시간: 약 3-5분
```

#### Option B: ArgoCD UI에서 수동 Sync (권장)
```
1. 브라우저에서 ArgoCD UI 접속
2. q-sign 애플리케이션 선택
3. SYNC 버튼 클릭
4. Hard Refresh 옵션 선택
5. SYNCHRONIZE 실행
```

#### Option C: ArgoCD CLI로 Sync (명령어)
```bash
argocd app sync q-sign --prune --force
```

---

### 단계 2: Keycloak Pod 재시작 확인

ArgoCD Sync 후 Keycloak Pod가 자동으로 재시작되는지 확인합니다.

```bash
# Pod 상태 확인
sudo k3s kubectl get pods -n q-sign | grep keycloak-pqc

# 기대 출력:
# keycloak-pqc-xxxxx-xxxxx   1/1   Running   0   1m
```

**Pod가 재시작되지 않은 경우** (수동 재시작):
```bash
sudo k3s kubectl rollout restart deployment/keycloak-pqc -n q-sign

# 재시작 확인
sudo k3s kubectl rollout status deployment/keycloak-pqc -n q-sign
```

---

### 단계 3: Keycloak 로그 확인

새로운 Keycloak Pod에서 Luna HSM 연결이 성공했는지 확인합니다.

```bash
# 실시간 로그 모니터링 (Luna HSM 관련만)
sudo k3s kubectl logs -n q-sign -l app=keycloak-pqc --tail=100 -f | grep -E '(Luna|HSM)'

# 기대 출력:
# 🔐 Luna HSM Client 초기화 (주소: http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090, 슬롯: PQC-HSM-Slot-1)
# ✅ Luna HSM 연결 성공 (HTTP 200)
```

**중요**: "UnknownHostException: luna-hsm" 에러가 더 이상 나타나지 않아야 합니다!

---

### 단계 4: SSO Test App 로그인 테스트

SSO Test App에서 실제 로그인을 테스트하여 에러가 해결되었는지 확인합니다.

#### 브라우저 테스트
```
1. 브라우저에서 http://192.168.0.11:30300 접속
2. "로그인" 버튼 클릭
3. Keycloak 로그인 페이지로 리다이렉트
4. 사용자 인증 정보 입력:
   - Username: testuser
   - Password: admin
5. 로그인 버튼 클릭
6. SSO Test App으로 리다이렉트 확인
7. "오류 발생" 메시지 없이 정상 표시 확인
```

#### curl 테스트
```bash
# SSO Test App 접근 테스트
curl -v http://192.168.0.11:30300

# 기대 응답: HTTP 200 OK
# SSO Test App 메인 페이지 HTML
```

---

### 단계 5: Keycloak 서명 로그 확인 (중요!)

실제 로그인 시 Luna HSM을 통해 서명이 생성되는지 확인합니다.

```bash
# 로그인 직후 Keycloak 로그 확인
sudo k3s kubectl logs -n q-sign -l app=keycloak-pqc --tail=50 | grep -E '(JWT 서명|Luna|DILITHIUM3|서명 생성)'

# 기대 출력 (성공 케이스):
# 🔐 JWT 서명 시작 (Dilithium3 via Vault + HSM, 데이터 크기: 787 bytes)
# 🔐 Luna HSM DILITHIUM3 서명 시작 (데이터 크기: 787 bytes)
# ✅ Luna HSM DILITHIUM3 서명 생성 완료 (크기: 3293 bytes)
```

**실패 시 나타나는 로그** (이전 상태):
```
❌ 더 이상 나타나지 않아야 함:
⚠️  Luna HSM 서명 실패, 로컬 서명으로 대체: java.net.UnknownHostException: luna-hsm
⚠️ Vault 서명이 null/empty 반환, 로컬 서명으로 대체
✅ 로컬 DILITHIUM3 서명 생성 완료 (크기: 3293 bytes)
```

---

## 🧪 검증 체크리스트

### ArgoCD 배포 확인
- [ ] ArgoCD에서 q-sign 앱이 "Synced" 상태
- [ ] Keycloak Pod가 재시작되었음 (새로운 Pod 이름)
- [ ] Pod 상태가 "Running" (1/1)

### Luna HSM 연결 확인
- [ ] Keycloak 로그에 "Luna HSM Client 초기화" 메시지
- [ ] LUNA_HSM_URL = `http://luna-hsm-simulator.pqc-sso.svc.cluster.local:8090`
- [ ] "✅ Luna HSM 연결 성공 (HTTP 200)" 로그 표시
- [ ] "UnknownHostException: luna-hsm" 에러 **없음**

### SSO Test App 로그인 테스트
- [ ] SSO Test App 접속 성공 (http://192.168.0.11:30300)
- [ ] 로그인 버튼 클릭 → Keycloak 리다이렉트
- [ ] Keycloak 로그인 성공 (testuser / admin)
- [ ] SSO Test App 콜백 성공
- [ ] "오류 발생" 메시지 **없음**

### Luna HSM 서명 생성 확인
- [ ] Keycloak 로그에 "Luna HSM DILITHIUM3 서명 시작" 메시지
- [ ] "✅ Luna HSM DILITHIUM3 서명 생성 완료 (크기: 3293 bytes)"
- [ ] "로컬 서명으로 대체" 경고 **없음**

---

## 🚨 문제 해결 (Troubleshooting)

### 문제 1: ArgoCD Sync 후에도 Pod가 재시작되지 않음

**원인**: ConfigMap이나 Deployment의 template이 변경되지 않아 Pod가 자동으로 재시작되지 않음.

**해결**:
```bash
# 수동으로 Pod 재시작
sudo k3s kubectl rollout restart deployment/keycloak-pqc -n q-sign

# 또는 Pod 직접 삭제 (자동으로 재생성됨)
sudo k3s kubectl delete pod -n q-sign -l app=keycloak-pqc
```

---

### 문제 2: Luna HSM 연결 실패 (HTTP 404/500)

**증상**:
```
⚠️  Luna HSM 연결 테스트 실패: HTTP 404
```

**원인**: Luna HSM Simulator가 `pqc-sso` 네임스페이스에 배포되지 않았거나 실행 중이 아님.

**확인**:
```bash
# Luna HSM Simulator Pod 확인
sudo k3s kubectl get pods -n pqc-sso | grep luna-hsm

# Luna HSM Simulator Service 확인
sudo k3s kubectl get svc -n pqc-sso | grep luna-hsm
```

**해결**:
```bash
# Luna HSM Simulator 배포
kubectl apply -f /home/user/QSIGN/keycloak-hsm/deployments/luna-hsm-simulator-deployment.yaml

# 또는 Helm으로 배포 (pqc-sso 네임스페이스)
helm install luna-hsm /path/to/luna-hsm-chart -n pqc-sso
```

---

### 문제 3: Vault Transit 여전히 HTTP 403

**증상**:
```
❌ Vault 서명 실패 (HTTP 403)
```

**현재 상태**: Luna HSM이 정상 작동하면 Vault는 사용되지 않음 (Fallback 경로).

**해결 (Optional)**:
- Luna HSM이 정상 작동하면 Vault 403 에러는 무시 가능
- Vault를 사용하려면 별도로 토큰 및 정책 설정 필요
- 상세 내용은 진단 보고서의 "옵션 2: Vault 인증 수정" 참조

---

### 문제 4: SSO Test App 여전히 에러 발생

**증상**: 로그인 후 "오류 발생" 메시지 지속

**원인 확인**:
```bash
# SSO Test App 로그 확인
sudo k3s kubectl logs -n pqc-sso -l app=sso-test-app --tail=100

# Keycloak 로그 확인
sudo k3s kubectl logs -n q-sign -l app=keycloak-pqc --tail=100 | grep -E '(ERROR|WARN)'
```

**가능한 원인**:
1. SSO Test App의 Keycloak Client 설정 오류
2. Redirect URI 불일치
3. Realm 또는 Client ID 불일치
4. Keycloak에서 여전히 다른 에러 발생

---

## 📊 서명 플로우 비교

### 이전 (문제 상황) ❌
```
Keycloak → Luna HSM (실패 ❌) → Vault Transit (실패 ❌) → 로컬 서명 (성공 ✅)
          UnknownHostException     HTTP 403              3,293 bytes
```

### 현재 (목표 상태) ✅
```
Keycloak → Luna HSM (성공 ✅) → JWT 서명 완료
          3,293 bytes DILITHIUM3
```

---

## 📝 다음 작업 (Optional)

### 1. Vault Transit 403 문제 해결
- 우선순위: 낮음 (Luna HSM이 1차 경로이므로 선택적)
- 상세 가이드: [SSO-TEST-APP-HSM-VAULT-ERROR-DIAGNOSIS.md](../02-results/SSO-TEST-APP-HSM-VAULT-ERROR-DIAGNOSIS.md)

### 2. 다른 애플리케이션 테스트
```bash
# App3 테스트
curl http://192.168.0.11:30202

# App4 테스트
curl http://192.168.0.11:30203

# App5 테스트
curl http://192.168.0.11:30204

# App6 (Luna HSM Verifier) 테스트
curl http://192.168.0.11:30205

# App7 (HSM PQC Integration) 테스트
curl http://192.168.0.11:30207
```

### 3. 프로덕션 보안 강화
- Luna HSM Simulator → 실제 Luna HSM 하드웨어 연동
- Vault Dev 모드 → Vault 프로덕션 모드 전환
- HSM PIN 및 슬롯 정보를 Kubernetes Secret으로 관리
- TLS/HTTPS 활성화

---

## 🎯 예상 결과

### 성공 시
- ✅ SSO Test App 로그인 성공
- ✅ Keycloak가 Luna HSM Simulator를 통해 DILITHIUM3 서명 생성
- ✅ "오류 발생" 메시지 사라짐
- ✅ JWT 토큰 정상 발급
- ✅ 로그에 "UnknownHostException" 에러 없음

### 관련 문서
- [SSO Test App HSM/Vault 에러 진단 보고서](../02-results/SSO-TEST-APP-HSM-VAULT-ERROR-DIAGNOSIS.md)
- [QSIGN Integration Tests README](../../README.md)
- [Kubernetes 로그 확인 가이드](./KUBERNETES-LOGS-GUIDE.md)

---

**요약**: Luna HSM URL 수정이 완료되었습니다. ArgoCD Sync를 실행하고 Keycloak Pod를 재시작한 후 SSO Test App 로그인을 테스트하세요!
