#!/bin/bash

# ======================================================================
#  Keycloak Pod 재시작 스크립트
# ======================================================================
# ArgoCD 동기화 또는 kubectl을 통한 Keycloak Pod 재시작
# ======================================================================

set -e

echo "======================================================================"
echo "  Keycloak Pod 재시작"
echo "======================================================================"
echo ""

# Method 1: kubectl delete pod (권한 있을 경우)
echo "→ kubectl을 통한 Keycloak Pod 재시작 시도..."
if sudo k3s kubectl delete pod -n q-sign -l app=keycloak-pqc 2>/dev/null; then
    echo "✅ Keycloak Pod 재시작 명령 성공"
    echo ""
    echo "→ Pod 재시작 대기 중..."
    sleep 10

    echo "→ Pod 상태 확인..."
    sudo k3s kubectl get pods -n q-sign -l app=keycloak-pqc

    echo ""
    echo "✅ Keycloak Pod 재시작 완료!"
    exit 0
fi

echo "❌ kubectl 접근 실패"
echo ""

# Method 2: ArgoCD CLI (토큰 있을 경우)
echo "→ ArgoCD CLI를 통한 동기화 시도..."
if argocd app sync q-sign 2>/dev/null; then
    echo "✅ ArgoCD 동기화 성공"
    exit 0
fi

echo "❌ ArgoCD CLI 실패 (인증 필요)"
echo ""

# Method 3: 사용자 안내
echo "======================================================================"
echo "  수동 재시작 방법"
echo "======================================================================"
echo ""
echo "ArgoCD UI를 통한 수동 동기화:"
echo "  1. 브라우저 열기: https://192.168.0.11:30443"
echo "  2. 로그인 (admin / [password])"
echo "  3. 'q-sign' 애플리케이션 클릭"
echo "  4. 'REFRESH' 버튼 클릭"
echo "  5. 'SYNC' 버튼 클릭"
echo ""
echo "또는 ArgoCD 자동 동기화 대기 (약 3분):"
echo "  - ArgoCD는 3분마다 Git 저장소를 폴링합니다"
echo "  - 변경사항이 감지되면 자동으로 동기화됩니다"
echo ""
echo "======================================================================"
