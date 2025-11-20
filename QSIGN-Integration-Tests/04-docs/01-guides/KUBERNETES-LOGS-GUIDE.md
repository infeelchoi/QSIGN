# Kubernetes 로그 확인 가이드

QSIGN 시스템의 Kubernetes Pod 로그를 확인하는 종합 가이드입니다.

## 📋 목차

- [기본 로그 확인](#기본-로그-확인)
- [Keycloak-PQC 로그](#keycloak-pqc-로그)
- [PostgreSQL 로그](#postgresql-로그)
- [APISIX Gateway 로그](#apisix-gateway-로그)
- [Q-APP 로그](#q-app-로그)
- [고급 로그 조회](#고급-로그-조회)
- [트러블슈팅](#트러블슈팅)

---

## 🔍 기본 로그 확인

### Pod 목록 확인

```bash
# 특정 namespace의 모든 Pod 확인
sudo k3s kubectl get pods -n q-sign

# 모든 namespace의 Pod 확인
sudo k3s kubectl get pods -A

# 특정 Pod 검색
sudo k3s kubectl get pods -A | grep keycloak
sudo k3s kubectl get pods -A | grep postgres
sudo k3s kubectl get pods -A | grep apisix
```

### 기본 로그 명령어

```bash
# 기본 형식
sudo k3s kubectl logs -n <namespace> <pod-name>

# 예제
sudo k3s kubectl logs -n q-sign keycloak-pqc
sudo k3s kubectl logs -n q-sign postgres-qsign
```

---

## 🔐 Keycloak-PQC 로그

### 기본 로그 확인

```bash
# 최신 50줄 확인
sudo k3s kubectl logs -n q-sign keycloak-pqc --tail=50

# 전체 로그 확인
sudo k3s kubectl logs -n q-sign keycloak-pqc

# 실시간 로그 스트리밍 (tail -f)
sudo k3s kubectl logs -n q-sign keycloak-pqc -f
```

### 시간 기반 조회

```bash
# 최근 5분간 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc --since=5m

# 최근 1시간 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc --since=1h

# 최근 1일 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc --since=24h
```

### 특정 컨테이너 로그

```bash
# 멀티 컨테이너 Pod인 경우
sudo k3s kubectl logs -n q-sign keycloak-pqc -c keycloak

# 이전 실행 Pod 로그 (Crash 후)
sudo k3s kubectl logs -n q-sign keycloak-pqc --previous
```

### Keycloak 에러 로그

```bash
# ERROR 레벨만 필터링
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep ERROR

# WARN + ERROR 필터링
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep -E "(ERROR|WARN)"

# Exception 검색
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep Exception

# PQC 관련 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep -i "DILITHIUM\|KYBER\|PQC"
```

### Keycloak 인증 로그

```bash
# 로그인 관련 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep -i "login\|authentication"

# 토큰 발급 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep -i "token"

# 사용자별 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep "testuser"
```

---

## 🗄️ PostgreSQL 로그

### 기본 로그 확인

```bash
# 최신 50줄
sudo k3s kubectl logs -n q-sign postgres-qsign --tail=50

# 실시간 로그
sudo k3s kubectl logs -n q-sign postgres-qsign -f

# 최근 10분 로그
sudo k3s kubectl logs -n q-sign postgres-qsign --since=10m
```

### PostgreSQL 에러 로그

```bash
# ERROR만 필터링
sudo k3s kubectl logs -n q-sign postgres-qsign | grep ERROR

# 연결 에러
sudo k3s kubectl logs -n q-sign postgres-qsign | grep -i "connection\|connect"

# 쿼리 에러
sudo k3s kubectl logs -n q-sign postgres-qsign | grep -i "query\|syntax"
```

### PostgreSQL 성능 로그

```bash
# Slow query 로그
sudo k3s kubectl logs -n q-sign postgres-qsign | grep -i "slow"

# 연결 수 관련 로그
sudo k3s kubectl logs -n q-sign postgres-qsign | grep -i "connection"
```

---

## 🌐 APISIX Gateway 로그

### APISIX 로그 확인

```bash
# Pod 이름 확인
sudo k3s kubectl get pods -n q-gateway

# APISIX 로그
sudo k3s kubectl logs -n q-gateway <apisix-pod-name> --tail=100

# 실시간 로그
sudo k3s kubectl logs -n q-gateway <apisix-pod-name> -f
```

### APISIX 라우팅 로그

```bash
# 라우트 관련 로그
sudo k3s kubectl logs -n q-gateway <apisix-pod-name> | grep -i "route"

# Upstream 에러
sudo k3s kubectl logs -n q-gateway <apisix-pod-name> | grep -i "upstream"

# 307 Redirect 로그
sudo k3s kubectl logs -n q-gateway <apisix-pod-name> | grep "307"
```

---

## 📱 Q-APP 로그

### App별 로그 확인

```bash
# App3 로그
sudo k3s kubectl logs -n q-app -l app=app3 --tail=50

# App4 로그
sudo k3s kubectl logs -n q-app -l app=app4 --tail=50

# App5 로그
sudo k3s kubectl logs -n q-app -l app=app5 --tail=50

# SSO Test App 로그
sudo k3s kubectl logs -n q-app -l app=sso-test-app --tail=50
```

### 실시간 디버깅

```bash
# App5 실시간 로그 + 에러 필터링
sudo k3s kubectl logs -n q-app -l app=app5 -f | grep -i error

# Angular 빌드 로그
sudo k3s kubectl logs -n q-app -l app=app5 | grep -i "webpack\|compile"

# HTTP 요청 로그
sudo k3s kubectl logs -n q-app -l app=app5 | grep -i "http\|request"
```

---

## 🔧 고급 로그 조회

### 여러 Pod 동시 조회

```bash
# Label selector로 여러 Pod 동시 조회
sudo k3s kubectl logs -n q-sign -l app=keycloak --tail=20

# 모든 q-app Pod 로그
sudo k3s kubectl logs -n q-app --all-containers=true --tail=50
```

### 로그 저장 및 분석

```bash
# 파일로 저장
sudo k3s kubectl logs -n q-sign keycloak-pqc > /tmp/keycloak-logs.txt

# 압축 저장
sudo k3s kubectl logs -n q-sign keycloak-pqc | gzip > /tmp/keycloak-logs.gz

# 타임스탬프와 함께 저장
sudo k3s kubectl logs -n q-sign keycloak-pqc --timestamps > /tmp/keycloak-logs-ts.txt
```

### 로그 필터링 조합

```bash
# 최근 100줄, ERROR만 필터링
sudo k3s kubectl logs -n q-sign keycloak-pqc --tail=100 | grep ERROR

# 실시간 로그 + 여러 패턴 검색
sudo k3s kubectl logs -n q-sign keycloak-pqc -f | grep -E "(ERROR|WARN|Exception|DILITHIUM)"

# 특정 시간대 + 특정 패턴
sudo k3s kubectl logs -n q-sign keycloak-pqc --since=1h | grep -i "authentication"
```

### 로그 통계

```bash
# ERROR 개수 세기
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep ERROR | wc -l

# 가장 많이 발생한 에러 Top 10
sudo k3s kubectl logs -n q-sign keycloak-pqc | grep ERROR | sort | uniq -c | sort -rn | head -10

# 시간대별 로그 개수
sudo k3s kubectl logs -n q-sign keycloak-pqc --timestamps | awk '{print $1}' | cut -d'T' -f1 | uniq -c
```

---

## 🐛 트러블슈팅

### Pod가 시작하지 않을 때

```bash
# Pod 상태 확인
sudo k3s kubectl get pods -n q-sign

# Pod 상세 정보 (Events 포함)
sudo k3s kubectl describe pod -n q-sign keycloak-pqc

# Init Container 로그 확인
sudo k3s kubectl logs -n q-sign keycloak-pqc -c init-container-name

# 이전 실패한 Pod 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc --previous
```

### CrashLoopBackOff 상태일 때

```bash
# 마지막 Crash 로그
sudo k3s kubectl logs -n q-sign keycloak-pqc --previous

# Pod 재시작 횟수 확인
sudo k3s kubectl get pod -n q-sign keycloak-pqc -o jsonpath='{.status.containerStatuses[0].restartCount}'

# Pod Event 확인
sudo k3s kubectl get events -n q-sign --field-selector involvedObject.name=keycloak-pqc
```

### 로그가 너무 많을 때

```bash
# 최신 로그만 조회 (메모리 절약)
sudo k3s kubectl logs -n q-sign keycloak-pqc --tail=100

# 로그 스트리밍 중단 (Ctrl+C)

# 로그 로테이션 확인
sudo k3s kubectl exec -n q-sign keycloak-pqc -- ls -lh /var/log/
```

### 로그에 아무것도 없을 때

```bash
# Pod가 실행 중인지 확인
sudo k3s kubectl get pod -n q-sign keycloak-pqc

# Pod 내부 들어가서 직접 확인
sudo k3s kubectl exec -n q-sign keycloak-pqc -it -- /bin/bash

# 표준 출력 리다이렉션 확인
sudo k3s kubectl exec -n q-sign keycloak-pqc -- cat /proc/1/fd/1
```

---

## 📊 유용한 조합 명령어

### 자주 사용하는 명령어

```bash
# 1. Keycloak 실시간 에러 모니터링
sudo k3s kubectl logs -n q-sign keycloak-pqc -f | grep -E "(ERROR|WARN|Exception)"

# 2. PostgreSQL 연결 에러 확인
sudo k3s kubectl logs -n q-sign postgres-qsign --tail=100 | grep -i "connection"

# 3. APISIX 라우팅 문제 디버깅
sudo k3s kubectl logs -n q-gateway <apisix-pod> -f | grep -E "(route|upstream|error)"

# 4. App5 Angular 빌드 진행 상황
sudo k3s kubectl logs -n q-app -l app=app5 -f | grep -i "compiled"

# 5. SSO 로그인 Flow 추적
sudo k3s kubectl logs -n q-sign keycloak-pqc -f | grep -i "login\|token\|redirect"
```

### 원라인 디버깅 스크립트

```bash
# Keycloak + PostgreSQL 동시 모니터링
watch -n 2 "sudo k3s kubectl get pods -n q-sign && echo '---' && sudo k3s kubectl logs -n q-sign keycloak-pqc --tail=5"

# 모든 에러 로그 수집
for pod in $(sudo k3s kubectl get pods -n q-sign -o name); do
  echo "=== $pod ===" >> /tmp/all-errors.log
  sudo k3s kubectl logs -n q-sign ${pod#pod/} | grep ERROR >> /tmp/all-errors.log
done
```

---

## 📝 로그 위치 정보

### Kubernetes 로그 저장 위치

```bash
# K3s 기본 로그 위치
/var/log/pods/<namespace>_<pod-name>_<pod-uid>/<container-name>/

# 예제
/var/log/pods/q-sign_keycloak-pqc_<uuid>/keycloak/0.log
/var/log/pods/q-sign_postgres-qsign_<uuid>/postgres/0.log
```

### 직접 로그 파일 접근

```bash
# 로그 파일 직접 조회
sudo ls -lh /var/log/pods/q-sign_*/

# 특정 Pod 로그 파일
sudo tail -f /var/log/pods/q-sign_keycloak-pqc_*/keycloak/0.log
```

---

## 🔗 관련 가이드

- [Q-SIGN-FIX-GUIDE.md](Q-SIGN-FIX-GUIDE.md) - Q-SIGN 종합 트러블슈팅
- [DELETE-ERROR-POD-GUIDE.md](DELETE-ERROR-POD-GUIDE.md) - 에러 Pod 삭제 가이드
- [Q-SIGN-ARGOCD-TROUBLESHOOT.md](../troubleshooting/Q-SIGN-ARGOCD-TROUBLESHOOT.md) - ArgoCD 트러블슈팅

---

## 💡 팁

### 로그 조회 성능 향상

- `--tail` 옵션을 사용하여 불필요한 로그 로딩 방지
- `grep`보다 `awk`가 대용량 로그에서 더 빠름
- 실시간 모니터링 시 필터링을 병행하여 트래픽 감소

### 로그 보관

```bash
# 일별 로그 백업
sudo k3s kubectl logs -n q-sign keycloak-pqc > /backup/keycloak-$(date +%Y%m%d).log

# 로그 로테이션 스크립트
#!/bin/bash
LOG_DIR="/backup/logs"
mkdir -p $LOG_DIR
sudo k3s kubectl logs -n q-sign keycloak-pqc > $LOG_DIR/keycloak-$(date +%Y%m%d-%H%M%S).log
find $LOG_DIR -name "keycloak-*.log" -mtime +7 -delete  # 7일 이상 로그 삭제
```

---

**버전**: 1.0.0
**마지막 업데이트**: 2025-11-19
**작성자**: QSIGN Team
