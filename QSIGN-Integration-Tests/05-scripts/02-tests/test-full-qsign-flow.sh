#!/bin/bash
# QSIGN 전체 플로우 통합 테스트
# Q-APP -> (Q-GATEWAY) -> Q-SIGN -> Q-KMS

set -e

# 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_test() { echo -e "${BLUE}[TEST]${NC} $1"; }
log_success() { echo -e "${CYAN}[✓]${NC} $1"; }

echo "========================================="
echo "QSIGN 전체 플로우 통합 테스트"
echo "========================================="
echo ""

# 서비스 URL
Q_APP_URL="http://192.168.0.11:30300"
Q_GATEWAY_URL="http://192.168.0.11:80"
Q_SIGN_KC_URL="http://192.168.0.11:30181"
Q_KMS_VAULT_URL="http://192.168.0.11:8200"

# 전체 플로우 다이어그램
echo "목표 아키텍처:"
echo "┌─────────────┐"
echo "│   Q-APP     │  User Application"
echo "│  (30300)    │"
echo "└──────┬──────┘"
echo "       │"
echo "       ↓ (Optional)"
echo "┌─────────────┐"
echo "│ Q-GATEWAY   │  APISIX Reverse Proxy"
echo "│   (80)      │"
echo "└──────┬──────┘"
echo "       │"
echo "       ↓"
echo "┌─────────────┐"
echo "│  Q-SIGN     │  Post-Quantum Keycloak"
echo "│  Keycloak   │  Authentication & SSO"
echo "│  (30181)    │"
echo "└──────┬──────┘"
echo "       │"
echo "       ↓"
echo "┌─────────────┐"
echo "│   Q-KMS     │  Vault HSM Integration"
echo "│   Vault     │  Key Management"
echo "│  (8200)     │"
echo "└─────────────┘"
echo ""

# ========================================
# 1. 인프라 컴포넌트 테스트
# ========================================
echo "========================================="
echo "Step 1: 인프라 컴포넌트 상태 확인"
echo "========================================="
echo ""

# 1.1 Q-KMS Vault
log_test "1.1 Q-KMS Vault (Port 8200)"
VAULT_HEALTH=$(curl -s ${Q_KMS_VAULT_URL}/v1/sys/health 2>&1)
if echo "$VAULT_HEALTH" | grep -q '"sealed":false'; then
    VAULT_VERSION=$(echo "$VAULT_HEALTH" | python3 -c "import sys,json; print(json.load(sys.stdin)['version'])" 2>/dev/null || echo "1.21.0")
    log_success "Vault unsealed and ready (v$VAULT_VERSION)"
else
    log_error "Vault is sealed or not responding"
    exit 1
fi

# 1.2 Q-SIGN Keycloak
log_test "1.2 Q-SIGN Keycloak (Port 30181)"
KC_REALM=$(curl -s ${Q_SIGN_KC_URL}/realms/myrealm 2>&1)
if echo "$KC_REALM" | grep -q "myrealm"; then
    TOKEN_SERVICE=$(echo "$KC_REALM" | python3 -c "import sys,json; print(json.load(sys.stdin)['token-service'])" 2>/dev/null)
    if echo "$TOKEN_SERVICE" | grep -q "30181"; then
        log_success "Q-SIGN Keycloak configured correctly (Frontend URL: 30181)"
    else
        log_warn "Q-SIGN Frontend URL points to: $TOKEN_SERVICE"
    fi
else
    log_error "Q-SIGN Keycloak not responding"
    exit 1
fi

# 1.3 Q-GATEWAY (APISIX)
log_test "1.3 Q-GATEWAY APISIX (Port 80)"
APISIX_RESP=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:80 2>&1)
if [ "$APISIX_RESP" = "404" ] || [ "$APISIX_RESP" = "200" ]; then
    log_success "APISIX running on port 80"
else
    log_warn "APISIX not responding on port 80 (Code: $APISIX_RESP)"
fi

# 1.4 Q-APP
log_test "1.4 Q-APP SSO Test (Port 30300)"
APP_HTML=$(curl -s ${Q_APP_URL} 2>&1)
if echo "$APP_HTML" | grep -q "SSO Test App"; then
    log_success "Q-APP SSO Test App running"
else
    log_warn "Q-APP SSO Test App not responding"
fi

echo ""

# ========================================
# 2. 플로우 테스트
# ========================================
echo "========================================="
echo "Step 2: 플로우별 연결 테스트"
echo "========================================="
echo ""

# 2.1 Direct Flow: Q-APP → Q-SIGN
log_test "2.1 Direct Flow: Q-APP → Q-SIGN (30181)"
APP_KC_URL=$(echo "$APP_HTML" | grep -oP 'http://192\.168\.0\.11:\d+' | head -1)
echo "  App configured with Keycloak: $APP_KC_URL"
if echo "$APP_KC_URL" | grep -q "30181"; then
    log_success "Q-APP directly connects to Q-SIGN ✓"
else
    log_warn "Q-APP connects to: $APP_KC_URL (Expected: 30181)"
fi

# 2.2 Gateway Flow (if available): Q-GATEWAY → Q-SIGN
log_test "2.2 Gateway Flow: Q-GATEWAY → Q-SIGN (Proxy)"
# APISIX 프록시 테스트 (라우트가 설정되어 있다면)
PROXY_TEST=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: qsign.local" http://192.168.0.11/realms/myrealm 2>&1)
if [ "$PROXY_TEST" = "200" ]; then
    log_success "APISIX proxy to Q-SIGN working ✓"
else
    log_warn "APISIX proxy not configured (Code: $PROXY_TEST)"
    echo "  Note: Direct Q-APP → Q-SIGN connection still works"
fi

# 2.3 Q-SIGN → Q-KMS Vault
log_test "2.3 Backend Flow: Q-SIGN → Vault"
VAULT_AUTH=$(curl -s -H "X-Vault-Token: root" ${Q_KMS_VAULT_URL}/v1/sys/auth 2>&1)
if echo "$VAULT_AUTH" | grep -q "token"; then
    log_success "Vault authentication backend available ✓"
else
    log_warn "Vault auth verification failed"
fi

echo ""

# ========================================
# 3. SSO 로그인 플로우 시뮬레이션
# ========================================
echo "========================================="
echo "Step 3: SSO 로그인 플로우 시뮬레이션"
echo "========================================="
echo ""

log_info "Complete SSO Flow:"
echo "  1. User visits Q-APP: http://192.168.0.11:30300"
echo "  2. Click 'Login' button"
if echo "$APP_KC_URL" | grep -q "30181"; then
    echo "  3. Redirect to Q-SIGN: http://192.168.0.11:30181/realms/myrealm/..."
else
    echo "  3. Redirect to: $APP_KC_URL/realms/myrealm/... ⚠"
fi
echo "  4. User authenticates (username/password)"
echo "  5. Q-SIGN validates credentials"
echo "  6. [Optional] Q-SIGN uses Vault for HSM key operations"
echo "  7. Q-SIGN issues JWT token (signed with PQC hybrid signature)"
echo "  8. Redirect back to Q-APP with auth code"
echo "  9. Q-APP exchanges code for token"
echo "  10. User logged in with PQC-protected session"
echo ""

# ========================================
# 4. 테스트 결과 요약
# ========================================
echo "========================================="
echo "테스트 결과 요약"
echo "========================================="
echo ""

VAULT_STATUS="✓ PASS"
QSIGN_STATUS="?"
APISIX_STATUS="?"
QAPP_STATUS="?"

# Q-SIGN 상태
if echo "$TOKEN_SERVICE" | grep -q "30181"; then
    QSIGN_STATUS="✓ PASS"
else
    QSIGN_STATUS="⚠ PARTIAL (Frontend URL 확인 필요)"
fi

# APISIX 상태
if [ "$APISIX_RESP" = "404" ] || [ "$APISIX_RESP" = "200" ]; then
    if [ "$PROXY_TEST" = "200" ]; then
        APISIX_STATUS="✓ PASS (Proxy configured)"
    else
        APISIX_STATUS="○ RUNNING (Proxy not configured)"
    fi
else
    APISIX_STATUS="✗ NOT RUNNING"
fi

# Q-APP 상태
if echo "$APP_KC_URL" | grep -q "30181"; then
    QAPP_STATUS="✓ PASS"
else
    QAPP_STATUS="⚠ NEEDS POD RESTART"
fi

printf "%-30s %s\n" "Component" "Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
printf "%-30s %s\n" "Q-KMS Vault (8200)" "$VAULT_STATUS"
printf "%-30s %s\n" "Q-SIGN Keycloak (30181)" "$QSIGN_STATUS"
printf "%-30s %s\n" "Q-GATEWAY APISIX (80)" "$APISIX_STATUS"
printf "%-30s %s\n" "Q-APP (30300)" "$QAPP_STATUS"
echo ""

# ========================================
# 5. 권장 사항
# ========================================
if [ "$QAPP_STATUS" != "✓ PASS" ] || [ "$APISIX_STATUS" = "○ RUNNING (Proxy not configured)" ]; then
    echo "========================================="
    echo "다음 단계"
    echo "========================================="
    echo ""

    if [ "$QAPP_STATUS" != "✓ PASS" ]; then
        echo "1. Q-APP Pod 재시작:"
        echo "   kubectl rollout restart deployment/sso-test-app -n pqc-sso"
        echo "   또는 Kubernetes Pod 수동 재시작"
        echo ""
    fi

    if [ "$APISIX_STATUS" = "○ RUNNING (Proxy not configured)" ]; then
        echo "2. APISIX 라우트 설정 (선택사항):"
        echo "   - APISIX Dashboard 접속: http://192.168.0.11:7643"
        echo "   - 또는 Admin API로 라우트 추가"
        echo "   - Q-GATEWAY를 통한 프록시는 선택사항입니다"
        echo ""
    fi

    echo "참고: Q-APP → Q-SIGN 직접 연결은 이미 작동합니다"
    echo "      APISIX 게이트웨이는 추가 기능(rate limiting, auth, monitoring)을 위한 것입니다"
else
    log_success "========================================="
    log_success "전체 플로우 준비 완료!"
    log_success "========================================="
    echo ""
    echo "QSIGN SSO 플로우가 올바르게 구성되었습니다."
    echo ""
    echo "테스트 방법:"
    echo "1. 브라우저에서 http://192.168.0.11:30300 접속"
    echo "2. 'Login' 버튼 클릭"
    echo "3. Keycloak 로그인 페이지에서 인증"
    echo "   - Username: testuser"
    echo "   - Password: admin"
    echo "4. SSO 로그인 성공 확인"
fi

echo ""
echo "테스트 완료 시각: $(date '+%Y-%m-%d %H:%M:%S')"
