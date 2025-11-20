# ReplicaSet 정리 가이드

## 🔍 ReplicaSet이란?

Kubernetes Deployment가 Pod를 관리하기 위해 생성하는 중간 리소스입니다. Deployment를 업데이트할 때마다 새로운 ReplicaSet이 생성되고, 이전 ReplicaSet은 `replicas=0`으로 유지됩니다 (롤백을 위해).

---

## 🗑️ 정리가 필요한 이유

**문제점**:
- app3, app4, app6, app7 업데이트할 때마다 ReplicaSet 누적
- 특히 `rollout-timestamp` annotation 사용 시 매번 새 ReplicaSet 생성
- 리소스 낭비는 없지만 kubectl get rs 출력이 지저분해짐

**정리 대상**:
- `replicas=0`인 오래된 ReplicaSet
- 현재 사용 중이 아닌 ReplicaSet

---

## 🚀 실행 방법

### 방법 1: 준비된 스크립트 사용 (권장)

```bash
cd /home/user/QSIGN
./cleanup-replicasets.sh
```

**스크립트 동작**:
1. q-app namespace의 모든 ReplicaSet 조회
2. `replicas=0`인 ReplicaSet 목록 표시
3. 삭제 확인 후 일괄 삭제
4. 최종 상태 확인

### 방법 2: 수동 확인 및 삭제

```bash
# 1. ReplicaSet 목록 확인
sudo k3s kubectl get rs -n q-app

# 출력 예시:
# NAME              DESIRED   CURRENT   READY   AGE
# app3-abc123       1         1         1       5m    ← 현재 사용 중
# app3-def456       0         0         0       10m   ← 오래된 버전 (삭제 대상)
# app3-ghi789       0         0         0       20m   ← 오래된 버전 (삭제 대상)
# app4-jkl012       1         1         1       3m    ← 현재 사용 중
# app4-mno345       0         0         0       15m   ← 오래된 버전 (삭제 대상)

# 2. replicas=0인 ReplicaSet만 필터링
sudo k3s kubectl get rs -n q-app -o json | \
  jq -r '.items[] | select(.spec.replicas==0) | .metadata.name'

# 3. 개별 삭제
sudo k3s kubectl delete rs -n q-app app3-def456
sudo k3s kubectl delete rs -n q-app app3-ghi789

# 4. 또는 일괄 삭제 (replicas=0인 모든 ReplicaSet)
sudo k3s kubectl get rs -n q-app -o json | \
  jq -r '.items[] | select(.spec.replicas==0) | .metadata.name' | \
  xargs -I {} sudo k3s kubectl delete rs -n q-app {}
```

### 방법 3: Kubernetes 자동 정리 설정 (영구 해결)

Deployment의 `revisionHistoryLimit` 설정:

```yaml
# deployment.yaml
spec:
  revisionHistoryLimit: 3  # 최근 3개의 ReplicaSet만 유지 (기본값: 10)
```

**values.yaml에 추가** (선택사항):
```yaml
global:
  revisionHistoryLimit: 3
```

---

## ⚠️ 주의사항

### 삭제하면 안 되는 ReplicaSet

**DESIRED 값이 1 이상인 것들**:
```
app3-abc123       1         1         1       5m    ← 현재 사용 중! 삭제 금지
```

### 안전하게 삭제 가능한 ReplicaSet

**DESIRED 값이 0인 것들**:
```
app3-def456       0         0         0       10m   ← 오래된 버전, 삭제 가능
```

### 롤백 고려사항

- ReplicaSet을 삭제하면 해당 버전으로 롤백 불가
- 최근 1-2개의 ReplicaSet은 롤백을 위해 유지 권장
- 오래된 ReplicaSet (7일 이상)만 삭제 권장

---

## 📊 예상 정리 대상

현재 q-app namespace에는 다음과 같은 업데이트가 있었습니다:

1. **app3**:
   - Direct Flow 복귀 (0cf232b)
   - Gateway Flow 시도 (1f62241)
   - 환경 변수 수정 등

2. **app4**:
   - 환경 변수 수정 (4d27478)
   - rollout-timestamp annotation 추가

3. **app6, app7**:
   - 로그아웃 URL 수정
   - rollout-timestamp annotation 추가

**예상 ReplicaSet 수**: 15-20개 (앱당 3-5개)

---

## 🧪 테스트

### 삭제 전 현재 상태 백업

```bash
# ReplicaSet 목록 저장
sudo k3s kubectl get rs -n q-app -o yaml > /tmp/replicasets-backup.yaml

# 또는 JSON 형식
sudo k3s kubectl get rs -n q-app -o json > /tmp/replicasets-backup.json
```

### 삭제 후 앱 동작 확인

```bash
# Pod 상태 확인
sudo k3s kubectl get pods -n q-app

# app3 health check
curl -s http://192.168.0.11:30202/health

# app4 health check
curl -s http://192.168.0.11:30203/health
```

---

## 🔄 자동화 (선택사항)

### Cron Job으로 정기 정리

```bash
# /etc/cron.d/cleanup-replicasets
# 매주 일요일 02:00에 오래된 ReplicaSet 정리
0 2 * * 0 root /home/user/QSIGN/cleanup-replicasets.sh -y > /var/log/replicaset-cleanup.log 2>&1
```

---

## 📋 실행 명령어 요약

```bash
# 빠른 실행
cd /home/user/QSIGN
./cleanup-replicasets.sh

# 또는 한 줄로
sudo k3s kubectl get rs -n q-app -o json | \
  jq -r '.items[] | select(.spec.replicas==0) | .metadata.name' | \
  xargs -I {} sudo k3s kubectl delete rs -n q-app {}
```

---

**다음 단계**: 위의 스크립트를 실행하여 오래된 ReplicaSet을 정리하세요! 🚀
