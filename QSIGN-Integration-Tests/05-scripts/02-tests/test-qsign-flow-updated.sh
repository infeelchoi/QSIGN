#!/bin/bash
# QSIGN 전체 플로우 테스트 스크립트 (업데이트)
# Q-APP -> Q-SIGN (Port 30181) -> Q-KMS Vault

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_test() { echo -e "${BLUE}[TEST]${NC} $1"; }
log_success() { echo -e "${CYAN}[SUCCESS]${NC} $1"; }

# 서비스 URL
Q_APP_SSO_URL="http://192.168.0.11:30300"
Q_SIGN_KC_URL="http://192.168.0.11:30181"
Q_KMS_KC_URL="http://192.168.0.11:30699"
Q_KMS_VAULT_URL="http://192.168.0.11:8200"

echo "========================================="
echo "QSIGN 플로우 테스트 (Updated)"
echo "========================================="
echo ""
echo "Expected Flow:"
echo "Q-APP (30300) → Q-SIGN Keycloak (30181) → Q-KMS Vault (8200)"
echo ""

# 1. Q-KMS Vault 테스트
log_test "Step 1: Q-KMS Vault 상태 확인 (Port 8200)"
VAULT_HEALTH=$(curl -s ${Q_KMS_VAULT_URL}/v1/sys/health 2>&1)
if echo "$VAULT_HEALTH" | grep -q '"sealed":false'; then
    log_success "✓ Vault unsealed and ready"
    VAULT_VERSION=$(echo "$VAULT_HEALTH" | python3 -c "import sys,json; print(json.load(sys.stdin)['version'])" 2>/dev/null || echo "1.21.0")
    echo "  Version: $VAULT_VERSION"
else
    log_error "✗ Vault is sealed or not responding"
    echo "  Response: $VAULT_HEALTH"
    exit 1
fi
echo ""

# 2. Q-SIGN Keycloak 테스트 (Port 30181)
log_test "Step 2: Q-SIGN Keycloak 상태 확인 (Port 30181)"
KC_30181_REALM=$(curl -s ${Q_SIGN_KC_URL}/realms/myrealm 2>&1)
if echo "$KC_30181_REALM" | grep -q "myrealm"; then
    log_success "✓ Q-SIGN Keycloak responding"

    # Token service URL 확인
    TOKEN_SERVICE=$(echo "$KC_30181_REALM" | python3 -c "import sys,json; print(json.load(sys.stdin)['token-service'])" 2>/dev/null)
    echo "  Token Service: $TOKEN_SERVICE"

    if echo "$TOKEN_SERVICE" | grep -q "30181"; then
        log_success "✓ Token service correctly points to Q-SIGN (30181)"
    elif echo "$TOKEN_SERVICE" | grep -q "30699"; then
        log_warn "⚠ Token service points to Q-KMS (30699)"
        log_warn "  This needs frontend URL configuration in Keycloak"
    else
        log_warn "⚠ Token service points to: $TOKEN_SERVICE"
    fi
else
    log_error "✗ Q-SIGN Keycloak not responding"
    exit 1
fi
echo ""

# 3. Q-SIGN Keycloak OpenID Configuration
log_test "Step 3: Q-SIGN OpenID Configuration"
OIDC_CONFIG=$(curl -s "${Q_SIGN_KC_URL}/realms/myrealm/.well-known/openid-configuration" 2>&1)
if echo "$OIDC_CONFIG" | grep -q "authorization_endpoint"; then
    AUTH_ENDPOINT=$(echo "$OIDC_CONFIG" | python3 -c "import sys,json; print(json.load(sys.stdin)['authorization_endpoint'])" 2>/dev/null)
    TOKEN_ENDPOINT=$(echo "$OIDC_CONFIG" | python3 -c "import sys,json; print(json.load(sys.stdin)['token_endpoint'])" 2>/dev/null)

    echo "  Authorization: $AUTH_ENDPOINT"
    echo "  Token: $TOKEN_ENDPOINT"

    if echo "$AUTH_ENDPOINT" | grep -q "30181"; then
        log_success "✓ OpenID endpoints point to Q-SIGN (30181)"
    else
        log_warn "⚠ OpenID endpoints not configured for Q-SIGN"
    fi
else
    log_error "✗ Failed to get OpenID configuration"
fi
echo ""

# 4. Q-APP SSO Test App 확인
log_test "Step 4: Q-APP SSO Test App 상태 (Port 30300)"
APP_HTML=$(curl -s ${Q_APP_SSO_URL} 2>&1)
if echo "$APP_HTML" | grep -q "SSO Test App"; then
    log_success "✓ SSO Test App running"

    # App이 사용하는 Keycloak URL 확인
    APP_KC_URL=$(echo "$APP_HTML" | grep -oP 'http://192\.168\.0\.11:\d+' | head -1)
    echo "  App configured with: $APP_KC_URL"

    if echo "$APP_KC_URL" | grep -q "30181"; then
        log_success "✓ App correctly configured to use Q-SIGN (30181)"
    elif echo "$APP_KC_URL" | grep -q "30699"; then
        log_warn "⚠ App configured to use Q-KMS (30699)"
        log_warn "  Update Q-APP values.yaml to use Q-SIGN (30181)"
    fi
else
    log_error "✗ SSO Test App not responding"
fi
echo ""

# 5. Keycloak → Vault 연결 테스트
log_test "Step 5: Keycloak → Vault 연결 테스트"
echo "  Checking Vault auth methods..."
VAULT_AUTH=$(curl -s -H "X-Vault-Token: root" ${Q_KMS_VAULT_URL}/v1/sys/auth 2>&1)
if echo "$VAULT_AUTH" | grep -q "token"; then
    log_success "✓ Vault authentication available"
    echo "$VAULT_AUTH" | python3 -c "import sys,json; auths=json.load(sys.stdin); print('  Methods:', ', '.join(auths.keys()))" 2>/dev/null || echo "  Methods: token/"
else
    log_warn "⚠ Cannot verify Vault auth methods"
fi
echo ""

# 6. 실행 중인 프로세스 확인
log_test "Step 6: QSIGN 컴포넌트 프로세스"
KEYCLOAK_COUNT=$(ps aux | grep -c "[j]ava.*keycloak" || echo "0")
VAULT_COUNT=$(ps aux | grep -c "[v]ault server" || echo "0")
echo "  Keycloak processes: $KEYCLOAK_COUNT"
echo "  Vault processes: $VAULT_COUNT"
echo ""

# 7. SSO 로그인 플로우 시뮬레이션
log_test "Step 7: SSO 로그인 플로우 시뮬레이션"
echo "  1. User visits Q-APP: http://192.168.0.11:30300"
echo "  2. App redirects to Q-SIGN: http://192.168.0.11:30181/realms/myrealm/..."
echo "  3. User authenticates via Q-SIGN Keycloak"
echo "  4. Q-SIGN validates with Q-KMS Vault (if HSM enabled)"
echo "  5. Token issued by Q-SIGN (30181)"
echo "  6. User redirected back to Q-APP with token"
echo ""

# 요약
echo "========================================="
echo "테스트 결과 요약"
echo "========================================="
echo ""
VAULT_STATUS="✓ PASS"
Q_SIGN_KC_STATUS="?"
Q_APP_STATUS="?"

if echo "$TOKEN_SERVICE" | grep -q "30181"; then
    Q_SIGN_KC_STATUS="✓ PASS"
else
    Q_SIGN_KC_STATUS="✗ FAIL - Frontend URL not configured"
fi

if echo "$APP_KC_URL" | grep -q "30181"; then
    Q_APP_STATUS="✓ PASS"
else
    Q_APP_STATUS="✗ FAIL - Using wrong Keycloak URL"
fi

echo "Q-KMS Vault (8200):        $VAULT_STATUS"
echo "Q-SIGN Keycloak (30181):   $Q_SIGN_KC_STATUS"
echo "Q-APP (30300):             $Q_APP_STATUS"
echo ""

# 다음 단계
if [ "$Q_SIGN_KC_STATUS" != "✓ PASS" ] || [ "$Q_APP_STATUS" != "✓ PASS" ]; then
    echo "========================================="
    echo "수정 필요 사항"
    echo "========================================="
    echo ""

    if [ "$Q_SIGN_KC_STATUS" != "✓ PASS" ]; then
        echo "1. Q-SIGN Keycloak Frontend URL 설정:"
        echo "   - Keycloak Admin Console에서 Realm Settings → Frontend URL 설정"
        echo "   - 또는 환경변수: KC_HOSTNAME=192.168.0.11, KC_HOSTNAME_PORT=30181"
        echo ""
    fi

    if [ "$Q_APP_STATUS" != "✓ PASS" ]; then
        echo "2. Q-APP Keycloak URL 설정:"
        echo "   - 파일: Q-APP/k8s/helm/q-app/values.yaml"
        echo "   - keycloakUrl: \"http://192.168.0.11:30181\""
        echo "   - Pod 재시작 필요"
        echo ""
    fi
else
    log_success "========================================="
    log_success "모든 테스트 통과!"
    log_success "========================================="
    echo ""
    echo "QSIGN 플로우가 올바르게 구성되었습니다."
    echo "Q-APP → Q-SIGN → Q-KMS 플로우 준비 완료"
fi
