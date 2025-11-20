#!/bin/bash
#
# QSIGN 통합 로그 수집 스크립트
#
# 사용법: ./collect-qsign-logs.sh [옵션]
#
# 옵션:
#   -t, --tail <N>     각 컴포넌트에서 가져올 로그 라인 수 (기본: 500)
#   -o, --output <DIR> 출력 디렉토리 (기본: /tmp/qsign-logs-<timestamp>)
#   -h, --help         도움말 표시
#

set -e

# 기본값
TAIL_LINES=500
OUTPUT_DIR=""
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 도움말
usage() {
    cat << EOF
QSIGN 통합 로그 수집 스크립트

사용법: $0 [옵션]

옵션:
  -t, --tail <N>     각 컴포넌트에서 가져올 로그 라인 수 (기본: 500)
  -o, --output <DIR> 출력 디렉토리 (기본: /tmp/qsign-logs-$TIMESTAMP)
  -h, --help         도움말 표시

예제:
  $0                              # 기본 설정으로 실행
  $0 -t 1000                      # 각 컴포넌트에서 1000줄 수집
  $0 -o /var/log/qsign            # 특정 디렉토리에 저장

수집되는 로그:
  - App5 (q-app)
  - APISIX Gateway (qsign-prod)
  - Keycloak PQC (q-sign)
  - Vault KMS (q-kms)
  - Pod 상태 정보
  - 서비스 정보

EOF
    exit 0
}

# 인자 파싱
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tail)
            TAIL_LINES="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

# 출력 디렉토리 설정
if [ -z "$OUTPUT_DIR" ]; then
    OUTPUT_DIR="/tmp/qsign-logs-$TIMESTAMP"
fi

mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  QSIGN 로그 수집 시작${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "📁 출력 디렉토리: ${GREEN}$OUTPUT_DIR${NC}"
echo -e "📄 로그 라인 수: ${GREEN}$TAIL_LINES${NC}"
echo ""

# 1. App5 로그
echo -e "${YELLOW}📱 App5 로그 수집 중...${NC}"
kubectl logs -n q-app deployment/app5 --tail=$TAIL_LINES > "$OUTPUT_DIR/app5.log" 2>&1 && \
    echo -e "   ${GREEN}✅ app5.log${NC}" || \
    echo -e "   ${RED}❌ App5 로그 수집 실패${NC}"

# 2. APISIX 로그
echo -e "${YELLOW}🌐 APISIX Gateway 로그 수집 중...${NC}"
kubectl logs -n qsign-prod deployment/apisix --tail=$TAIL_LINES > "$OUTPUT_DIR/apisix.log" 2>&1 && \
    echo -e "   ${GREEN}✅ apisix.log${NC}" || \
    echo -e "   ${RED}❌ APISIX 로그 수집 실패${NC}"

kubectl logs -n qsign-prod deployment/apisix-dashboard --tail=$TAIL_LINES > "$OUTPUT_DIR/apisix-dashboard.log" 2>&1 && \
    echo -e "   ${GREEN}✅ apisix-dashboard.log${NC}" || \
    echo -e "   ${YELLOW}⚠️  APISIX Dashboard 로그 수집 실패 (Pod 없을 수 있음)${NC}"

kubectl logs -n qsign-prod deployment/apisix-route-init --tail=$TAIL_LINES > "$OUTPUT_DIR/apisix-route-init.log" 2>&1 && \
    echo -e "   ${GREEN}✅ apisix-route-init.log${NC}" || \
    echo -e "   ${YELLOW}⚠️  APISIX Route Init 로그 수집 실패${NC}"

# 3. Keycloak 로그
echo -e "${YELLOW}🔐 Keycloak PQC 로그 수집 중...${NC}"
kubectl logs -n q-sign deployment/keycloak-pqc --tail=$TAIL_LINES > "$OUTPUT_DIR/keycloak.log" 2>&1 && \
    echo -e "   ${GREEN}✅ keycloak.log${NC}" || \
    echo -e "   ${RED}❌ Keycloak 로그 수집 실패${NC}"

# Keycloak 이전 Pod 로그 (재시작된 경우)
kubectl logs -n q-sign deployment/keycloak-pqc --previous --tail=$TAIL_LINES > "$OUTPUT_DIR/keycloak-previous.log" 2>&1 && \
    echo -e "   ${GREEN}✅ keycloak-previous.log${NC}" || \
    echo -e "   ${YELLOW}⚠️  이전 Keycloak Pod 로그 없음${NC}"

# 4. Vault 로그
echo -e "${YELLOW}🔑 Vault KMS 로그 수집 중...${NC}"
kubectl logs -n q-kms deployment/q-kms --tail=$TAIL_LINES > "$OUTPUT_DIR/vault.log" 2>&1 && \
    echo -e "   ${GREEN}✅ vault.log${NC}" || \
    echo -e "   ${RED}❌ Vault 로그 수집 실패${NC}"

# 5. PostgreSQL 로그
echo -e "${YELLOW}🗄️  PostgreSQL 로그 수집 중...${NC}"
kubectl logs -n q-sign deployment/postgres-qsign --tail=$TAIL_LINES > "$OUTPUT_DIR/postgres.log" 2>&1 && \
    echo -e "   ${GREEN}✅ postgres.log${NC}" || \
    echo -e "   ${YELLOW}⚠️  PostgreSQL 로그 수집 실패${NC}"

# 6. Redis 로그
echo -e "${YELLOW}💾 Redis 로그 수집 중...${NC}"
kubectl logs -n qsign-prod deployment/redis --tail=$TAIL_LINES > "$OUTPUT_DIR/redis.log" 2>&1 && \
    echo -e "   ${GREEN}✅ redis.log${NC}" || \
    echo -e "   ${YELLOW}⚠️  Redis 로그 수집 실패${NC}"

echo ""
echo -e "${YELLOW}📊 클러스터 상태 정보 수집 중...${NC}"

# 7. Pod 상태
kubectl get pods -n q-app -o wide > "$OUTPUT_DIR/pods-q-app.txt" 2>&1
kubectl get pods -n qsign-prod -o wide > "$OUTPUT_DIR/pods-qsign-prod.txt" 2>&1
kubectl get pods -n q-sign -o wide > "$OUTPUT_DIR/pods-q-sign.txt" 2>&1
kubectl get pods -n q-kms -o wide > "$OUTPUT_DIR/pods-q-kms.txt" 2>&1
echo -e "   ${GREEN}✅ Pod 상태 정보${NC}"

# 8. 서비스 정보
kubectl get svc -n q-app > "$OUTPUT_DIR/svc-q-app.txt" 2>&1
kubectl get svc -n qsign-prod > "$OUTPUT_DIR/svc-qsign-prod.txt" 2>&1
kubectl get svc -n q-sign > "$OUTPUT_DIR/svc-q-sign.txt" 2>&1
kubectl get svc -n q-kms > "$OUTPUT_DIR/svc-q-kms.txt" 2>&1
echo -e "   ${GREEN}✅ 서비스 정보${NC}"

# 9. Deployment 상태
kubectl get deployments -n q-app > "$OUTPUT_DIR/deployments-q-app.txt" 2>&1
kubectl get deployments -n qsign-prod > "$OUTPUT_DIR/deployments-qsign-prod.txt" 2>&1
kubectl get deployments -n q-sign > "$OUTPUT_DIR/deployments-q-sign.txt" 2>&1
kubectl get deployments -n q-kms > "$OUTPUT_DIR/deployments-q-kms.txt" 2>&1
echo -e "   ${GREEN}✅ Deployment 상태${NC}"

# 10. 이벤트
kubectl get events -n q-app --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events-q-app.txt" 2>&1
kubectl get events -n qsign-prod --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events-qsign-prod.txt" 2>&1
kubectl get events -n q-sign --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events-q-sign.txt" 2>&1
kubectl get events -n q-kms --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events-q-kms.txt" 2>&1
echo -e "   ${GREEN}✅ 이벤트 로그${NC}"

# 11. ConfigMap (Keycloak, APISIX)
kubectl get configmap -n q-sign keycloak-pqc-config -o yaml > "$OUTPUT_DIR/configmap-keycloak.yaml" 2>&1
kubectl get configmap -n qsign-prod apisix -o yaml > "$OUTPUT_DIR/configmap-apisix.yaml" 2>&1
echo -e "   ${GREEN}✅ ConfigMap 정보${NC}"

# 12. APISIX 라우트 정보
echo ""
echo -e "${YELLOW}🔍 APISIX 라우트 정보 수집 중...${NC}"
curl -s "http://192.168.0.11:30282/apisix/admin/routes" \
  -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" > "$OUTPUT_DIR/apisix-routes.json" 2>&1 && \
    echo -e "   ${GREEN}✅ apisix-routes.json${NC}" || \
    echo -e "   ${YELLOW}⚠️  APISIX Admin API 접근 실패${NC}"

# 13. Vault 상태
echo ""
echo -e "${YELLOW}🔐 Vault 상태 정보 수집 중...${NC}"
curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/sys/health" > "$OUTPUT_DIR/vault-health.json" 2>&1 && \
    echo -e "   ${GREEN}✅ vault-health.json${NC}" || \
    echo -e "   ${YELLOW}⚠️  Vault Health API 접근 실패${NC}"

curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/sys/mounts" > "$OUTPUT_DIR/vault-mounts.json" 2>&1 && \
    echo -e "   ${GREEN}✅ vault-mounts.json${NC}" || \
    echo -e "   ${YELLOW}⚠️  Vault Mounts API 접근 실패${NC}"

curl -s -X LIST -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" \
  "http://192.168.0.11:30820/v1/transit/keys" > "$OUTPUT_DIR/vault-transit-keys.json" 2>&1 && \
    echo -e "   ${GREEN}✅ vault-transit-keys.json${NC}" || \
    echo -e "   ${YELLOW}⚠️  Vault Transit Keys API 접근 실패${NC}"

# 14. 로그 요약 생성
echo ""
echo -e "${YELLOW}📝 로그 요약 생성 중...${NC}"
cat > "$OUTPUT_DIR/SUMMARY.txt" << EOF
QSIGN 로그 수집 요약
==================

수집 시간: $TIMESTAMP
로그 라인 수: $TAIL_LINES

수집된 로그 파일:
-----------------
$(ls -lh "$OUTPUT_DIR" | tail -n +2)

총 파일 개수: $(ls -1 "$OUTPUT_DIR" | wc -l)
총 크기: $(du -sh "$OUTPUT_DIR" | cut -f1)

주요 컴포넌트 상태:
-----------------
$(kubectl get pods -n q-app,qsign-prod,q-sign,q-kms --no-headers 2>/dev/null | awk '{print $1, $2, $3}')

APISIX 라우트 수:
-----------------
$(curl -s "http://192.168.0.11:30282/apisix/admin/routes" -H "X-API-KEY: edd1c9f034335f136f87ad84b625c8f1" 2>/dev/null | python3 -c "import sys, json; data = json.load(sys.stdin); print(len(data.get('list', {}).get('list', [])))" 2>/dev/null || echo "조회 실패")

Vault 상태:
----------
$(curl -s -H "X-Vault-Token: <VAULT_ROOT_TOKEN>" "http://192.168.0.11:30820/v1/sys/health" 2>/dev/null | python3 -c "import sys, json; data = json.load(sys.stdin); print(f\"Initialized: {data.get('initialized')}, Sealed: {data.get('sealed')}\")" 2>/dev/null || echo "조회 실패")

로그 분석 명령어:
---------------
# 에러 검색
grep -r "ERROR\|error" $OUTPUT_DIR/*.log

# Vault 관련 로그
grep -r "vault\|Vault" $OUTPUT_DIR/keycloak.log

# DILITHIUM3 서명
grep -r "DILITHIUM3\|dilithium" $OUTPUT_DIR/*.log

# HTTP 에러
grep -rE "HTTP [4-5][0-9][0-9]" $OUTPUT_DIR/*.log
EOF
echo -e "   ${GREEN}✅ SUMMARY.txt${NC}"

# 15. 로그 압축
echo ""
echo -e "${YELLOW}📦 로그 압축 중...${NC}"
ARCHIVE_FILE="$OUTPUT_DIR.tar.gz"
tar -czf "$ARCHIVE_FILE" -C "$(dirname "$OUTPUT_DIR")" "$(basename "$OUTPUT_DIR")" 2>&1 && \
    echo -e "   ${GREEN}✅ $ARCHIVE_FILE${NC}" || \
    echo -e "   ${RED}❌ 압축 실패${NC}"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  ✅ 로그 수집 완료!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "📁 로그 디렉토리: ${BLUE}$OUTPUT_DIR${NC}"
echo -e "📦 압축 파일: ${BLUE}$ARCHIVE_FILE${NC}"
echo -e "📊 요약 파일: ${BLUE}$OUTPUT_DIR/SUMMARY.txt${NC}"
echo ""
echo -e "${YELLOW}다음 명령어로 요약을 확인하세요:${NC}"
echo -e "  ${BLUE}cat $OUTPUT_DIR/SUMMARY.txt${NC}"
echo ""
echo -e "${YELLOW}압축 파일을 다른 위치로 이동하려면:${NC}"
echo -e "  ${BLUE}cp $ARCHIVE_FILE /path/to/destination/${NC}"
echo ""
