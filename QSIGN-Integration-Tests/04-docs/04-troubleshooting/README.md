# QSIGN 트러블슈팅 문서

QSIGN 시스템에서 발생 가능한 문제와 해결 방법을 문서화한 모음입니다.

## 🔍 트러블슈팅 문서 목록

### 1. Q-SIGN-ARGOCD-TROUBLESHOOT.md
**ArgoCD 관련 트러블슈팅**

- ArgoCD 동기화 문제
- Health 체크 실패
- Sync 오류 해결

**주요 이슈:**
- ❌ OutOfSync 상태
- ❌ Degraded Health
- ❌ Sync Failed

---

### 2. Q-SIGN-FINAL-FIX.md
**최종 수정 사항 문서**

- 최종 배포 전 수정 사항
- 크리티컬 버그 픽스
- 성능 개선 사항

**포함 내용:**
- 수정된 버그 목록
- 적용된 패치
- 검증 결과

---

### 3. Q-SIGN-PENDING-FIX.md
**Pending 상태 Pod 문제 해결**

- Pod가 Pending 상태로 멈춘 경우
- 리소스 부족 문제
- 스케줄링 실패 해결

**일반적인 원인:**
- 🔴 노드 리소스 부족 (CPU/Memory)
- 🔴 PVC 마운트 실패
- 🔴 이미지 풀 실패
- 🔴 노드 셀렉터 불일치

---

### 4. Q-SIGN-RESTORE-COMPLETE.md
**복구 완료 보고서**

- 시스템 복구 절차
- 복구 후 검증
- 복구 완료 체크리스트

**복구 시나리오:**
- 배포 실패 후 롤백
- 데이터 복구
- 서비스 재시작

---

## 📊 트러블슈팅 플로우

```
문제 발생
    ↓
1️⃣ 증상 확인
    ↓
2️⃣ 로그 수집
    ↓
3️⃣ 관련 문서 찾기
    ↓
4️⃣ 해결 방법 적용
    ↓
5️⃣ 검증
    ↓
6️⃣ 문서화
```

---

## 🛠️ 기본 진단 명령어

### Pod 상태 확인
```bash
sudo k3s kubectl get pods -n q-sign
sudo k3s kubectl describe pod <pod-name> -n q-sign
sudo k3s kubectl logs <pod-name> -n q-sign
```

### 서비스 상태 확인
```bash
sudo k3s kubectl get svc -n q-sign
sudo k3s kubectl describe svc <service-name> -n q-sign
```

### ArgoCD 상태 확인
```bash
argocd app get q-sign
argocd app sync q-sign
```

### 이벤트 확인
```bash
sudo k3s kubectl get events -n q-sign --sort-by='.lastTimestamp'
```

---

## 🚨 일반적인 문제와 빠른 해결

| 문제 | 빠른 해결 | 상세 문서 |
|------|----------|----------|
| Pod Pending | 리소스 확인 및 PVC 체크 | Q-SIGN-PENDING-FIX.md |
| OutOfSync | ArgoCD refresh & sync | Q-SIGN-ARGOCD-TROUBLESHOOT.md |
| CrashLoopBackOff | 로그 확인 및 설정 검증 | Q-SIGN-FINAL-FIX.md |
| ImagePullBackOff | 이미지 이름/태그 확인 | Q-SIGN-FIX-GUIDE.md |

---

## 📝 트러블슈팅 체크리스트

문제 발생 시 다음 순서로 확인:

- [ ] **1단계**: Pod 상태 확인 (`kubectl get pods`)
- [ ] **2단계**: 로그 확인 (`kubectl logs`)
- [ ] **3단계**: 이벤트 확인 (`kubectl get events`)
- [ ] **4단계**: 리소스 확인 (`kubectl top nodes/pods`)
- [ ] **5단계**: 설정 확인 (`kubectl describe`)
- [ ] **6단계**: 네트워크 확인 (`kubectl get svc, endpoints`)
- [ ] **7단계**: ArgoCD 상태 확인 (`argocd app get`)

---

## 💡 트러블슈팅 팁

### 로그 수집
```bash
# 모든 Pod 로그 수집
for pod in $(sudo k3s kubectl get pods -n q-sign -o name); do
    echo "=== $pod ===" >> qsign-logs.txt
    sudo k3s kubectl logs $pod -n q-sign >> qsign-logs.txt 2>&1
done
```

### 상태 스냅샷
```bash
# 전체 상태 저장
sudo k3s kubectl get all -n q-sign > qsign-state.txt
sudo k3s kubectl describe all -n q-sign > qsign-describe.txt
```

### 리소스 모니터링
```bash
# 실시간 리소스 확인
watch -n 2 'sudo k3s kubectl top nodes && echo && sudo k3s kubectl top pods -n q-sign'
```

---

## 🔗 관련 리소스

- [가이드 문서](../guides/)
- [테스트 스크립트](../../scripts/tests/)
- [테스트 결과](../results/)

---

## 📞 에스컬레이션

다음 경우 팀에 에스컬레이션:

1. 30분 이상 문제가 해결되지 않을 때
2. 데이터 손실 위험이 있을 때
3. 프로덕션 서비스가 중단되었을 때
4. 보안 이슈가 의심될 때

---

**업데이트**: 2025-11-17
