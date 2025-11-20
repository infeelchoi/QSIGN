#!/bin/bash
# APISIX PQC-realm 라우트 설정 (Port 30080)
# Q-APP → APISIX (30080) → Q-SIGN (30181) → Q-KMS (8200)

set -e

# APISIX Pod 내부의 Admin API 사용
APISIX_ADMIN_URL="http://192.168.0.11:30080/apisix/admin"
APISIX_API_KEY="edd1c9f034335f136f87ad84b625c8f1"
Q_SIGN_HOST="192.168.0.11:30181"

echo "========================================="
echo "APISIX PQC-realm 라우트 설정 (Port 30080)"
echo "========================================="
echo ""
echo "Architecture:"
echo "  Q-APP → APISIX (30080) → Q-SIGN (30181) → Q-KMS (8200)"
echo ""

# Upstream 생성
echo "Step 1: Upstream 생성..."
curl -s -X PUT "$APISIX_ADMIN_URL/upstreams/q-sign-keycloak" \
  -H "X-API-KEY: $APISIX_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "q-sign-keycloak",
    "desc": "Q-SIGN Keycloak PQC Backend",
    "type": "roundrobin",
    "scheme": "http",
    "nodes": {
      "192.168.0.11:30181": 1
    },
    "timeout": {
      "connect": 10,
      "send": 10,
      "read": 60
    }
  }' | python3 -c "import sys,json; print('✓ Upstream 생성:', json.load(sys.stdin).get('key', 'OK'))" 2>/dev/null || echo "⚠ Upstream 설정 실패"

echo ""

# PQC-realm Route
echo "Step 2: PQC-realm 라우트 생성..."
curl -s -X PUT "$APISIX_ADMIN_URL/routes/pqc-realm" \
  -H "X-API-KEY: $APISIX_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "q-sign-pqc-realm",
    "uri": "/realms/PQC-realm/*",
    "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    "upstream_id": "q-sign-keycloak",
    "plugins": {
      "cors": {
        "allow_origins": "http://192.168.0.11:30300",
        "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
        "allow_headers": "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization",
        "allow_credential": true
      },
      "limit-req": {
        "rate": 100,
        "burst": 50,
        "key": "remote_addr"
      }
    }
  }' | python3 -c "import sys,json; print('✓ PQC-realm 라우트:', json.load(sys.stdin).get('key', 'OK'))" 2>/dev/null || echo "⚠ PQC-realm 라우트 실패"

echo ""

# All Realms Route
echo "Step 3: Realms 라우트 생성..."
curl -s -X PUT "$APISIX_ADMIN_URL/routes/realms" \
  -H "X-API-KEY: $APISIX_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "q-sign-realms",
    "uri": "/realms/*",
    "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    "upstream_id": "q-sign-keycloak",
    "plugins": {
      "cors": {
        "allow_origins": "http://192.168.0.11:30300",
        "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
        "allow_headers": "*",
        "allow_credential": true
      }
    }
  }' | python3 -c "import sys,json; print('✓ Realms 라우트:', json.load(sys.stdin).get('key', 'OK'))" 2>/dev/null || echo "⚠ Realms 라우트 실패"

echo ""

# Admin Route
echo "Step 4: Admin 라우트 생성..."
curl -s -X PUT "$APISIX_ADMIN_URL/routes/admin" \
  -H "X-API-KEY: $APISIX_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "q-sign-admin",
    "uri": "/admin/*",
    "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    "upstream_id": "q-sign-keycloak",
    "plugins": {
      "cors": {
        "allow_origins": "*",
        "allow_methods": "GET,POST,PUT,DELETE,OPTIONS",
        "allow_headers": "*",
        "allow_credential": true
      }
    }
  }' | python3 -c "import sys,json; print('✓ Admin 라우트:', json.load(sys.stdin).get('key', 'OK'))" 2>/dev/null || echo "⚠ Admin 라우트 실패"

echo ""
echo "========================================="
echo "라우트 설정 완료!"
echo "========================================="
echo ""
echo "테스트:"
echo "  curl http://192.168.0.11:30080/realms/PQC-realm"
echo ""