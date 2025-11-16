# 🌐 서비스 접속 정보

## 📅 배포 일시
**2025-11-12 13:50 KST**

## 🖥️ 서버 정보
- **서버 IP**: 192.168.0.11
- **Kubernetes**: K3s
- **환경**: Development/Test

---

## 🎯 통합 Dashboard

### CI/CD & SSO Dashboard
**가장 먼저 여기에 접속하세요!**

- **외부 접속 (NodePort)**: http://192.168.0.11:30090
- **로컬 접속**: http://localhost:30090
- **Namespace**: dashboard
- **설명**: 모든 서비스의 접속 정보와 명령어가 포함된 통합 대시보드

```bash
# 브라우저에서 접속
http://192.168.0.11:30090
```

---

## 🔄 Argo CD

### GitOps 배포 플랫폼

**외부 접속 (NodePort):**
- **HTTP**: http://192.168.0.11:30080
- **HTTPS**: https://192.168.0.11:30443

**로컬 Port Forward:**
```bash
export KUBECONFIG=/home/user/.kube/config
kubectl port-forward -n argocd svc/argocd-server 8443:443
```
- **접속 URL**: https://localhost:8443

**로그인 정보:**
- **Username**: `admin`
- **Password**: `jOxvYsjXKjwdbWZC`

**비밀번호 확인 명령어:**
```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
```

**등록된 Application:**
- `pqc-sso-stack`: SSO Test App + Keycloak + Luna HSM 통합 스택

---

## 🔐 Keycloak (SSO)

### Identity & Access Management

**로컬 Port Forward (현재 실행 중):**
```bash
kubectl port-forward -n pqc-sso svc/keycloak 8080:80
```
- **접속 URL**: http://localhost:8080

**로그인 정보:**
- **Admin Console**: http://localhost:8080
- **Username**: `admin`
- **Password**: `admin123!@#`

**주요 기능:**
- OIDC/SAML2.0 지원
- Luna HSM 연동 설정됨
- PostgreSQL 데이터베이스 연결
- Realm: `master`

---

## 🌐 SSO Test App

### OIDC 클라이언트 테스트 애플리케이션

**로컬 Port Forward (현재 실행 중):**
```bash
kubectl port-forward -n pqc-sso svc/sso-test-app 3000:80
```
- **접속 URL**: http://localhost:3000

**설정 정보:**
- **Keycloak URL**: http://keycloak:80 (내부)
- **Realm**: master
- **Client ID**: sso-test-app
- **Status**: Keycloak 연동 완료

---

## 🔒 Luna HSM Simulator

### Hardware Security Module Simulator

**로컬 Port Forward (현재 실행 중):**
```bash
kubectl port-forward -n pqc-sso svc/luna-hsm-simulator 8090:8090
```
- **접속 URL**: http://localhost:8090

**포트:**
- **8090**: HSM API
- **1792**: Luna Client

**지원 알고리즘:**
- ML-DSA-65, ML-DSA-87
- ECDSA-P384
- RSA-2048

---

## 🦊 GitLab (외부)

### Git Repository & CI/CD

- **URL**: https://192.168.0.11:7743
- **Status**: External Service
- **용도**: 소스 코드 관리, CI/CD 파이프라인

---

## ⚓ Harbor (외부)

### Container Registry

- **URL**: https://192.168.0.12:7801
- **Status**: External Service
- **용도**: Docker 이미지 레지스트리

---

## 📊 서비스 상태 확인

### 전체 Pod 상태
```bash
export KUBECONFIG=/home/user/.kube/config

# pqc-sso namespace
kubectl get pods -n pqc-sso

# argocd namespace
kubectl get pods -n argocd

# dashboard namespace
kubectl get pods -n dashboard
```

### 서비스 확인
```bash
# 모든 서비스 확인
kubectl get svc --all-namespaces

# pqc-sso 서비스
kubectl get svc -n pqc-sso

# NodePort 서비스 확인
kubectl get svc --all-namespaces | grep NodePort
```

### 로그 확인
```bash
# Keycloak 로그
kubectl logs -n pqc-sso -l app=keycloak -f

# SSO Test App 로그
kubectl logs -n pqc-sso -l app=sso-test-app -f

# Luna HSM 로그
kubectl logs -n pqc-sso -l app=luna-hsm-simulator -f

# Argo CD 로그
kubectl logs -n argocd -l app.kubernetes.io/name=argocd-server -f
```

---

## 🔧 Port Forward 명령어 모음

### 한번에 모든 서비스 Port Forward
```bash
#!/bin/bash
export KUBECONFIG=/home/user/.kube/config

# Argo CD
kubectl port-forward -n argocd svc/argocd-server 8443:443 &

# Keycloak
kubectl port-forward -n pqc-sso svc/keycloak 8080:80 &

# SSO Test App
kubectl port-forward -n pqc-sso svc/sso-test-app 3000:80 &

# Luna HSM
kubectl port-forward -n pqc-sso svc/luna-hsm-simulator 8090:8090 &

echo "All port-forwards started in background"
echo "Argo CD: https://localhost:8443"
echo "Keycloak: http://localhost:8080"
echo "SSO Test App: http://localhost:3000"
echo "Luna HSM: http://localhost:8090"
```

### Port Forward 종료
```bash
# 모든 kubectl port-forward 프로세스 종료
pkill -f "kubectl port-forward"
```

---

## 🚀 빠른 시작 가이드

### 1. Dashboard 접속
```
http://192.168.0.11:30090
```
통합 대시보드에서 모든 서비스 정보 확인

### 2. Argo CD 확인
```
https://192.168.0.11:30443
```
- Username: admin
- Password: jOxvYsjXKjwdbWZC
- Application: pqc-sso-stack 상태 확인

### 3. Keycloak 설정
```
http://localhost:8080 (port-forward 필요)
```
- OIDC 클라이언트 생성
- 테스트 사용자 생성

### 4. SSO 테스트
```
http://localhost:3000 (port-forward 필요)
```
- SSO 로그인 테스트

---

## 📋 주요 포트 정리

| 서비스 | 내부 포트 | NodePort | 로컬 Port Forward |
|--------|----------|----------|------------------|
| **Dashboard** | 80 | 30090 | - |
| **Argo CD (HTTP)** | 80 | 30080 | 8080 |
| **Argo CD (HTTPS)** | 443 | 30443 | 8443 |
| **Keycloak** | 80 | - | 8080 |
| **SSO Test App** | 80 | - | 3000 |
| **Luna HSM** | 8090 | - | 8090 |
| **PostgreSQL** | 5432 | - | - |

---

## 🛠️ 트러블슈팅

### Port Forward가 작동하지 않을 때
```bash
# 기존 port-forward 프로세스 확인
ps aux | grep "kubectl port-forward"

# 모두 종료
pkill -f "kubectl port-forward"

# 다시 시작
kubectl port-forward -n pqc-sso svc/keycloak 8080:80
```

### Pod가 Running이 아닐 때
```bash
# Pod 상태 확인
kubectl get pods -n pqc-sso

# Pod 상세 정보
kubectl describe pod -n pqc-sso <pod-name>

# Pod 재시작
kubectl rollout restart deployment/<deployment-name> -n pqc-sso
```

### Argo CD 동기화 오류
```bash
# Application 상태 확인
kubectl get applications -n argocd

# Application 상세 정보
kubectl describe application pqc-sso-stack -n argocd

# 수동 동기화
kubectl patch application pqc-sso-stack -n argocd \
  -p '{"operation":{"initiatedBy":{"username":"admin"},"sync":{}}}' \
  --type merge
```

---

## 📚 관련 문서

- **배포 성공 보고서**: `/home/user/QSIGN/keycloak-hsm/DEPLOYMENT_SUCCESS.md`
- **빠른 시작 가이드**: `/home/user/QSIGN/keycloak-hsm/k8s-full-stack/QUICK_START.md`
- **상세 배포 가이드**: `/home/user/QSIGN/keycloak-hsm/k8s-full-stack/DEPLOYMENT.md`

---

## 🎉 배포 완료!

모든 서비스가 정상적으로 배포되었습니다.

**가장 먼저**: http://192.168.0.11:30090 (통합 Dashboard) 접속하세요!