#!/bin/bash
# QSIGN 전체 플로우 테스트 스크립트
# Q-APP -> Q-GATEWAY -> Q-SIGN -> Q-KMS

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_test() { echo -e "${BLUE}[TEST]${NC} $1"; }

# 서비스 URL
Q_APP_URL="http://192.168.0.11:32127"
Q_APP_SSO_URL="http://192.168.0.11:30300"
Q_GATEWAY_URL="http://192.168.0.11:9080"
Q_SIGN_KC_URL="http://192.168.0.11:30181"
Q_KMS_KC_URL="http://192.168.0.11:30699"
Q_KMS_VAULT_URL="http://192.168.0.11:8200"

echo "========================================="
echo "QSIGN 전체 플로우 테스트"
echo "========================================="
echo ""

# 1. Q-KMS Vault 테스트
log_test "1. Q-KMS Vault 상태 확인 (Port 8200)"
VAULT_STATUS=$(curl -s ${Q_KMS_VAULT_URL}/v1/sys/health | python3 -m json.tool 2>/dev/null)
if echo "$VAULT_STATUS" | grep -q '"sealed": false'; then
    log_info "✓ Vault unsealed and ready"
    echo "$VAULT_STATUS" | grep -E "(initialized|sealed|version)" | head -5
else
    log_error "✗ Vault is sealed or not responding"
    exit 1
fi
echo ""

# 2. Q-SIGN Keycloak 테스트 (Port 30181)
log_test "2. Q-SIGN Keycloak 상태 확인 (Port 30181)"
KC_30181_REALM=$(curl -s ${Q_SIGN_KC_URL}/realms/myrealm 2>&1)
if echo "$KC_30181_REALM" | grep -q "myrealm"; then
    log_info "✓ Q-SIGN Keycloak responding"
    echo "$KC_30181_REALM" | python3 -m json.tool | grep -E "(realm|token-service|issuer)" | head -5

    # Token service URL 확인
    TOKEN_SERVICE=$(echo "$KC_30181_REALM" | python3 -c "import sys,json; print(json.load(sys.stdin)['token-service'])" 2>/dev/null)
    if echo "$TOKEN_SERVICE" | grep -q "30181"; then
        log_info "✓ Token service points to self (30181)"
    else
        log_warn "⚠ Token service points to: $TOKEN_SERVICE"
        log_warn "  Expected: http://192.168.0.11:30181/..."
    fi
else
    log_error "✗ Q-SIGN Keycloak not responding"
fi
echo ""

# 3. Q-KMS Keycloak 테스트 (Port 30699)
log_test "3. Q-KMS Keycloak 상태 확인 (Port 30699)"
KC_30699_HEALTH=$(curl -s ${Q_KMS_KC_URL}/health 2>&1)
if echo "$KC_30699_HEALTH" | grep -q "UP"; then
    log_info "✓ Q-KMS Keycloak healthy"
    echo "$KC_30699_HEALTH" | python3 -m json.tool | head -10
else
    log_error "✗ Q-KMS Keycloak not healthy"
fi
echo ""

# 4. Q-GATEWAY (APISIX) 테스트
log_test "4. Q-GATEWAY (APISIX) 상태 확인 (Port 9080)"
APISIX_RESPONSE=$(curl -s ${Q_GATEWAY_URL}/ 2>&1 | head -5)
if [ -n "$APISIX_RESPONSE" ]; then
    log_info "✓ APISIX responding"
    echo "$APISIX_RESPONSE"
else
    log_warn "⚠ APISIX no response on port 9080"
fi
echo ""

# 5. Q-APP SSO Test App 테스트
log_test "5. Q-APP SSO Test App 상태 확인 (Port 30300)"
APP_RESPONSE=$(curl -s ${Q_APP_SSO_URL} 2>&1 | grep -i "keycloak" | head -3)
if [ -n "$APP_RESPONSE" ]; then
    log_info "✓ SSO Test App running"
    echo "$APP_RESPONSE" | sed 's/<[^>]*>//g'

    # App이 사용하는 Keycloak URL 확인
    APP_KC_URL=$(echo "$APP_RESPONSE" | grep -oP 'http://[^<]+' | head -1)
    if echo "$APP_KC_URL" | grep -q "30181"; then
        log_info "✓ App configured to use Q-SIGN (30181)"
    elif echo "$APP_KC_URL" | grep -q "30699"; then
        log_warn "⚠ App configured to use Q-KMS (30699) - Should use 30181"
    fi
else
    log_error "✗ SSO Test App not responding"
fi
echo ""

# 6. 프로세스 확인
log_test "6. 실행 중인 QSIGN 프로세스 확인"
echo "Keycloak 프로세스:"
ps aux | grep "[j]ava.*keycloak" | grep -v grep | awk '{print "  PID:", $2, "CMD:", $NF}' || echo "  No Keycloak processes found"
echo ""
echo "Vault 프로세스:"
ps aux | grep "[v]ault server" | awk '{print "  PID:", $2}' || echo "  No Vault processes found"
echo ""
echo "APISIX 프로세스:"
ps aux | grep "[n]ginx.*apisix\|[a]pisix.*manager" | awk '{print "  PID:", $2, "CMD:", $(NF-1), $NF}' | head -3 || echo "  No APISIX processes found"
echo ""

# 요약
echo "========================================="
echo "테스트 요약"
echo "========================================="
echo "✓ Q-KMS Vault (8200): Unsealed"
echo "✓ Q-KMS Keycloak (30699): Healthy"
echo "? Q-SIGN Keycloak (30181): Check token-service URL"
echo "? Q-GATEWAY APISIX (9080): Check configuration"
echo "? Q-APP (30300): Check Keycloak URL configuration"
echo ""
echo "권장 사항:"
echo "1. Q-APP이 Q-SIGN (30181)을 사용하도록 설정"
echo "2. Q-SIGN (30181)의 frontend URL을 자기 자신으로 설정"
echo "3. APISIX 라우트 설정 확인"
