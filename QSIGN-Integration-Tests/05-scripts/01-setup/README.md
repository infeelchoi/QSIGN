# QSIGN 설정 스크립트

QSIGN 시스템의 초기 설정 및 구성을 자동화하는 스크립트 모음입니다.

## 📋 설정 스크립트 목록

| 스크립트 | 목적 | 실행 시점 |
|---------|------|----------|
| `fix-keycloak-frontend-url.sh` | Keycloak Frontend URL 수정 | Keycloak 배포 후 |

---

## 🔧 fix-keycloak-frontend-url.sh

### 개요
Keycloak의 Frontend URL을 QSIGN 통합에 맞게 자동으로 설정하는 스크립트입니다.

### 필요성
- QSIGN은 APISIX Gateway를 통해 Keycloak에 접근
- Frontend URL이 올바르게 설정되지 않으면 리다이렉션 오류 발생
- 이 스크립트는 자동으로 올바른 URL로 설정

### 사용법
```bash
cd /home/user/QSIGN/QSIGN-Integration-Tests/scripts/setup
./fix-keycloak-frontend-url.sh
```

### 실행 조건
- ✅ Keycloak이 실행 중
- ✅ Keycloak Admin API 접근 가능
- ✅ 네트워크 연결 정상

### 수행 작업
1. Keycloak Admin 토큰 획득
2. 현재 Frontend URL 확인
3. 새로운 Frontend URL로 업데이트
4. 설정 검증

### 예상 출력
```
==================================================
Keycloak Frontend URL Fix Script
==================================================

[1/4] Getting admin token...
✅ Token acquired

[2/4] Getting current frontend URL...
Current URL: http://keycloak.keycloak.svc.cluster.local:8080

[3/4] Updating frontend URL...
New URL: http://localhost:9180
✅ Frontend URL updated

[4/4] Verifying configuration...
✅ Configuration verified

==================================================
✅ Keycloak Frontend URL fix completed
==================================================
```

---

## 🚀 배포 시나리오별 사용법

### 시나리오 1: 새 환경 배포
```bash
# 1. Keycloak 배포
kubectl apply -f keycloak-deployment.yaml

# 2. Keycloak 준비 대기
kubectl wait --for=condition=ready pod -l app=keycloak -n keycloak --timeout=300s

# 3. Frontend URL 수정
./fix-keycloak-frontend-url.sh

# 4. 테스트
cd ../tests
./test-qsign-flow.sh
```

### 시나리오 2: 설정 초기화
```bash
# Keycloak 설정이 꼬였을 때
./fix-keycloak-frontend-url.sh

# 검증
curl http://localhost:9180/realms/qsign/.well-known/openid-configuration
```

### 시나리오 3: 포트 변경 후
```bash
# APISIX 포트가 변경되었을 때
export NEW_PORT=9280
sed -i "s/9180/$NEW_PORT/g" fix-keycloak-frontend-url.sh
./fix-keycloak-frontend-url.sh
```

---

## ⚙️ 환경 변수

스크립트 동작을 커스터마이징할 수 있는 환경 변수:

```bash
# Keycloak 접속 정보
export KEYCLOAK_URL="http://localhost:9180"
export KEYCLOAK_ADMIN_USER="admin"
export KEYCLOAK_ADMIN_PASSWORD="admin"

# 대상 Realm
export KEYCLOAK_REALM="qsign"

# Frontend URL (자동 감지되지 않을 때)
export FRONTEND_URL="http://localhost:9180"

# 디버그 모드
export DEBUG=1
```

### 사용 예제
```bash
# 커스텀 Keycloak URL로 실행
KEYCLOAK_URL="http://keycloak.example.com" ./fix-keycloak-frontend-url.sh

# 디버그 모드로 실행
DEBUG=1 ./fix-keycloak-frontend-url.sh
```

---

## 🔍 스크립트 내부 동작

### 1단계: 인증
```bash
# Admin CLI를 통한 토큰 획득
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$KEYCLOAK_ADMIN_USER" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | jq -r '.access_token')
```

### 2단계: 현재 설정 확인
```bash
# Realm 설정 조회
CURRENT_CONFIG=$(curl -s -X GET \
  "$KEYCLOAK_URL/admin/realms/$KEYCLOAK_REALM" \
  -H "Authorization: Bearer $TOKEN")

CURRENT_FRONTEND_URL=$(echo $CURRENT_CONFIG | jq -r '.attributes.frontendUrl')
```

### 3단계: 설정 업데이트
```bash
# Frontend URL 업데이트
curl -X PUT "$KEYCLOAK_URL/admin/realms/$KEYCLOAK_REALM" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"attributes\": {
      \"frontendUrl\": \"$NEW_FRONTEND_URL\"
    }
  }"
```

### 4단계: 검증
```bash
# 설정 재조회하여 확인
UPDATED_CONFIG=$(curl -s -X GET \
  "$KEYCLOAK_URL/admin/realms/$KEYCLOAK_REALM" \
  -H "Authorization: Bearer $TOKEN")

UPDATED_FRONTEND_URL=$(echo $UPDATED_CONFIG | jq -r '.attributes.frontendUrl')

if [ "$UPDATED_FRONTEND_URL" == "$NEW_FRONTEND_URL" ]; then
  echo "✅ Verification successful"
fi
```

---

## 🐛 문제 해결

### 오류: "Connection refused"
```bash
# Keycloak 상태 확인
kubectl get pods -n keycloak

# 포트 포워딩 확인
kubectl get svc -n keycloak

# 직접 접속 테스트
curl http://localhost:9180/health
```

**해결 방법:**
- Keycloak Pod가 Running 상태인지 확인
- 포트 포워딩이 올바른지 확인
- 방화벽 규칙 확인

---

### 오류: "Unauthorized"
```bash
# Admin 계정 확인
kubectl get secret -n keycloak keycloak-admin-credentials -o yaml
```

**해결 방법:**
- Admin 사용자명/비밀번호 확인
- Keycloak 초기 설정 확인
- Admin CLI 클라이언트 활성화 확인

---

### 오류: "Realm not found"
```bash
# Realm 목록 확인
curl -s "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  | jq -r '.[].realm'
```

**해결 방법:**
- qsign Realm이 생성되었는지 확인
- Realm 이름 스펠링 확인
- Keycloak 초기 설정 스크립트 재실행

---

### 오류: "jq: command not found"
```bash
# jq 설치
sudo apt-get update
sudo apt-get install jq

# 또는 yum
sudo yum install jq
```

---

## 📊 스크립트 검증

### 수동 검증
스크립트 실행 후 수동으로 확인:

```bash
# 1. Well-known 엔드포인트 확인
curl http://localhost:9180/realms/qsign/.well-known/openid-configuration | jq

# 2. Authorization endpoint 확인
# 출력에서 "authorization_endpoint" 값 확인
# 예상값: "http://localhost:9180/realms/qsign/protocol/openid-connect/auth"

# 3. Token endpoint 확인
curl -X POST "http://localhost:9180/realms/qsign/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

### 자동 검증
```bash
# 통합 테스트 실행
cd ../tests
./test-qsign-flow.sh
```

---

## 🔄 롤백

만약 스크립트 실행 후 문제가 생기면:

### 1. 이전 설정으로 복구
```bash
# 이전 Frontend URL로 복구 (스크립트가 백업해둔 값 사용)
KEYCLOAK_URL="http://localhost:9180"
OLD_FRONTEND_URL="http://keycloak.keycloak.svc.cluster.local:8080"

# Admin 토큰 획득
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  | jq -r '.access_token')

# 롤백
curl -X PUT "$KEYCLOAK_URL/admin/realms/qsign" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"attributes\": {\"frontendUrl\": \"$OLD_FRONTEND_URL\"}}"
```

### 2. Keycloak 재시작
```bash
kubectl rollout restart deployment/keycloak -n keycloak
kubectl rollout status deployment/keycloak -n keycloak
```

---

## 📝 로그 및 디버깅

### 디버그 모드 활성화
```bash
# 스크립트 수정
set -x  # 각 명령어 출력
set -e  # 오류 시 중단
set -u  # 미정의 변수 사용 시 오류
```

### 로그 저장
```bash
# 출력을 파일로 저장
./fix-keycloak-frontend-url.sh 2>&1 | tee keycloak-fix-$(date +%Y%m%d-%H%M%S).log
```

### 상세 출력
```bash
# curl 상세 출력
export CURL_VERBOSE="-v"

# 또는 스크립트 내 curl 명령에 -v 추가
curl -v -X POST ...
```

---

## 🔗 관련 리소스

- [Keycloak Admin REST API 문서](https://www.keycloak.org/docs-api/latest/rest-api/)
- [테스트 스크립트](../tests/)
- [트러블슈팅 가이드](../../docs/troubleshooting/)
- [QSIGN 아키텍처](../../docs/)

---

## 📋 체크리스트

스크립트 실행 전:
- [ ] Keycloak이 실행 중
- [ ] Admin 계정 정보 확인
- [ ] jq가 설치됨
- [ ] curl이 설치됨
- [ ] 네트워크 연결 정상

스크립트 실행 후:
- [ ] 출력에 에러 없음
- [ ] "✅ Configuration verified" 메시지 확인
- [ ] Well-known 엔드포인트 확인
- [ ] 통합 테스트 실행
- [ ] 로그 저장

---

## 🚀 다음 단계

스크립트 실행 완료 후:

1. **테스트 실행**
   ```bash
   cd ../tests
   ./test-qsign-flow.sh
   ```

2. **전체 시스템 검증**
   ```bash
   ./test-full-qsign-flow.sh
   ```

3. **문서화**
   - 실행 결과를 `../../docs/results/`에 기록
   - 문제가 있었다면 `../../docs/troubleshooting/`에 추가

---

**업데이트**: 2025-11-17
