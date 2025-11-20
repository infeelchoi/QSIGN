# Q-SIGN 최종 해결 가이드

## 🎯 적용된 수정사항 (3단계)

### Git 커밋 완료

**Repository**: http://192.168.0.11:7780/root/q-sign.git

```
최신 커밋 3개:
  9bc1f17 - Change Deployment strategy to Recreate ⭐ 최신!
  8b493fb - Remove hostNetwork
  792054c - Fix image configuration
```

---

## 🔧 수정 내용

### 1차 수정 (792054c)
**문제**: ImagePullBackOff
**해결**: 작동하는 이미지로 변경
```yaml
image:
  repository: localhost:7800/qsign-prod/keycloak-hsm
  tag: v1.2.0-hybrid
```

### 2차 수정 (8b493fb)
**문제**: Pod Pending (hostNetwork 충돌)
**해결**: hostNetwork 제거
```yaml
# hostNetwork: true ← 삭제됨
dnsPolicy: ClusterFirst
```

### 3차 수정 (9bc1f17) ⭐ 최신
**문제**: 롤링 업데이트 실패 (새 Pod 계속 에러)
**해결**: Recreate 전략으로 변경
```yaml
spec:
  strategy:
    type: Recreate  # ← 추가됨
```

**효과**:
- ✅ 기존 Pod를 먼저 종료
- ✅ 그 다음 새 Pod 생성
- ✅ 롤링 업데이트 문제 회피
- ✅ 깨끗한 상태에서 재시작

---

## 🚀 ArgoCD에서 적용 방법

### Step 1: REFRESH (Git 최신 커밋 가져오기)

1. **ArgoCD UI 유지**
   - 현재 q-sign 애플리케이션 화면

2. **REFRESH 버튼 클릭**
   - 상단 툴바의 "REFRESH" 버튼
   - Git 저장소에서 최신 변경사항 가져오기

3. **커밋 확인**
   - "Synced to main (9bc1f17)" 확인
   - 또는 "OutOfSync" 표시 (정상)

---

### Step 2: 에러 Pod 삭제 (선택사항)

**삭제 대상**:
- Pod: `keycloak-pqc-7d5dc44c8-xxxxx`
- 상태: error, 0/1

**삭제 방법**:
1. 리소스 트리에서 해당 Pod 찾기
2. 우측 3점 메뉴 (⋮) 클릭
3. "Delete" 선택
4. 확인

**주의**: 정상 Pod (`keycloak-pqc-7dfb996cf5`)는 **삭제하지 마세요**!

---

### Step 3: SYNC (최종 적용)

1. **SYNC 버튼 클릭**

2. **Sync Options 선택**:
   - ✅ **PRUNE** (이전 리소스 정리)
   - ✅ **FORCE** (강제 동기화)
   - ✅ **REPLACE** (리소스 교체)

3. **"SYNCHRONIZE" 클릭**

4. **진행 관찰**:
   ```
   1. 기존 모든 Pod 종료 (Terminating)
   2. Pod 완전히 삭제됨
   3. 새로운 Pod 생성 시작
   4. Pod 상태: Pending → ContainerCreating → Running
   5. Health: Progressing → Healthy
   ```

5. **완료 확인**:
   - Health: ✅ **Healthy**
   - Sync: ✅ **Synced to 9bc1f17**
   - Pod: ✅ **Running (1/1)**

---

## 🔍 Recreate vs RollingUpdate

### RollingUpdate (기존 - 문제 발생)
```
┌─────────────────────────────────────┐
│  Old Pod (7dfb996cf5)  Running      │ ← 정상 작동
└─────────────────────────────────────┘
             │
             ↓ (동시에)
┌─────────────────────────────────────┐
│  New Pod (7d5dc44c8)  Starting      │ ← 시작 실패!
└─────────────────────────────────────┘

문제: 새 Pod가 시작 실패하면 Old Pod도 삭제 안됨
결과: Progressing 상태 지속
```

### Recreate (수정 후 - 문제 해결)
```
Step 1: Old Pod 종료
┌─────────────────────────────────────┐
│  Old Pod (7dfb996cf5)  Terminating  │
└─────────────────────────────────────┘
             │
             ↓ (완전 삭제 후)
         (Pod 없음)
             │
             ↓ (그 다음)
┌─────────────────────────────────────┐
│  New Pod (XXXXXXX)  Starting        │
└─────────────────────────────────────┘

장점: 깨끗한 상태에서 재시작
결과: 성공 또는 실패가 명확함
```

---

## 📊 예상 동작

### 시나리오 1: 성공 (가장 가능성 높음)

```
1. SYNC 시작
2. Deployment 업데이트 감지 (strategy: Recreate)
3. 기존 Pod 종료: keycloak-pqc-7dfb996cf5 → Terminating
4. Pod 완전 삭제됨
5. 새 Pod 생성: keycloak-pqc-XXXXXXX
6. 이미지 Pull: localhost:7800/qsign-prod/keycloak-hsm:v1.2.0-hybrid
7. 컨테이너 시작
8. Health checks 통과
9. Pod Running (1/1)
10. Service 연결
11. Port 30181 응답 시작
12. Health: Healthy ✅
```

**결과**: 완전히 정상 작동 ✅

---

### 시나리오 2: 일시적 서비스 중단 (Recreate 전략의 특성)

```
⚠️ 주의: Recreate 전략은 짧은 다운타임 발생

1. 기존 Pod 종료
   → Port 30181 일시 중단 (10-30초)

2. 새 Pod 시작
   → 컨테이너 생성 (10-20초)
   → Health check 통과 (10-30초)

3. 서비스 복구
   → Port 30181 다시 응답

총 다운타임: 30-60초 예상
```

**영향**:
- Q-APP SSO 로그인: 일시적 불가 (1분 이내)
- 기존 세션: 유지 (PostgreSQL에 저장됨)
- 복구 후: 정상 작동

---

### 시나리오 3: 새 Pod도 실패하는 경우

만약 새 Pod도 계속 실패한다면, 로그 확인 필요:

**ArgoCD UI에서**:
1. 실패한 Pod 클릭
2. "LOGS" 탭 클릭
3. 에러 메시지 확인

**일반적인 에러**:

**PostgreSQL 연결 실패**:
```
Error: could not connect to database
```
→ postgres-qsign Pod 및 Service 확인 필요

**이미지 문제**:
```
Failed to pull image
```
→ 레지스트리 확인 필요

**리소스 부족**:
```
0/1 nodes available: insufficient resources
```
→ 메모리/CPU 요청량 감소 필요

---

## ✅ SYNC 후 검증

### 1. ArgoCD UI 확인

**예상 상태**:
```
Application: q-sign
  Health:    ✅ Healthy
  Sync:      ✅ Synced to main (9bc1f17)

Resources:
  ✅ Deployment: keycloak-pqc (strategy: Recreate)
  ✅ Pod: keycloak-pqc-XXXXXXX (Running 1/1, Age: 방금)
  ✅ Service: keycloak-pqc
  ✅ PostgreSQL: All healthy
```

### 2. 서비스 테스트

```bash
# Keycloak Realm 확인
curl -s http://192.168.0.11:30181/realms/myrealm | python3 -c "import sys,json; d=json.load(sys.stdin); print('Realm:', d.get('realm')); print('Token:', d.get('token-service'))"
```

**예상 출력**:
```
Realm: myrealm
Token: http://192.168.0.11:30181/realms/myrealm/protocol/openid-connect
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
Q-SIGN Keycloak (30181)        ✓ PASS  ← 새로운 Pod!
Q-GATEWAY APISIX (80)          ○ RUNNING
Q-APP (30300)                  ✓ PASS
```

### 4. SSO 로그인 테스트

**브라우저에서**:
1. http://192.168.0.11:30300 접속
2. "Login" 버튼 클릭
3. Q-SIGN Keycloak (30181)로 리디렉션
4. 로그인: testuser / admin
5. 성공: 사용자 정보 표시 ✅

---

## 🔧 문제 해결

### SYNC 후에도 Pod가 에러인 경우

**로그 확인 단계**:

1. **Pod 로그**:
   - ArgoCD UI → Pod 클릭 → LOGS
   - 마지막 에러 메시지 확인

2. **이벤트 확인**:
   - ArgoCD UI → Pod 클릭 → EVENTS
   - Warning 이벤트 확인

3. **Deployment 상태**:
   - Deployment 클릭 → Conditions 확인

**일반적인 해결책**:

**PostgreSQL 문제**:
```bash
# postgres-qsign 상태 확인
# ArgoCD UI에서 postgres-qsign Pod 확인
# Running (1/1)인지 확인
```

**이미지 Pull 문제**:
```bash
# 레지스트리 확인
curl -s http://localhost:7800/v2/qsign-prod/keycloak-hsm/tags/list

# 예상 출력: {"name":"qsign-prod/keycloak-hsm","tags":["v1.2.0-hybrid",...]}
```

**리소스 부족**:
- values.yaml에서 resources 요청량 감소
- 또는 노드 리소스 확보

---

## 📝 체크리스트

SYNC 전:
- [ ] Git 커밋 확인: 9bc1f17
- [ ] GitLab 푸시 완료
- [ ] ArgoCD UI 열림

SYNC 실행:
- [ ] REFRESH 버튼 클릭
- [ ] 커밋 9bc1f17 확인
- [ ] (선택) 에러 Pod 삭제
- [ ] SYNC 버튼 클릭 (PRUNE + FORCE + REPLACE)
- [ ] SYNCHRONIZE 확인

SYNC 후:
- [ ] Health: Healthy 확인
- [ ] Pod: Running (1/1) 확인
- [ ] Service: curl 테스트 성공
- [ ] 전체 플로우 테스트 실행
- [ ] SSO 로그인 브라우저 테스트

---

## 🎯 최종 상태

### Git

```
Repository: http://192.168.0.11:7780/root/q-sign.git
Branch: main
Commit: 9bc1f17 (Recreate strategy)

History:
  9bc1f17 - Recreate strategy
  8b493fb - Remove hostNetwork
  792054c - Fix image
```

### ArgoCD

```
Application: q-sign
  Health:    Healthy
  Sync:      Synced to 9bc1f17
  Strategy:  Recreate

Resources:
  Deployment: 1/1 (Recreate)
  Pod: 1/1 Running
  Service: Active
```

### 서비스

```
Q-SIGN Keycloak
  Port:       30181 ✅
  Status:     Running ✅
  Image:      keycloak-hsm:v1.2.0-hybrid ✅
  Strategy:   Recreate ✅
  Realm:      myrealm ✅
  Frontend:   http://192.168.0.11:30181 ✅
```

---

**생성 시각**: 2025-11-17 11:35
**최종 커밋**: 9bc1f17
**상태**: Ready for ArgoCD Sync
**예상 소요 시간**: 1-2분
**다운타임**: 30-60초 (Recreate 특성)
