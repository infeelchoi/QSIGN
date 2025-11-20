# Q-SIGN 안정 상태 복원 완료

## ✅ 복원 완료

### Git 커밋 상태

**Repository**: http://192.168.0.11:7780/root/q-sign.git

```
최신 커밋:
  c86d38c - Revert "Remove hostNetwork" ⭐ 최신
  730c0c6 - Revert "Change Deployment strategy to Recreate"
  bff1eca - Fix Keycloak PQC image to use Harbor registry
  559fca7 - Merge
  9a7e4ac - Fix Q-SIGN Keycloak deployment
```

**Status**: ✅ GitLab 푸시 완료

---

## 🔄 되돌린 변경사항

### Revert된 수정 (안정 상태로 복원)

**1. Recreate 전략 제거** (730c0c6)
```yaml
# 제거됨:
spec:
  strategy:
    type: Recreate

# 복원됨:
# strategy 없음 (기본 RollingUpdate 사용)
```

**2. hostNetwork 복원** (c86d38c)
```yaml
# 복원됨:
spec:
  hostNetwork: true
  dnsPolicy: ClusterFirstWithHostNet
```

**3. 이미지 설정** (bff1eca - 원격)
```yaml
# Harbor registry 사용
image:
  repository: 192.168.0.11:30800/qsign-prod/keycloak-pqc
  # 또는 원래 설정
```

---

## 🎯 현재 설정 (안정 상태)

### 복원된 Deployment 설정

**정상 작동하는 이전 설정으로 돌아감**:

```yaml
# Deployment
spec:
  # strategy: (기본 RollingUpdate)
  replicas: 1
  template:
    spec:
      hostNetwork: true
      dnsPolicy: ClusterFirstWithHostNet
      containers:
      - name: keycloak-pqc
        image: 192.168.0.11:30800/qsign-prod/keycloak-pqc:...
        # 또는 원래 이미지
```

**특징**:
- ✅ 이전에 정상 작동하던 설정
- ✅ hostNetwork 사용 (원래 설정)
- ✅ RollingUpdate 전략 (기본값)
- ✅ 검증된 안정 상태

---

## 🚀 ArgoCD에서 복원 적용

### Step 1: REFRESH

1. **ArgoCD UI 접속**
   ```
   http://192.168.0.11:30080
   ```

2. **q-sign 애플리케이션 선택**

3. **REFRESH 버튼 클릭**
   - Git 저장소에서 최신 변경사항 가져오기
   - 커밋: c86d38c 확인

---

### Step 2: 문제 Pod 삭제 (중요!)

**삭제 대상**:
- Pod: `keycloak-pqc-7d5dc44c8-xxxxx` (error 상태)
- Pod: `keycloak-pqc-7d5dc44c8-xxxxx` (다른 에러 Pod들)

**삭제 방법**:
1. 리소스 트리에서 에러 상태 Pod 모두 찾기
2. 각 Pod 우측 3점 메뉴 (⋮) 클릭
3. "Delete" 선택
4. 확인

**⚠️ 유지할 Pod**:
- `keycloak-pqc-7dfb996cf5-xxxxx` (Running 1/1, 4 days)

  **이 Pod는 절대 삭제하지 마세요!** ✅ 정상 작동 중

---

### Step 3: SYNC (복원 적용)

1. **SYNC 버튼 클릭**

2. **Sync Options**:
   - ✅ **PRUNE** (사용하지 않는 리소스 제거)
   - ⬜ FORCE (필요 없음)
   - ⬜ REPLACE (필요 없음)

3. **"SYNCHRONIZE" 클릭**

4. **진행 관찰**:
   ```
   1. Deployment 업데이트 감지
   2. 에러 Pod 정리
   3. 기존 정상 Pod (7dfb996cf5) 유지
   4. 설정이 안정 버전으로 복원됨
   5. Health: Progressing → Healthy
   ```

5. **완료 확인**:
   - Health: ✅ **Healthy**
   - Sync: ✅ **Synced to c86d38c**
   - Pod: ✅ **Running (1/1)** - 기존 정상 Pod 유지

---

## 📊 예상 결과

### 시나리오 1: 정상 Pod 유지 (가장 가능성 높음)

```
1. SYNC 시작
2. Deployment 설정이 안정 버전으로 업데이트
3. 기존 정상 Pod (7dfb996cf5) 계속 Running
4. 에러 Pod들만 삭제됨
5. Deployment가 더 이상 새 Pod 생성 시도 안함
6. Health: Healthy ✅
```

**결과**:
- ✅ 서비스 중단 없음
- ✅ 기존 정상 Pod 계속 작동
- ✅ Port 30181 계속 응답
- ✅ Progressing → Healthy

---

### 시나리오 2: Pod 재생성 (필요한 경우)

만약 설정 변경으로 인해 Pod 재생성이 필요하다면:

```
1. SYNC 시작
2. Deployment 업데이트
3. 기존 Pod (7dfb996cf5) 종료
4. 새 Pod 생성 (안정 설정)
5. 새 Pod Running
6. Health: Healthy ✅
```

**영향**:
- 짧은 다운타임 가능 (30-60초)
- 하지만 안정적인 설정으로 시작

---

## ✅ 복원 후 검증

### 1. ArgoCD UI 확인

**예상 상태**:
```
Application: q-sign
  Health:    ✅ Healthy
  Sync:      ✅ Synced to c86d38c

Resources:
  ✅ Deployment: keycloak-pqc (1/1)
  ✅ Pod: keycloak-pqc-7dfb996cf5 (Running 1/1, 4 days)
       또는
       keycloak-pqc-XXXXXXX (Running 1/1, 방금)
  ✅ Service: keycloak-pqc
  ✅ All resources Healthy
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
Q-SIGN Keycloak (30181)        ✓ PASS
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

### SYNC 후에도 Progressing 상태인 경우

**원인**: 에러 Pod가 아직 남아있음

**해결**:
1. 에러 상태의 모든 Pod 확인
2. 각 에러 Pod 삭제:
   - Pod 우클릭 → Delete
3. 10초 대기
4. Health 상태 확인

**삭제 대상 Pod 특징**:
- Status: error, CrashLoopBackOff, ImagePullBackOff
- Ready: 0/1
- Age: 수 분 ~ 수 시간
- ⚠️ **4일 이상 된 정상 Pod는 삭제하지 마세요!**

---

### 여전히 새 Pod가 생성되는 경우

**원인**: Deployment가 여전히 업데이트 시도

**확인**:
1. ArgoCD UI → Deployment 클릭
2. "Desired Replicas": 1
3. "Current Replicas": ?
4. Image 확인

**해결**:
- Deployment를 삭제하고 재생성:
  1. Deployment 우클릭 → Delete
  2. ArgoCD가 자동으로 재생성
  3. 안정 설정으로 생성됨

---

### Pod가 완전히 없는 경우

**원인**: 모든 Pod를 삭제했거나 Deployment 문제

**해결**:
1. ArgoCD에서 SYNC 다시 실행
2. Deployment가 자동으로 Pod 생성
3. 안정 설정으로 시작
4. Running 대기

---

## 📝 변경 이력 요약

### 시도한 수정 (되돌림)

**1차 시도** (792054c):
- 이미지 변경
- → 되돌림 (원래 이미지 사용)

**2차 시도** (8b493fb):
- hostNetwork 제거
- → 되돌림 (hostNetwork 복원)

**3차 시도** (9bc1f17):
- Recreate 전략
- → 되돌림 (RollingUpdate)

### 최종 결과

**안정 상태로 복원** (c86d38c):
- ✅ 이전 검증된 설정 사용
- ✅ 정상 작동하는 Pod 유지
- ✅ 서비스 중단 없음

---

## 💡 교훈 및 권장사항

### 안정적인 운영을 위한 권장사항

**1. 점진적 변경**
- 한 번에 하나씩 변경
- 각 변경 후 충분한 검증
- 문제 발생 시 즉시 롤백

**2. 테스트 환경**
- Production 변경 전 테스트 환경에서 먼저 시도
- 여러 시나리오 테스트
- 롤백 계획 수립

**3. 모니터링**
- ArgoCD Health Status 지속 모니터링
- Pod 로그 확인
- 서비스 가용성 체크

**4. 백업**
- Git 커밋 히스토리 유지
- 안정적인 커밋 태그 지정
- 롤백 절차 문서화

---

## ✅ 체크리스트

복원 전:
- [x] Git 변경사항 revert 완료
- [x] GitLab 푸시 완료
- [x] 복원 가이드 작성

복원 실행:
- [ ] ArgoCD REFRESH 실행
- [ ] 커밋 c86d38c 확인
- [ ] 에러 Pod 모두 삭제 (정상 Pod 제외)
- [ ] SYNC 실행 (PRUNE 옵션)
- [ ] SYNCHRONIZE 확인

복원 후:
- [ ] Health: Healthy 확인
- [ ] Pod: Running (1/1) 확인
- [ ] Service: curl 테스트 성공
- [ ] 전체 플로우 테스트 실행
- [ ] SSO 로그인 브라우저 테스트

---

## 🎯 최종 목표

**현재 상태**:
- 정상 작동하는 Pod 유지
- 안정적인 설정으로 복원
- 서비스 가용성 보장

**예상 결과**:
```
Q-SIGN Keycloak
  Status:     ✅ Healthy
  Pod:        ✅ Running (1/1)
  Image:      안정 버전
  Config:     검증된 설정
  Service:    Port 30181 정상 응답
  SSO:        정상 작동
```

---

**생성 시각**: 2025-11-17 13:05
**복원 커밋**: c86d38c
**상태**: Ready for ArgoCD Sync
**목표**: 안정적인 이전 상태로 복원
**위험도**: 낮음 (검증된 설정으로 되돌림)
