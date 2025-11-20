#!/bin/bash
# APISIX PQC-realm 라우트 초기화 스크립트
# Q-APP → APISIX → Q-SIGN (PQC-realm) → Q-KMS (Vault)

set -e

APISIX_ADMIN_URL="${APISIX_ADMIN_URL:-http://192.168.0.11:9180}"
APISIX_API_KEY="${APISIX_API_KEY:-edd1c9f034335f136f87ad84b625c8f1}"
Q_SIGN_HOST="${Q_SIGN_HOST:-192.168.0.11:30181}"

echo "======================================================================"
echo "  APISIX PQC-realm 라우트 초기화"
echo "======================================================================"
echo "APISIX Admin URL: $APISIX_ADMIN_URL"
echo "Q-SIGN Host: $Q_SIGN_HOST"
echo ""

# APISIX가 준비될 때까지 대기
echo "⏳ APISIX 서버 준비 대기 중..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s "$APISIX_ADMIN_URL/apisix/admin/routes" -H "X-API-KEY: $APISIX_API_KEY" > /dev/null 2>&1; then
        echo "✅ APISIX 서버 준비 완료!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "   대기 중... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ APISIX 서버가 준비되지 않았습니다."
    echo "   Admin API: $APISIX_ADMIN_URL"
    echo ""
    echo "문제 해결:"
    echo "  1. APISIX가 실행 중인지 확인"
    echo "  2. Admin API 포트(9180) 확인"
    echo "  3. API Key 확인"
    exit 1
fi

# 라우트 생성 함수
create_route() {
    local ROUTE_ID=$1
    local ROUTE_NAME=$2
    local ROUTE_DATA=$3

    echo ""
    echo "📝 라우트 생성 중: $ROUTE_NAME (ID: $ROUTE_ID)"

    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$APISIX_ADMIN_URL/apisix/admin/routes/$ROUTE_ID" \
        -H "X-API-KEY: $APISIX_API_KEY" \
        -H "Content-Type: application/json" \
        -d "$ROUTE_DATA")

    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
        echo "   ✅ 라우트 생성 성공: $ROUTE_NAME"
    else
        echo "   ❌ 라우트 생성 실패: $ROUTE_NAME (HTTP $HTTP_CODE)"
        echo "$RESPONSE" | head -n -1
        return 1
    fi
}

# Upstream 생성
echo ""
echo "📝 Upstream 생성 중: Q-SIGN Keycloak"

UPSTREAM_DATA=$(cat <<EOF
{
  "name": "q-sign-keycloak",
  "desc": "Q-SIGN Keycloak PQC Backend",
  "type": "roundrobin",
  "scheme": "http",
  "nodes": {
    "${Q_SIGN_HOST}": 1
  },
  "timeout": {
    "connect": 10,
    "send": 10,
    "read": 60
  },
  "keepalive_pool": {
    "size": 320,
    "idle_timeout": 60,
    "requests": 1000
  },
  "retries": 2
}
EOF
)

UPSTREAM_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$APISIX_ADMIN_URL/apisix/admin/upstreams/q-sign-keycloak" \
    -H "X-API-KEY: $APISIX_API_KEY" \
    -H "Content-Type: application/json" \
    -d "$UPSTREAM_DATA")

HTTP_CODE=$(echo "$UPSTREAM_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "   ✅ Upstream 생성 성공"
else
    echo "   ⚠ Upstream 생성 실패 또는 이미 존재 (HTTP $HTTP_CODE)"
fi

# PQC-realm Route (최우선)
PQC_REALM_ROUTE=$(cat <<EOF
{
  "name": "q-sign-pqc-realm",
  "desc": "Q-SIGN PQC-realm Authentication Route",
  "uri": "/realms/PQC-realm/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  "priority": 100,
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "cors": {
      "allow_origins": "http://192.168.0.11:30300,http://192.168.0.11:30210,http://192.168.0.11:30201,http://192.168.0.11:30202",
      "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
      "allow_headers": "DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization",
      "expose_headers": "Content-Length,Content-Type",
      "max_age": 3600,
      "allow_credential": true
    },
    "limit-req": {
      "rate": 100,
      "burst": 50,
      "key_type": "var",
      "key": "remote_addr",
      "rejected_code": 429
    },
    "prometheus": {
      "prefer_name": true
    }
  },
  "status": 1
}
EOF
)

create_route "pqc-realm" "PQC-realm Route" "$PQC_REALM_ROUTE"

# All Realms Route (fallback)
REALMS_ROUTE=$(cat <<EOF
{
  "name": "q-sign-realms",
  "desc": "Q-SIGN All Realms Route",
  "uri": "/realms/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  "priority": 50,
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "cors": {
      "allow_origins": "http://192.168.0.11:30300",
      "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
      "allow_headers": "*",
      "allow_credential": true
    },
    "prometheus": {
      "prefer_name": true
    }
  },
  "status": 1
}
EOF
)

create_route "realms" "All Realms Route" "$REALMS_ROUTE"

# Admin API Route
ADMIN_ROUTE=$(cat <<EOF
{
  "name": "q-sign-admin",
  "desc": "Q-SIGN Keycloak Admin API Route",
  "uri": "/admin/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  "priority": 80,
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "cors": {
      "allow_origins": "*",
      "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
      "allow_headers": "*",
      "allow_credential": true
    },
    "limit-req": {
      "rate": 50,
      "burst": 20,
      "key": "remote_addr",
      "rejected_code": 429
    }
  },
  "status": 1
}
EOF
)

create_route "admin" "Admin API Route" "$ADMIN_ROUTE"

# Resources Route
RESOURCES_ROUTE=$(cat <<EOF
{
  "name": "q-sign-resources",
  "desc": "Q-SIGN Keycloak Resources Route",
  "uri": "/resources/*",
  "methods": ["GET"],
  "priority": 70,
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "prometheus": {
      "prefer_name": true
    }
  },
  "status": 1
}
EOF
)

create_route "resources" "Resources Route" "$RESOURCES_ROUTE"

# JS Route
JS_ROUTE=$(cat <<EOF
{
  "name": "q-sign-js",
  "desc": "Q-SIGN Keycloak JavaScript Route",
  "uri": "/js/*",
  "methods": ["GET"],
  "priority": 70,
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "prometheus": {
      "prefer_name": true
    }
  },
  "status": 1
}
EOF
)

create_route "js" "JavaScript Route" "$JS_ROUTE"

# Health Check Route
HEALTH_ROUTE=$(cat <<EOF
{
  "name": "q-sign-health",
  "desc": "Q-SIGN Keycloak Health Check Route",
  "uri": "/health/*",
  "methods": ["GET"],
  "priority": 90,
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "prometheus": {
      "prefer_name": true
    }
  },
  "status": 1
}
EOF
)

create_route "health" "Health Check Route" "$HEALTH_ROUTE"

echo ""
echo "======================================================================"
echo "✅ APISIX PQC-realm 라우트 초기화 완료!"
echo "======================================================================"
echo ""
echo "생성된 라우트:"
echo "  - pqc-realm (PQC-realm Route) - Priority: 100"
echo "  - realms (All Realms Route) - Priority: 50"
echo "  - admin (Admin API Route) - Priority: 80"
echo "  - resources (Resources Route) - Priority: 70"
echo "  - js (JavaScript Route) - Priority: 70"
echo "  - health (Health Check Route) - Priority: 90"
echo ""
echo "생성된 Upstream:"
echo "  - q-sign-keycloak → ${Q_SIGN_HOST}"
echo ""
echo "======================================================================"
echo "테스트 방법"
echo "======================================================================"
echo ""
echo "1. APISIX 라우트 확인:"
echo "   curl -s $APISIX_ADMIN_URL/apisix/admin/routes -H 'X-API-KEY: $APISIX_API_KEY'"
echo ""
echo "2. PQC-realm 접근 테스트:"
echo "   curl http://192.168.0.11/realms/PQC-realm"
echo ""
echo "3. Q-APP 설정 업데이트:"
echo "   keycloakUrl: http://192.168.0.11  # APISIX를 통한 접근"
echo ""
