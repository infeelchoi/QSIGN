#!/bin/bash
# Q-GATEWAY Nginx 리버스 프록시 설정
# Q-APP → Q-GATEWAY (nginx on port 8888) → Q-SIGN → Q-KMS

set -e

echo "========================================="
echo "Q-GATEWAY 리버스 프록시 설정"
echo "========================================="
echo ""
echo "Architecture:"
echo "  Q-APP (30300) → Q-GATEWAY (8888) → Q-SIGN (30181) → Q-KMS (8200)"
echo ""

# Nginx 설정 파일 생성
NGINX_CONF="/tmp/q-gateway-nginx.conf"

cat > "$NGINX_CONF" <<'EOF'
# Q-GATEWAY Nginx Configuration
# Reverse Proxy: Q-APP → Q-SIGN

user nginx;
worker_processes auto;
error_log /var/log/nginx/q-gateway-error.log warn;
pid /var/run/nginx-q-gateway.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/q-gateway-access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Upstream: Q-SIGN Keycloak
    upstream q-sign-keycloak {
        server 192.168.0.11:30181;
        keepalive 32;
    }

    # Q-GATEWAY Server
    server {
        listen 8888;
        server_name q-gateway.local 192.168.0.11;

        # CORS Headers
        add_header 'Access-Control-Allow-Origin' 'http://192.168.0.11:30300' always;
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization' always;
        add_header 'Access-Control-Allow-Credentials' 'true' always;

        # OPTIONS method for CORS preflight
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' 'http://192.168.0.11:30300';
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
            add_header 'Access-Control-Allow-Headers' 'DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization';
            add_header 'Access-Control-Max-Age' 1728000;
            add_header 'Content-Type' 'text/plain; charset=utf-8';
            add_header 'Content-Length' 0;
            return 204;
        }

        # Proxy to Q-SIGN Keycloak
        location / {
            proxy_pass http://q-sign-keycloak;
            proxy_http_version 1.1;

            # Proxy Headers
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_set_header X-Forwarded-Host $host;
            proxy_set_header X-Forwarded-Port $server_port;

            # Connection headers
            proxy_set_header Connection "";
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";

            # Timeouts
            proxy_connect_timeout 10s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;

            # Buffering
            proxy_buffering on;
            proxy_buffer_size 4k;
            proxy_buffers 8 4k;
            proxy_busy_buffers_size 8k;

            # Redirect handling
            proxy_redirect http://192.168.0.11:30181/ http://192.168.0.11:8888/;
        }

        # Health check endpoint
        location /health {
            access_log off;
            return 200 "Q-GATEWAY Healthy\n";
            add_header Content-Type text/plain;
        }

        # Metrics endpoint (optional)
        location /metrics {
            access_log off;
            stub_status on;
        }
    }
}
EOF

echo "✓ Nginx 설정 파일 생성: $NGINX_CONF"
echo ""

# Nginx 설정 테스트
echo "Step 1: Nginx 설정 검증..."
if command -v nginx &> /dev/null; then
    if nginx -t -c "$NGINX_CONF" 2>&1 | grep -q "successful"; then
        echo "✓ Nginx 설정 검증 성공"
    else
        echo "⚠ Nginx 설정 검증 실패"
        nginx -t -c "$NGINX_CONF"
    fi
else
    echo "⚠ Nginx가 설치되어 있지 않습니다"
    echo "  설치 방법: sudo apt-get install nginx"
fi
echo ""

# Docker를 사용한 Q-GATEWAY 실행 (권장)
echo "========================================="
echo "Q-GATEWAY 시작 방법"
echo "========================================="
echo ""

echo "Option 1: Docker를 사용한 실행 (권장)"
echo ""
echo "docker run -d \\"
echo "  --name q-gateway \\"
echo "  -p 8888:8888 \\"
echo "  -v $NGINX_CONF:/etc/nginx/nginx.conf:ro \\"
echo "  --restart unless-stopped \\"
echo "  nginx:alpine"
echo ""

echo "Option 2: Systemd 서비스로 실행"
echo ""
echo "sudo nginx -c $NGINX_CONF"
echo ""

echo "Option 3: K3s에 배포"
echo ""
echo "kubectl create configmap q-gateway-config --from-file=nginx.conf=$NGINX_CONF -n qsign-prod"
echo "kubectl create -f q-gateway-deployment.yaml"
echo ""

echo "========================================="
echo "테스트 방법"
echo "========================================="
echo ""

echo "1. Q-GATEWAY 상태 확인:"
echo "   curl http://192.168.0.11:8888/health"
echo ""

echo "2. PQC-realm 접근 테스트:"
echo "   curl http://192.168.0.11:8888/realms/PQC-realm"
echo ""

echo "3. Q-APP 설정 업데이트:"
echo "   keycloakUrl: http://192.168.0.11:8888"
echo ""

echo "설정 파일 위치: $NGINX_CONF"
echo ""
