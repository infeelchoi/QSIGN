# Keycloak 배포 실패 원인 분석

**작성일**: 2025-11-19
**문제**: Keycloak YAML 수정 시 새 Pod가 Pending 상태로 배포 실패

---

## 🔍 근본 원인

### hostNetwork: true 설정의 문제

**파일**: `/home/user/QSIGN/Q-SIGN/helm/q-sign/templates/keycloak.yaml`
**라인**: 205

```yaml
spec:
  hostNetwork: true           # ← 문제의 원인
  dnsPolicy: ClusterFirstWithHostNet
```

---

## ❌ 왜 배포가 실패하는가?

### hostNetwork: true의 동작 방식

```
┌─────────────────────────────────────┐
│ Kubernetes Node (192.168.0.11)     │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ keycloak-pqc-748dcf4fbd       │ │
│  │ (기존 Pod)                    │ │
│  │ Port 8080 사용 중 ✅          │ │
│  └───────────────────────────────┘ │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ keycloak-pqc-bbccc99f8        │ │
│  │ (새 Pod)                      │ │
│  │ Port 8080 사용 시도 ❌        │ │
│  │ → Pending (포트 충돌)         │ │
│  └───────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

### Rolling Update 시나리오

1. **기존 Pod**: keycloak-pqc-748dcf4fbd-nnbh6
   - 상태: Running
   - 호스트 포트 8080 점유 중

2. **새 Pod 생성 시도**: keycloak-pqc-bbccc99f8-dhmxh
   - Kubernetes가 새 Pod 생성 시도
   - hostNetwork: true이므로 호스트 포트 8080 필요
   - **포트 충돌 발생!**
   - Pod가 Pending 상태로 멈춤

3. **결과**:
   - 새 Pod: Pending (스케줄링 불가)
   - 기존 Pod: Running (계속 실행)
   - Deployment: Progressing (무한 대기)

---

## 📊 Rolling Update 전략의 문제

### 기본 Rolling Update 설정

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # 동시에 1개 초과 Pod 허용
    maxUnavailable: 0  # 0개만 중단 허용
```

**동작 순서**:
1. 새 Pod 먼저 생성 (maxSurge: 1)
2. 새 Pod가 Ready 될 때까지 대기
3. 기존 Pod 종료
4. 완료

**hostNetwork: true 환경에서**:
1. 새 Pod 생성 시도 ✅
2. **포트 충돌로 Pending** ❌
3. 기존 Pod는 계속 실행 (종료 안 됨)
4. **무한 대기** 🔄

---

## 🔧 해결 방안

### 옵션 1: hostNetwork 제거 (권장) ✅

**장점**:
- Rolling Update 정상 작동
- 포트 충돌 없음
- Kubernetes Service를 통한 안정적인 통신
- 표준 Kubernetes 네트워킹

**단점**:
- 없음 (표준 방식)

**수정 방법**:
```yaml
# 변경 전
spec:
  hostNetwork: true
  dnsPolicy: ClusterFirstWithHostNet

# 변경 후
spec:
  # hostNetwork 라인 제거
  # dnsPolicy는 기본값 (ClusterFirst) 사용
```

---

### 옵션 2: Recreate 전략 사용

**장점**:
- hostNetwork: true 유지 가능
- 포트 충돌 없음

**단점**:
- 다운타임 발생 (기존 Pod 먼저 종료)
- 무중단 배포 불가

**수정 방법**:
```yaml
spec:
  replicas: 1
  strategy:
    type: Recreate  # Rolling Update 대신 Recreate
```

**동작 순서**:
1. 기존 Pod 먼저 종료 (포트 해제)
2. 새 Pod 생성
3. 새 Pod 실행

**다운타임**: 약 2-3분 (Keycloak 시작 시간)

---

### 옵션 3: 수동 롤아웃

**수동으로 기존 Pod 삭제 후 배포**:

```bash
# 1. 기존 Pod 삭제
kubectl delete pod keycloak-pqc-748dcf4fbd-nnbh6 -n q-sign

# 2. 새 Pod 자동 생성됨 (포트 충돌 없음)
```

**장점**:
- 즉시 적용 가능

**단점**:
- 수동 작업 필요
- 다운타임 발생
- 자동화 불가

---

## 🎯 권장 사항

### **옵션 1 권장**: hostNetwork 제거

**이유**:
1. Rolling Update 정상 작동
2. 무중단 배포 가능
3. Kubernetes 표준 방식
4. NodePort Service로 외부 접근 가능

### hostNetwork를 사용하는 이유?

일반적으로 `hostNetwork: true`는 다음 경우에만 사용:
- LoadBalancer가 없는 환경에서 특정 포트 바인딩 필요
- 성능 최적화 (네트워크 오버헤드 제거)
- 레거시 시스템 호환성

**하지만 Keycloak은**:
- NodePort Service (30181) 사용 중
- hostNetwork 불필요
- 제거해도 문제 없음

---

## 📝 수정 예시

### 현재 설정 (문제)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak-pqc
spec:
  replicas: 1
  template:
    spec:
      hostNetwork: true                      # ← 제거 필요
      dnsPolicy: ClusterFirstWithHostNet     # ← 제거 필요
      containers:
      - name: keycloak-pqc
        ports:
        - containerPort: 8080
```

### 권장 설정 (수정)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak-pqc
spec:
  replicas: 1
  template:
    spec:
      # hostNetwork 제거
      containers:
      - name: keycloak-pqc
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak-pqc
spec:
  type: NodePort
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30181     # 외부 접근 포트
  selector:
    app: keycloak-pqc
```

---

## 🧪 검증 방법

### hostNetwork 제거 후 테스트

```bash
# 1. YAML 수정
# hostNetwork: true 라인 삭제

# 2. Git Commit & Push
git add helm/q-sign/templates/keycloak.yaml
git commit -m "Remove hostNetwork from Keycloak"
git push

# 3. ArgoCD Sync
argocd app sync q-sign

# 4. Rolling Update 확인
kubectl get pods -n q-sign -l app=keycloak-pqc -w

# 예상 출력:
# keycloak-pqc-748dcf4fbd-nnbh6   1/1   Running       0   39h
# keycloak-pqc-xxxxx-xxxxx        0/1   Pending       0   0s  ← 새 Pod 생성
# keycloak-pqc-xxxxx-xxxxx        0/1   ContainerCreating  0   1s
# keycloak-pqc-xxxxx-xxxxx        1/1   Running       0   2m  ← 새 Pod 실행
# keycloak-pqc-748dcf4fbd-nnbh6   1/1   Terminating   0   39h ← 기존 Pod 종료
```

---

## 📊 비교 표

| 항목 | hostNetwork: true | hostNetwork 제거 |
|------|------------------|-----------------|
| Rolling Update | ❌ 실패 (포트 충돌) | ✅ 정상 작동 |
| 무중단 배포 | ❌ 불가능 | ✅ 가능 |
| 외부 접근 | NodePort 필요 | NodePort 사용 |
| 포트 충돌 | ✅ 발생 | ❌ 없음 |
| Kubernetes 표준 | ❌ 비표준 | ✅ 표준 |
| DNS 해석 | ClusterFirstWithHostNet | ClusterFirst (기본) |
| 복잡도 | 높음 | 낮음 |

---

## 🚨 현재 상황 요약

### 문제
- Keycloak YAML 수정 시 새 Pod가 Pending
- Rolling Update 실패
- 기존 Pod만 계속 실행

### 원인
- `hostNetwork: true` 설정
- 포트 8080 충돌
- 새 Pod 스케줄링 불가

### 해결책
1. **즉시 해결**: 옵션 3 (수동 Pod 삭제)
2. **장기 해결**: 옵션 1 (hostNetwork 제거)

---

**결론**: `hostNetwork: true`를 제거하면 Keycloak 수정 시 정상적으로 Rolling Update가 작동합니다.
