#!/bin/bash
# APISIX Gateway 라우팅 설정 스크립트
# Q-APP → Q-GATEWAY (APISIX) → Q-SIGN (Keycloak) 흐름 구성

set -e

APISIX_ADMIN_API="http://192.168.0.11:9180/apisix/admin"
APISIX_API_KEY="edd1c9f034335f136f87ad84b625c8f1"
Q_SIGN_UPSTREAM="192.168.0.11:30181"
Q_APP_ORIGIN="http://192.168.0.11:30300"

echo "========================================="
echo "APISIX Gateway 라우팅 설정"
echo "========================================="
echo ""
echo "Architecture:"
echo "  Q-APP (30300) → APISIX (80) → Q-SIGN (30181) → Vault (8200)"
echo ""

# 1. Q-SIGN Upstream 생성
echo "Step 1: Creating Q-SIGN upstream..."
UPSTREAM_DATA=$(cat <<EOF
{
  "name": "q-sign-keycloak",
  "desc": "Q-SIGN Keycloak PQC Backend",
  "type": "roundrobin",
  "scheme": "http",
  "nodes": {
    "${Q_SIGN_UPSTREAM}": 1
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
  "retries": 2,
  "checks": {
    "active": {
      "type": "http",
      "http_path": "/health/ready",
      "healthy": {
        "interval": 10,
        "successes": 2
      },
      "unhealthy": {
        "interval": 5,
        "http_failures": 3
      }
    }
  }
}
EOF
)

UPSTREAM_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${APISIX_ADMIN_API}/upstreams/q-sign-keycloak" \
  -H "X-API-KEY: ${APISIX_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "$UPSTREAM_DATA")

HTTP_CODE=$(echo "$UPSTREAM_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✓ Upstream created successfully"
else
    echo "⚠ Unexpected response: $HTTP_CODE"
    echo "$UPSTREAM_RESPONSE"
fi
echo ""

# 2. PQC-realm 라우트 생성 (SSO 인증)
echo "Step 2: Creating PQC-realm route..."
ROUTE_PQC_DATA=$(cat <<EOF
{
  "name": "q-sign-pqc-realm",
  "desc": "Q-SIGN PQC-realm Authentication Route",
  "uri": "/realms/PQC-realm/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  "upstream_id": "q-sign-keycloak",
  "plugins": {
    "cors": {
      "allow_origins": "${Q_APP_ORIGIN}",
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

ROUTE_PQC_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${APISIX_ADMIN_API}/routes/q-sign-pqc-realm" \
  -H "X-API-KEY: ${APISIX_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "$ROUTE_PQC_DATA")

HTTP_CODE=$(echo "$ROUTE_PQC_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✓ PQC-realm route created successfully"
else
    echo "⚠ Unexpected response: $HTTP_CODE"
fi
echo ""

# 3. Admin API 라우트 생성
echo "Step 3: Creating Keycloak Admin API route..."
ROUTE_ADMIN_DATA=$(cat <<EOF
{
  "name": "q-sign-admin",
  "desc": "Q-SIGN Keycloak Admin API Route",
  "uri": "/admin/*",
  "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
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

ROUTE_ADMIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${APISIX_ADMIN_API}/routes/q-sign-admin" \
  -H "X-API-KEY: ${APISIX_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "$ROUTE_ADMIN_DATA")

HTTP_CODE=$(echo "$ROUTE_ADMIN_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✓ Admin API route created successfully"
else
    echo "⚠ Unexpected response: $HTTP_CODE"
fi
echo ""

# 4. Health Check 라우트
echo "Step 4: Creating health check route..."
ROUTE_HEALTH_DATA=$(cat <<EOF
{
  "name": "q-sign-health",
  "desc": "Q-SIGN Keycloak Health Check Route",
  "uri": "/health/*",
  "methods": ["GET"],
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

ROUTE_HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${APISIX_ADMIN_API}/routes/q-sign-health" \
  -H "X-API-KEY: ${APISIX_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "$ROUTE_HEALTH_DATA")

HTTP_CODE=$(echo "$ROUTE_HEALTH_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✓ Health check route created successfully"
else
    echo "⚠ Unexpected response: $HTTP_CODE"
fi
echo ""

# 5. 설정 검증
echo "Step 5: Verifying APISIX configuration..."
sleep 2

echo ""
echo "Upstream Status:"
curl -s "${APISIX_ADMIN_API}/upstreams/q-sign-keycloak" \
  -H "X-API-KEY: ${APISIX_API_KEY}" | \
  python3 -c "import sys,json; d=json.load(sys.stdin); print('  Name:', d.get('value', {}).get('name')); print('  Nodes:', d.get('value', {}).get('nodes'))" 2>/dev/null || echo "  Unable to parse"

echo ""
echo "Routes Status:"
curl -s "${APISIX_ADMIN_API}/routes" \
  -H "X-API-KEY: ${APISIX_API_KEY}" | \
  python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    routes = data.get('list', [])
    for route in routes:
        value = route.get('value', {})
        if 'q-sign' in value.get('name', ''):
            print(f\"  - {value.get('name')}: {value.get('uri')} [{value.get('status')}]\")
except:
    print('  Unable to parse routes')
" 2>/dev/null

echo ""
echo "========================================="
echo "Complete"
echo "========================================="
echo ""
echo "✅ APISIX Gateway 설정 완료!"
echo ""
echo "접근 경로:"
echo "  Direct:  http://192.168.0.11:30181/realms/PQC-realm"
echo "  Gateway: http://192.168.0.11/realms/PQC-realm (APISIX를 통한 접근)"
echo ""
echo "다음 단계:"
echo "1. Q-APP values.yaml 업데이트 (keycloakUrl: http://192.168.0.11)"
echo "2. ArgoCD Sync"
echo "3. Gateway를 통한 SSO 로그인 테스트"
echo ""
