#!/bin/bash

echo "App4를 RS256으로 설정"
echo "===================="

# Admin token 획득
TOKEN=$(curl -s -X POST "http://192.168.0.11:30181/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" | \
  python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

if [ -z "$TOKEN" ]; then
  echo "❌ Token 획득 실패"
  exit 1
fi

echo "✅ Admin token 획득 완료"
echo ""

# 클라이언트 정보 조회
curl -s -X GET "http://192.168.0.11:30181/admin/realms/PQC-realm/clients" \
  -H "Authorization: Bearer $TOKEN" > /tmp/app4_client_update.json

# App4 클라이언트 업데이트
python3 <<'EOF'
import json
import subprocess
import os

with open('/tmp/app4_client_update.json') as f:
    clients = json.load(f)

token = os.environ.get('TOKEN', '')

for client in clients:
    if client.get('clientId') == 'app4-client':
        client_id = client.get('id')
        redirect_uris = client.get('redirectUris', [])

        print(f"App4 Client ID: {client_id}")
        print(f"현재 Redirect URIs: {redirect_uris}")

        # 클라이언트 속성 업데이트
        if 'attributes' not in client:
            client['attributes'] = {}

        # RS256으로 서명 알고리즘 설정
        client['attributes']['access.token.signed.response.alg'] = 'RS256'
        client['attributes']['id.token.signed.response.alg'] = 'RS256'
        client['attributes']['user.info.response.signature.alg'] = 'RS256'

        print(f"\n✅ 서명 알고리즘을 RS256으로 설정")

        # 로그아웃 URI도 함께 설정
        logout_uris = []
        for uri in redirect_uris:
            base_uri = uri.rstrip('/')
            if '/callback' in base_uri:
                base_uri = base_uri.replace('/callback', '')
            logout_uris.append(base_uri)
            logout_uris.append(base_uri + '/*')

        post_logout_uris = '##'.join(logout_uris)
        client['attributes']['post.logout.redirect.uris'] = post_logout_uris

        print(f"✅ Post Logout URIs 설정: {len(logout_uris)}개 URI")

        # JSON 저장
        with open('/tmp/app4_client_rs256.json', 'w') as f:
            json.dump(client, f, indent=2)

        # Keycloak에 적용
        result = subprocess.run([
            'curl', '-s', '-X', 'PUT',
            f'http://192.168.0.11:30181/admin/realms/PQC-realm/clients/{client_id}',
            '-H', f'Authorization: Bearer {token}',
            '-H', 'Content-Type: application/json',
            '-d', '@/tmp/app4_client_rs256.json'
        ], capture_output=True, text=True, env={'TOKEN': token})

        if result.returncode == 0:
            print(f"\n✅ App4 클라이언트 업데이트 완료!")
            print(f"\n설정 내용:")
            print(f"  - Access Token Signature: RS256")
            print(f"  - ID Token Signature: RS256")
            print(f"  - UserInfo Signature: RS256")
        else:
            print(f"\n❌ 업데이트 실패: {result.stderr}")

        break
EOF
