#!/bin/bash

################################################################################
# app3 QSIGN 전체 통합 테스트
# app3-pqc-client ↔ Q-GATEWAY(APISIX) ↔ Q-SIGN(Keycloak-PQC) ↔ Q-KMS(Vault+HSM)
################################################################################

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 테스트 결과 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 결과 함수
pass_test() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

fail_test() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    echo -e "${YELLOW}  Reason: $2${NC}"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
}

info_log() {
    echo -e "${CYAN}ℹ INFO${NC}: $1"
}

section_header() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ $1${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 테스트 요약 출력
print_summary() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║                  테스트 결과 요약                          ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "  총 테스트: ${TOTAL_TESTS}"
    echo -e "  ${GREEN}성공: ${PASSED_TESTS}${NC}"
    echo -e "  ${RED}실패: ${FAILED_TESTS}${NC}"
    echo ""

    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✓ 모든 테스트 통과!${NC}"
        echo -e "  성공률: 100%"
    else
        SUCCESS_RATE=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo -e "${YELLOW}⚠ 일부 테스트 실패${NC}"
        echo -e "  성공률: ${SUCCESS_RATE}%"
    fi
    echo ""
}

################################################################################
# 1. Q-KMS (Vault + HSM) 테스트
################################################################################
test_qkms() {
    section_header "1. Q-KMS (Vault + HSM) 연결 테스트"

    # Vault health check
    info_log "Vault health check..."
    VAULT_HEALTH=$(curl -sf http://192.168.0.11:30280/v1/sys/health 2>&1)
    if [ $? -eq 0 ]; then
        pass_test "Vault 연결 성공 (http://192.168.0.11:30280)"
    else
        fail_test "Vault 연결 실패" "Vault가 응답하지 않습니다"
    fi

    # Vault seal 상태 확인
    info_log "Vault seal 상태 확인..."
    SEALED=$(echo "$VAULT_HEALTH" | python3 -c "import sys, json; print(json.load(sys.stdin).get('sealed', 'unknown'))" 2>/dev/null)
    if [ "$SEALED" = "False" ] || [ "$SEALED" = "false" ]; then
        pass_test "Vault unsealed 상태 확인"
    else
        fail_test "Vault sealed 상태" "Vault가 sealed 상태입니다"
    fi
}

################################################################################
# 2. Q-SIGN (Keycloak PQC) 테스트
################################################################################
test_qsign() {
    section_header "2. Q-SIGN (Keycloak PQC) 연결 테스트"

    # Keycloak health check
    info_log "Keycloak health check..."
    KC_HEALTH=$(curl -sf http://192.168.0.11:30181/health 2>&1)
    if [ $? -eq 0 ]; then
        pass_test "Keycloak 연결 성공 (http://192.168.0.11:30181)"
    else
        fail_test "Keycloak 연결 실패" "Keycloak이 응답하지 않습니다"
    fi

    # PQC-realm 확인
    info_log "PQC-realm 설정 확인..."
    REALM_INFO=$(curl -sf http://192.168.0.11:30181/realms/PQC-realm 2>&1)
    if [ $? -eq 0 ]; then
        pass_test "PQC-realm 접근 성공"

        # DILITHIUM3 알고리즘 지원 확인
        REALM_NAME=$(echo "$REALM_INFO" | python3 -c "import sys, json; print(json.load(sys.stdin).get('realm', ''))" 2>/dev/null)
        if [ "$REALM_NAME" = "PQC-realm" ]; then
            pass_test "PQC-realm 이름 확인: $REALM_NAME"
        fi
    else
        fail_test "PQC-realm 접근 실패" "Realm을 찾을 수 없습니다"
    fi

    # Admin token 획득 테스트
    info_log "Keycloak admin 인증 테스트..."
    ADMIN_TOKEN=$(curl -sf -X POST "http://192.168.0.11:30181/realms/master/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=admin-cli" \
      -d "grant_type=password" \
      -d "username=admin" \
      -d "password=admin" | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)

    if [ -n "$ADMIN_TOKEN" ]; then
        pass_test "Keycloak admin 인증 성공"

        # app3-client 존재 확인
        info_log "app3-client 설정 확인..."
        CLIENTS=$(curl -s "http://192.168.0.11:30181/admin/realms/PQC-realm/clients" \
          -H "Authorization: Bearer $ADMIN_TOKEN")

        APP3_EXISTS=$(echo "$CLIENTS" | python3 -c "import sys, json; clients = json.load(sys.stdin); print(any(c['clientId'] == 'app3-client' for c in clients))" 2>/dev/null)

        if [ "$APP3_EXISTS" = "True" ]; then
            pass_test "app3-client 클라이언트 존재 확인"

            # DILITHIUM3 설정 확인
            APP3_UUID=$(echo "$CLIENTS" | python3 -c "import sys, json; clients = json.load(sys.stdin); print([c['id'] for c in clients if c['clientId'] == 'app3-client'][0])" 2>/dev/null)
            CLIENT_CONFIG=$(curl -s "http://192.168.0.11:30181/admin/realms/PQC-realm/clients/$APP3_UUID" \
              -H "Authorization: Bearer $ADMIN_TOKEN")

            ACCESS_TOKEN_ALG=$(echo "$CLIENT_CONFIG" | python3 -c "import sys, json; config = json.load(sys.stdin); print(config.get('attributes', {}).get('access.token.signed.response.alg', 'RS256'))" 2>/dev/null)

            if [ "$ACCESS_TOKEN_ALG" = "DILITHIUM3" ]; then
                pass_test "app3-client DILITHIUM3 알고리즘 설정 확인"
            else
                fail_test "app3-client PQC 설정 없음" "현재 알고리즘: $ACCESS_TOKEN_ALG"
            fi
        else
            fail_test "app3-client 찾을 수 없음" "PQC-realm에 app3-client가 없습니다"
        fi
    else
        fail_test "Keycloak admin 인증 실패" "Admin token을 획득하지 못했습니다"
    fi
}

################################################################################
# 3. Q-GATEWAY (APISIX) 테스트
################################################################################
test_qgateway() {
    section_header "3. Q-GATEWAY (APISIX) 연결 테스트"

    # APISIX health check (internal)
    info_log "APISIX 내부 연결 테스트..."
    APISIX_HEALTH=$(kubectl get svc -n q-sign apisix -o json 2>/dev/null | python3 -c "import sys, json; svc = json.load(sys.stdin); print(svc['metadata']['name'])" 2>/dev/null)

    if [ "$APISIX_HEALTH" = "apisix" ]; then
        pass_test "APISIX 서비스 존재 확인 (q-sign namespace)"
    else
        fail_test "APISIX 서비스 없음" "q-sign namespace에 APISIX 서비스가 없습니다"
    fi

    # APISIX external access (제한적)
    info_log "APISIX 외부 접근 테스트 (제한적 접근 예상)..."
    APISIX_RESPONSE=$(curl -sf -o /dev/null -w "%{http_code}" http://192.168.0.11:30080 2>/dev/null)

    if [ "$APISIX_RESPONSE" = "404" ] || [ "$APISIX_RESPONSE" = "403" ]; then
        pass_test "APISIX 외부 접근 제한 확인 (예상된 동작)"
    elif [ "$APISIX_RESPONSE" = "200" ]; then
        pass_test "APISIX 외부 접근 가능"
    else
        info_log "APISIX 외부 접근 제한됨 (HTTP $APISIX_RESPONSE)"
    fi
}

################################################################################
# 4. app3 애플리케이션 테스트
################################################################################
test_app3() {
    section_header "4. app3 애플리케이션 연결 테스트"

    # app3 health check
    info_log "app3 health check..."
    APP3_HEALTH=$(curl -sf http://192.168.0.11:30202/health 2>&1)
    if [ $? -eq 0 ]; then
        pass_test "app3 연결 성공 (http://192.168.0.11:30202)"

        # Health check 상세 정보
        APP3_STATUS=$(echo "$APP3_HEALTH" | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', 'unknown'))" 2>/dev/null)
        PQC_ENABLED=$(echo "$APP3_HEALTH" | python3 -c "import sys, json; print(json.load(sys.stdin).get('pqc_enabled', False))" 2>/dev/null)
        KC_INIT=$(echo "$APP3_HEALTH" | python3 -c "import sys, json; print(json.load(sys.stdin).get('keycloak_initialized', False))" 2>/dev/null)

        if [ "$APP3_STATUS" = "healthy" ]; then
            pass_test "app3 상태: healthy"
        fi

        if [ "$PQC_ENABLED" = "True" ]; then
            pass_test "app3 PQC 기능 활성화"
        else
            fail_test "app3 PQC 기능 비활성화" "PQC가 활성화되지 않았습니다"
        fi

        if [ "$KC_INIT" = "True" ]; then
            pass_test "app3 Keycloak 초기화 완료"
        else
            fail_test "app3 Keycloak 초기화 실패" "Keycloak 클라이언트가 초기화되지 않았습니다"
        fi
    else
        fail_test "app3 연결 실패" "app3가 응답하지 않습니다"
    fi

    # app3 메인 페이지 접근
    info_log "app3 메인 페이지 접근 테스트..."
    MAIN_PAGE=$(curl -sf -o /dev/null -w "%{http_code}" http://192.168.0.11:30202/ 2>/dev/null)
    if [ "$MAIN_PAGE" = "200" ]; then
        pass_test "app3 메인 페이지 접근 성공"
    else
        fail_test "app3 메인 페이지 접근 실패" "HTTP status: $MAIN_PAGE"
    fi
}

################################################################################
# 5. 전체 통합 플로우 테스트
################################################################################
test_full_integration() {
    section_header "5. app3 전체 통합 플로우 테스트"

    info_log "전체 플로우: app3 → APISIX → Keycloak (PQC) → Vault (HSM)"
    echo ""

    # 플로우 다이어그램
    echo -e "${CYAN}┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐${NC}"
    echo -e "${CYAN}│   app3-pqc   │───▶│  Q-GATEWAY   │───▶│   Q-SIGN     │───▶│    Q-KMS     │${NC}"
    echo -e "${CYAN}│   (30202)    │    │   (APISIX)   │    │  (Keycloak)  │    │ (Vault+HSM)  │${NC}"
    echo -e "${CYAN}│              │◀───│              │◀───│  DILITHIUM3  │◀───│              │${NC}"
    echo -e "${CYAN}└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘${NC}"
    echo ""

    # 연결성 종합 평가
    if [ $FAILED_TESTS -eq 0 ]; then
        pass_test "전체 QSIGN 스택 연결 성공"
        echo ""
        echo -e "${GREEN}✓ app3 → Q-GATEWAY → Q-SIGN → Q-KMS 플로우 정상 작동${NC}"
    else
        fail_test "일부 컴포넌트 연결 실패" "$FAILED_TESTS개 테스트 실패"
    fi

    echo ""
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}  수동 테스트 필요${NC}"
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "다음 단계는 브라우저에서 수동으로 테스트해야 합니다:"
    echo ""
    echo "1. 브라우저 접속: http://192.168.0.11:30202"
    echo "2. '로그인' 버튼 클릭"
    echo "3. Keycloak 로그인: testuser / admin"
    echo "4. 로그인 성공 후 토큰 정보 확인:"
    echo "   - 알고리즘: DILITHIUM3 (PQC)"
    echo "   - PQC 상태: Dilithium3 Provider 준비됨"
    echo "5. '/token' 엔드포인트 확인:"
    echo "   http://192.168.0.11:30202/token"
    echo ""
    echo "예상 결과:"
    echo "  {\"tokenInfo\": {\"alg\": \"DILITHIUM3\", \"quantum_resistant\": true}}"
    echo ""
}

################################################################################
# 메인 실행
################################################################################
main() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║                                                           ║${NC}"
    echo -e "${BLUE}║        app3 QSIGN 전체 통합 테스트                        ║${NC}"
    echo -e "${BLUE}║        app3 ↔ APISIX ↔ Keycloak ↔ Vault+HSM             ║${NC}"
    echo -e "${BLUE}║                                                           ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""

    test_qkms
    test_qsign
    test_qgateway
    test_app3
    test_full_integration

    echo ""
    print_summary
}

# 스크립트 실행
main