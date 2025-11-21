#!/bin/bash
# App3 로그아웃 URI 수정 스크립트
# 작성일: 2025-11-21
# 설명: App3 클라이언트의 Post Logout Redirect URIs를 올바르게 설정

echo "App3 로그아웃 URI 수정"
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

# 클라이언트 정보 조회
curl -s -X GET "http://192.168.0.11:30181/admin/realms/PQC-realm/clients" \
  -H "Authorization: Bearer $TOKEN" > /tmp/app3_client_fix.json

# App3 클라이언트 업데이트
python3 <<EOF
import json
import subprocess
import os

with open('/tmp/app3_client_fix.json') as f:
    clients = json.load(f)

token = os.environ.get('TOKEN', '')

for client in clients:
    if client.get('clientId') == 'app3-client':
        client_id = client.get('id')
        redirect_uris = client.get('redirectUris', [])

        print(f"App3 Client ID: {client_id}")
        print(f"현재 Redirect URIs: {redirect_uris}")

        # Redirect URIs에서 /callback을 제거하고 루트 경로 추가
        logout_uris = []
        for uri in redirect_uris:
            # /callback 제거하고 루트 경로 추가
            base_uri = uri.replace('/callback', '')
            logout_uris.append(base_uri)
            # 와일드카드도 추가
            logout_uris.append(base_uri + '/*')

        # ## 구분자로 결합 (Keycloak 표준)
        post_logout_uris = '##'.join(logout_uris)

        print(f"\n설정할 Post Logout URIs:")
        for i, uri in enumerate(logout_uris, 1):
            print(f"  {i}. {uri}")

        # 클라이언트 업데이트
        if 'attributes' not in client:
            client['attributes'] = {}

        client['attributes']['post.logout.redirect.uris'] = post_logout_uris

        # JSON 저장
        with open('/tmp/app3_client_updated.json', 'w') as f:
            json.dump(client, f)

        # Keycloak에 적용
        result = subprocess.run([
            'curl', '-s', '-X', 'PUT',
            f'http://192.168.0.11:30181/admin/realms/PQC-realm/clients/{client_id}',
            '-H', f'Authorization: Bearer {token}',
            '-H', 'Content-Type: application/json',
            '-d', '@/tmp/app3_client_updated.json'
        ], capture_output=True, text=True, env={'TOKEN': token})

        if result.returncode == 0:
            print(f"\n✅ App3 로그아웃 URI 업데이트 완료!")
            print(f"\n설정된 Post Logout URIs:")
            print(f"{post_logout_uris}")
        else:
            print(f"\n❌ 업데이트 실패: {result.stderr}")

        break
EOF
