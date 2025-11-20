# Q-SIGN 에러 Pod 삭제 가이드

## 📊 현재 상태 (ArgoCD UI 분석)

### Application 상태
- **Health**: 🔄 Progressing
- **Sync**: ✅ Synced to main (8b493fb) - 최신 커밋!
- **Last Sync**: 2분 전 성공

### Pod 상태

**문제 Pod (삭제 필요)** ❌:
```
keycloak-pqc-7d5dc44c8-xxxxx
  Status: error
  Ready: 0/1
  Age: 8 minutes
  Restarts: 1
  Exit Code: 500
```

**정상 Pod (유지)** ✅:
```
keycloak-pqc-7dfb996cf5-xxxxx
  Status: running
  Ready: 1/1
  Age: 4 days
```

### 분석

**문제**:
- 새로운 Pod (7d5dc44c8)가 생성되었지만 시작 실패
- 이전 Pod (7dfb996cf5)는 여전히 Running 상태
- Deployment가 롤링 업데이트 중 실패

**원인**:
- 새 Pod가 에러로 시작하지 못함 (Exit 500)
- 기존 Pod는 정상 작동 중
- Deployment의 maxSurge/maxUnavailable 설정으로 인해 두 Pod 모두 존재

---

## 🗑️ ArgoCD UI를 통한 Pod 삭제 방법

### 방법 1: 리소스 트리에서 삭제 (가장 쉬움) ⭐

1. **ArgoCD UI에서 q-sign 애플리케이션 화면 유지**
   - 현재 화면에서 계속 진행

2. **에러 Pod 찾기**
   - 리소스 트리에서 `keycloak-pqc-7d5dc44c8` Pod 찾기
   - 빨간색 하트 아이콘과 "error" 표시 확인

3. **Pod 삭제**
   - Pod 위에 마우스 올리기
   - 우측의 **3점 메뉴 (⋮)** 클릭
   - **"Delete"** 선택

4. **삭제 확인**
   - 확인 대화상자에서 **"OK"** 또는 **"DELETE"** 클릭
   - Pod 이름 확인: `keycloak-pqc-7d5dc44c8`

5. **결과 확인**
   - Pod가 리소스 트리에서 사라짐
   - Deployment가 자동으로 새 Pod 생성 시작
   - 또는 기존 정상 Pod (7dfb996cf5)만 유지

---

### 방법 2: Pod 상세 화면에서 삭제

1. **Pod 클릭**
   - 에러 상태의 `keycloak-pqc-7d5dc44c8` Pod 클릭

2. **상세 정보 확인**
   - Pod 상세 화면 열림
   - 상태, 로그, 이벤트 확인 가능

3. **삭제 버튼 클릭**
   - 상단 툴바에서 **"DELETE"** 버튼 찾기
   - 클릭

4. **확인 및 완료**
   - 삭제 확인
   - 리소스 트리 화면으로 돌아가기

---

### 방법 3: YAML 편집기에서 삭제

1. **Pod 선택**
   - `keycloak-pqc-7d5dc44c8` Pod 클릭

2. **EDIT 버튼 클릭**
   - 상단 또는 우측 상단의 "EDIT" 버튼

3. **메타데이터에서 finalizers 제거** (필요시)
   - YAML 에디터에서 `metadata.finalizers` 찾기
   - 있다면 제거
   - "SAVE" 클릭

4. **DELETE 버튼 클릭**
   - 다시 상단의 "DELETE" 버튼
   - 확인

---

## 🔧 삭제 후 예상 동작

### 시나리오 1: 새 Pod 자동 생성 (Deployment replicas=1)

```
1. 에러 Pod 삭제
   keycloak-pqc-7d5dc44c8 → Terminating → 삭제됨

2. 정상 Pod 유지
   keycloak-pqc-7dfb996cf5 → Running (1/1)

3. Deployment 확인
   - Desired: 1
   - Current: 1 (7dfb996cf5)
   - Ready: 1
```

**결과**: 기존 정상 Pod가 계속 서비스 제공 ✅

---

### 시나리오 2: 롤링 업데이트 재시도

```
1. 에러 Pod 삭제

2. Deployment가 다시 새 Pod 생성 시도
   keycloak-pqc-XXXXXXX → Pending → Running

3. 새 Pod 정상 시작되면
   - 기존 Pod (7dfb996cf5) 종료
   - 새 Pod로 교체 완료
```

**결과**: 최신 커밋(8b493fb) 설정으로 Pod 업데이트 ✅

---

### 시나리오 3: 새 Pod도 실패 (계속 에러)

```
1. 에러 Pod 삭제

2. 새 Pod 생성 시도

3. 다시 에러 발생
   - Exit Code 500
   - CrashLoopBackOff 또는 Error

→ 이 경우 로그 확인 필요
```

---

## 📋 삭제 전 확인사항

### 삭제해도 되는 Pod 확인

**삭제 대상** ❌:
- Pod 이름: `keycloak-pqc-7d5dc44c8-xxxxx`
- 상태: error
- Ready: 0/1
- Age: 수 분 (최근 생성)

**유지할 Pod** ✅:
- Pod 이름: `keycloak-pqc-7dfb996cf5-xxxxx`
- 상태: running
- Ready: 1/1
- Age: 4 days (안정적)

### 서비스 영향

**영향 없음** ✅:
- 정상 Pod (7dfb996cf5)가 계속 서비스 제공
- Port 30181 계속 응답
- Q-APP 연결 유지

---

## 🔍 삭제 후 확인

### 1. ArgoCD UI 확인

**예상 상태**:
```
Health: Healthy (Progressing → Healthy)
Pods:
  ✅ keycloak-pqc-7dfb996cf5 (Running 1/1)
  또는
  ✅ keycloak-pqc-XXXXXXX (Running 1/1) - 새로 생성됨
```

### 2. 서비스 테스트

```bash
curl -s http://192.168.0.11:30181/realms/myrealm | python3 -c "import sys,json; print(json.load(sys.stdin).get('realm'))"
```

**예상 출력**: `myrealm` ✅

### 3. 전체 플로우 테스트

```bash
/home/user/QSIGN/test-full-qsign-flow.sh
```

**예상 결과**: 모든 컴포넌트 PASS ✅

---

## 🚨 새 Pod도 계속 실패하는 경우

### Pod 로그 확인

ArgoCD UI에서:
1. 새로 생성된 Pod 클릭
2. **"LOGS"** 탭 클릭
3. 에러 메시지 확인

### 일반적인 에러 원인

**1. PostgreSQL 연결 실패**
```
Error: could not connect to database
FATAL: password authentication failed
```
**해결**: postgres-qsign Pod 및 Service 확인

**2. 이미지 Pull 실패**
```
Failed to pull image "localhost:7800/qsign-prod/keycloak-hsm:v1.2.0-hybrid"
```
**해결**: 이미지 레지스트리 확인

**3. Health Check 실패**
```
Liveness probe failed
Readiness probe failed
```
**해결**: startupProbe 타임아웃 증가 필요

**4. 리소스 부족**
```
0/1 nodes available: insufficient memory/cpu
```
**해결**: 리소스 요청량 감소 또는 노드 확장

### 로그 확인 후 추가 조치

**PostgreSQL 문제**:
```bash
# postgres-qsign Pod 확인
sudo k3s kubectl get pod -n q-sign -l app=postgres-qsign
sudo k3s kubectl logs -n q-sign -l app=postgres-qsign
```

**이미지 문제**:
```bash
# 이미지 존재 확인
curl -s http://localhost:7800/v2/qsign-prod/keycloak-hsm/tags/list
```

**설정 확인**:
- values.yaml 재검토
- 환경변수 확인
- Secret 확인

---

## ⚡ 빠른 해결 (추천)

### 가장 빠른 방법

1. **ArgoCD UI 현재 화면에서**:
   - `keycloak-pqc-7d5dc44c8` Pod 찾기
   - 3점 메뉴 (⋮) 클릭
   - Delete 클릭
   - OK 클릭

2. **10초 대기**

3. **결과 확인**:
   - Health: Healthy ✅
   - 기존 Pod (7dfb996cf5) 계속 Running
   - 또는 새 Pod 정상 시작

---

## 📊 예상 최종 상태

### ArgoCD

```
Application: q-sign
  Health:    ✅ Healthy
  Sync:      ✅ Synced to 8b493fb

Resources:
  ✅ Deployment: keycloak-pqc (1/1)
  ✅ Pod: keycloak-pqc-xxxxxxx (Running 1/1)
  ✅ Pod: postgres-qsign-xxxxxxx (Running 1/1)
  ✅ Service: keycloak-pqc
  ✅ All resources Healthy
```

### 서비스

```
Q-SIGN Keycloak:
  Port:     30181 ✅
  Status:   Running ✅
  Realm:    myrealm ✅
  Ready:    1/1 ✅
```

---

**생성 시각**: 2025-11-17 11:27
**목적**: 에러 Pod 안전 제거
**예상 시간**: 30초
**위험도**: 낮음 (정상 Pod가 계속 서비스 제공)
