# Q-SIGN Pod Pending 문제 해결

## 🔍 문제 진단

**ArgoCD 상태**: 🔄 Progressing, 🟢 Synced
**Pod 상태**: ⏸️ Pending (5 minutes)

### 발견된 문제

**hostNetwork 설정 충돌** - Keycloak Deployment에 `hostNetwork: true` 설정으로 인한 포트 충돌

```yaml
# 문제가 있던 설정 (helm/q-sign/templates/keycloak.yaml:205)
spec:
  hostNetwork: true
  dnsPolicy: ClusterFirstWithHostNet
```

**증상**:
- Pod가 pending 상태에서 시작하지 못함
- 호스트 네트워크 포트 충돌
- ArgoCD Progressing 상태 지속

**문제 원인**:
1. **hostNetwork: true** → Pod가 호스트의 네트워크 네임스페이스를 직접 사용
2. 이미 NodePort (30181) 사용 중이므로 hostNetwork 불필요
3. 호스트에서 8080 포트 충돌 가능성
4. 보안상 좋지 않은 설정

---

## ✅ 적용된 수정사항

### Git 커밋 완료 (2개)

**Repository**: http://192.168.0.11:7780/root/q-sign.git

#### Commit 1: 이미지 수정
```
Commit: 792054c
Message: 🔧 Fix Q-SIGN Keycloak image configuration
Changes:
  - repository: localhost:7800/qsign-prod/keycloak-hsm
  - tag: v1.2.0-hybrid
  - pullPolicy: IfNotPresent
```

#### Commit 2: hostNetwork 제거 ⭐
```
Commit: 8b493fb
Message: 🔧 Remove hostNetwork from Q-SIGN Keycloak deployment
Changes:
  - hostNetwork: true 제거
  - dnsPolicy: ClusterFirst
```

### 변경 내용

```yaml
# 수정된 설정
spec:
  dnsPolicy: ClusterFirst
  # hostNetwork 제거됨
```

**수정 이유**:
- ✅ NodePort 30181을 이미 사용하므로 hostNetwork 불필요
- ✅ 표준 Kubernetes 네트워킹으로 충분
- ✅ 포트 충돌 방지
- ✅ 보안 개선

---

## 🚀 ArgoCD Sync 실행

### 두 번째 Sync 필요!

이전 sync(792054c)는 이미지만 수정했습니다.
**새로운 sync(8b493fb)로 hostNetwork 제거가 적용됩니다.**

### ArgoCD UI에서 Sync 실행

1. **ArgoCD 접속**
   ```
   http://192.168.0.11:30080
   ```

2. **q-sign 애플리케이션 클릭**
   - 현재 상태: Progressing

3. **REFRESH 버튼 먼저 클릭**
   - Git 저장소에서 최신 변경사항 가져오기
   - Commit 8b493fb 인식 확인

4. **SYNC 버튼 클릭**
   - Sync 옵션:
     - ✅ PRUNE (사용하지 않는 리소스 제거)
     - ✅ FORCE (강제 동기화)
   - **"SYNCHRONIZE"** 버튼 클릭

5. **Sync 진행 상황 확인**
   - 기존 pending Pod 삭제
   - 새로운 Pod 생성 (hostNetwork 없음)
   - Pod가 Running 상태로 전환
   - Progressing → Healthy 전환

6. **완료 확인**
   - Health: ✅ Healthy
   - Sync Status: ✅ Synced
   - Commit: 8b493fb
   - Pod: Running (1/1)

---

## 🧪 Sync 후 검증

### 1. Pod 상태 확인

ArgoCD UI에서:
- keycloak-pqc Pod: ✅ Running
- Status: 1/1 Ready
- Age: 방금 전 (새로 생성됨)

### 2. 서비스 접근 테스트

```bash
# Q-SIGN Keycloak 테스트
curl -s http://192.168.0.11:30181/realms/myrealm | python3 -c "import sys,json; d=json.load(sys.stdin); print('Realm:', d.get('realm')); print('Token Service:', d.get('token-service'))"
```

**예상 출력**:
```
Realm: myrealm
Token Service: http://192.168.0.11:30181/realms/myrealm/protocol/openid-connect
```

### 3. 전체 플로우 테스트

```bash
/home/user/QSIGN/test-full-qsign-flow.sh
```

**예상 결과**:
```
Component                      Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Q-KMS Vault (8200)             ✓ PASS
Q-SIGN Keycloak (30181)        ✓ PASS  ← Pod Running!
Q-GATEWAY APISIX (80)          ○ RUNNING
Q-APP (30300)                  ✓ PASS
```

### 4. Health Endpoints 확인

```bash
# Health check
curl -s http://192.168.0.11:30181/health/live
curl -s http://192.168.0.11:30181/health/ready
```

**예상 출력**:
```json
{"status":"UP","checks":[]}
```

### 5. SSO 로그인 테스트

브라우저에서:
1. **Q-APP 접속**: http://192.168.0.11:30300
2. **Login 버튼 클릭**
3. **Q-SIGN으로 리디렉션**: 30181
4. **로그인**: testuser / admin
5. **성공 확인**: 사용자 정보 표시

---

## 🔧 문제 해결

### Pod가 여전히 Pending인 경우

**ArgoCD에서 강제 재배포**:
1. ArgoCD UI → q-sign → keycloak-pqc Pod
2. Pod 우클릭 → **Delete**
3. 새로운 Pod가 자동 생성됨
4. Pod 상태가 Running으로 전환되는지 확인

### Sync가 완료되었는데 이전 커밋(792054c)인 경우

**Git 저장소 수동 갱신**:
1. ArgoCD UI → q-sign
2. **REFRESH** 버튼 클릭
3. Commit이 8b493fb로 업데이트되는지 확인
4. 다시 **SYNC** 버튼 클릭

### Pod가 CrashLoopBackOff인 경우

**Pod 로그 확인**:
- ArgoCD UI → Pod 클릭 → Logs 탭
- 에러 메시지 확인

**가능한 원인**:
- PostgreSQL 연결 실패 → postgres-qsign 서비스 확인
- Redis 연결 실패 → values.yaml에서 redis.enabled=false이므로 무시됨
- 이미지 문제 → localhost:7800/qsign-prod/keycloak-hsm:v1.2.0-hybrid 확인

---

## 📊 변경 이력 요약

### 2025-11-17 11:00 - 이미지 수정 (1차)

**문제**: ImagePullBackOff
**수정**: 작동하는 이미지로 변경
**Commit**: 792054c

### 2025-11-17 11:10 - hostNetwork 제거 (2차) ⭐

**문제**: Pod Pending - 포트 충돌
**수정**: hostNetwork: true 제거
**Commit**: 8b493fb

---

## 🎯 예상 결과

두 번째 Sync 완료 후:

```
┌─────────────────┐
│   Q-SIGN        │  ✅ Healthy
│  Keycloak       │
│   (30181)       │  Pod: Running (1/1)
└────────┬────────┘  Image: keycloak-hsm:v1.2.0-hybrid
         │            Network: Kubernetes (NodePort)
         ↓
┌─────────────────┐
│  PostgreSQL     │  ✅ Running
│  (postgres)     │  DB: keycloak
└─────────────────┘
```

**ArgoCD 상태**:
- Health: ✅ Healthy (Progressing → Healthy)
- Sync: ✅ Synced
- Commit: 8b493fb
- Resources: All healthy

**네트워킹**:
- NodePort: 30181 ✅
- Service: keycloak-pqc ✅
- Endpoint: http://192.168.0.11:30181 ✅

**Pod**:
- Status: Running ✅
- Ready: 1/1 ✅
- Restarts: 0 ✅
- hostNetwork: false ✅

---

## ✅ 완료 체크리스트

- [x] 문제 진단 완료
- [x] 이미지 설정 수정 (1차)
- [x] hostNetwork 제거 (2차)
- [x] Git 커밋 완료 (2개)
- [x] GitLab 푸시 완료
- [ ] **ArgoCD REFRESH 실행** ← 다음 단계
- [ ] **ArgoCD SYNC 실행** ← 다음 단계
- [ ] Pod Running 상태 확인
- [ ] Healthy 상태 확인
- [ ] Keycloak 기능 테스트
- [ ] SSO 로그인 테스트

---

## 📝 기술적 배경

### hostNetwork가 문제인 이유

**hostNetwork: true**:
- Pod가 호스트의 네트워크 네임스페이스 사용
- Pod의 포트가 호스트의 포트와 직접 충돌
- 보안상 권장되지 않음 (Pod가 호스트 네트워크에 직접 접근)
- Replica를 여러 개 실행할 수 없음 (같은 포트 충돌)

**NodePort vs hostNetwork**:
- NodePort: Kubernetes가 관리하는 포트 매핑 (권장)
- hostNetwork: Pod가 호스트 네트워크 직접 사용 (특수한 경우만)

**Q-SIGN의 경우**:
- NodePort 30181 이미 설정됨
- hostNetwork 불필요
- 표준 Kubernetes 네트워킹으로 충분

---

**생성 시각**: 2025-11-17 11:10
**최종 커밋**: 8b493fb
**상태**: Ready for 2nd ArgoCD Sync
**다음 단계**: ArgoCD UI에서 REFRESH → SYNC 실행
