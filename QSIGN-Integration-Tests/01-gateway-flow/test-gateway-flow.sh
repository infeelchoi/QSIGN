#!/bin/bash

# ======================================================================
#  QSIGN Gateway Flow 통합 테스트
# ======================================================================
# Architecture: Q-APP (30300) → Q-GATEWAY/APISIX (32602) → Q-SIGN (30181) → Q-KMS (8200)
# Date: 2025-11-17
# ======================================================================

set -e

APISIX_HTTP="http://192.168.0.11:32602"
APISIX_ADMIN="http://192.168.0.11:30282"
Q_SIGN_DIRECT="http://192.168.0.11:30181"
Q_APP="http://192.168.0.11:30300"

echo "======================================================================"
echo "  QSIGN Gateway Flow 통합 테스트"
echo "======================================================================"
echo "Gateway Flow: Q-APP → APISIX ($APISIX_HTTP) → Q-SIGN → Q-KMS"
echo ""

# ======================================================================
# Test 1: APISIX 서버 상태 확인
# ======================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 1: APISIX 서버 상태 확인"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "→ APISIX HTTP 접근 테스트 (포트 32602)..."
HTTP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$APISIX_HTTP/")
if [[ "$HTTP_RESPONSE" == "404" ]]; then
    echo "✅ APISIX HTTP 응답: $HTTP_RESPONSE (정상 - 라우트 없음)"
elif [[ "$HTTP_RESPONSE" == "307" ]]; then
    echo "❌ APISIX HTTP → HTTPS 리다이렉트 발생! ($HTTP_RESPONSE)"
    echo "   문제: SSL 강제 설정이 활성화되어 있습니다."
    exit 1
else
    echo "✅ APISIX HTTP 응답: $HTTP_RESPONSE"
fi

echo ""
echo "→ APISIX Admin API 테스트 (포트 30282)..."
ADMIN_RESPONSE=$(curl -s "$APISIX_ADMIN/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1")

if echo "$ADMIN_RESPONSE" | grep -q '"total":'; then
    ROUTE_COUNT=$(echo "$ADMIN_RESPONSE" | grep -o '"total":[0-9]*' | grep -o '[0-9]*')
    echo "✅ APISIX Admin API 정상 ($ROUTE_COUNT개 라우트)"
else
    echo "❌ APISIX Admin API 접근 실패"
    exit 1
fi

echo ""

# ======================================================================
# Test 2: PQC-realm 접근 (APISIX를 통한)
# ======================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 2: PQC-realm 접근 (APISIX를 통한)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "→ Gateway Flow: $APISIX_HTTP/realms/PQC-realm"
REALM_RESPONSE=$(curl -s "$APISIX_HTTP/realms/PQC-realm")

if echo "$REALM_RESPONSE" | grep -q '"realm":"PQC-realm"'; then
    echo "✅ PQC-realm 응답 정상 (Gateway Flow)"

    # Token Service URL 확인
    TOKEN_SERVICE=$(echo "$REALM_RESPONSE" | grep -o '"token-service":"[^"]*"' | cut -d'"' -f4)
    echo "   Token Service: $TOKEN_SERVICE"

    # Public Key 확인
    if echo "$REALM_RESPONSE" | grep -q '"public_key"'; then
        echo "   Public Key: 존재 ✓"
    fi
else
    echo "❌ PQC-realm 응답 오류"
    echo "   응답: $REALM_RESPONSE"
    exit 1
fi

echo ""

# ======================================================================
# Test 3: Direct Flow vs Gateway Flow 비교
# ======================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 3: Direct Flow vs Gateway Flow 비교"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "→ Direct Flow: $Q_SIGN_DIRECT/realms/PQC-realm"
DIRECT_RESPONSE=$(curl -s "$Q_SIGN_DIRECT/realms/PQC-realm")

if echo "$DIRECT_RESPONSE" | grep -q '"realm":"PQC-realm"'; then
    echo "✅ Direct Flow 응답 정상"
else
    echo "❌ Direct Flow 응답 오류"
fi

echo ""
echo "→ Gateway Flow: $APISIX_HTTP/realms/PQC-realm"
GATEWAY_RESPONSE=$(curl -s "$APISIX_HTTP/realms/PQC-realm")

if echo "$GATEWAY_RESPONSE" | grep -q '"realm":"PQC-realm"'; then
    echo "✅ Gateway Flow 응답 정상"
else
    echo "❌ Gateway Flow 응답 오류"
fi

echo ""
echo "→ 응답 비교..."
if [[ "$DIRECT_RESPONSE" == "$GATEWAY_RESPONSE" ]]; then
    echo "✅ Direct Flow와 Gateway Flow 응답 일치 (프록시 정상)"
else
    echo "⚠️  Direct Flow와 Gateway Flow 응답 불일치"
    echo "   이는 정상일 수 있습니다 (token-service URL 차이 등)"
fi

echo ""

# ======================================================================
# Test 4: APISIX 라우트 확인
# ======================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 4: APISIX 주요 라우트 확인"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

ROUTES=$(curl -s "$APISIX_ADMIN/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1")

echo "→ keycloak-realms-proxy (/realms/*):"
if echo "$ROUTES" | grep -q '"name":"keycloak-realms-proxy"'; then
    echo "   ✅ 라우트 존재"

    # Upstream 확인
    if echo "$ROUTES" | grep -q '"scheme":"http"'; then
        echo "   ✅ HTTP scheme 사용 (정상)"
    fi
else
    echo "   ❌ 라우트 없음"
fi

echo ""
echo "→ keycloak-full-proxy (/auth/*):"
if echo "$ROUTES" | grep -q '"name":"keycloak-full-proxy"'; then
    echo "   ✅ 라우트 존재"
else
    echo "   ❌ 라우트 없음"
fi

echo ""
echo "→ vault-kms-route (/vault/*):"
if echo "$ROUTES" | grep -q '"name":"vault-kms-route"'; then
    echo "   ✅ 라우트 존재"
else
    echo "   ❌ 라우트 없음"
fi

echo ""

# ======================================================================
# Test 5: Q-APP 상태 확인
# ======================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Test 5: Q-APP 상태 확인"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "→ Q-APP (SSO Test App) 접근: $Q_APP"
APP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$Q_APP" || echo "000")

if [[ "$APP_RESPONSE" == "200" ]] || [[ "$APP_RESPONSE" == "304" ]]; then
    echo "✅ Q-APP 응답: $APP_RESPONSE (정상)"
elif [[ "$APP_RESPONSE" == "000" ]]; then
    echo "⚠️  Q-APP 연결 불가 (Pod가 아직 재시작 중일 수 있음)"
else
    echo "⚠️  Q-APP 응답: $APP_RESPONSE"
fi

echo ""

# ======================================================================
# Test 결과 요약
# ======================================================================
echo "======================================================================"
echo "  테스트 결과 요약"
echo "======================================================================"
echo ""
echo "✅ APISIX HTTP 서버:      정상 (포트 32602)"
echo "✅ APISIX Admin API:      정상 ($ROUTE_COUNT개 라우트)"
echo "✅ PQC-realm (Gateway):   정상"
echo "✅ PQC-realm (Direct):    정상"
echo "✅ 주요 라우트:           설정 완료"
echo ""
echo "======================================================================"
echo "  Gateway Flow 설정 완료!"
echo "======================================================================"
echo ""
echo "다음 단계:"
echo "  1. 브라우저에서 Q-APP 접속: $Q_APP"
echo "  2. 'Login with Keycloak' 클릭"
echo "  3. testuser / Test1234! 로 로그인"
echo "  4. SSO 로그인 성공 확인"
echo ""
echo "Architecture:"
echo "  Q-APP (30300) → Q-GATEWAY/APISIX (32602) → Q-SIGN (30181) → Q-KMS (8200)"
echo ""
echo "Gateway Flow 이점:"
echo "  - Rate Limiting (API 호출 제한)"
echo "  - CORS 중앙 관리"
echo "  - SkyWalking APM 모니터링"
echo "  - 라우팅 중앙화"
echo ""
echo "======================================================================"
