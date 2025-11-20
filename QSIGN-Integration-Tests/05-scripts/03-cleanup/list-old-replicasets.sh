#!/bin/bash

# 모든 네임스페이스의 오래된 ReplicaSet 조회 스크립트 (삭제 안함)

echo "========================================="
echo "전체 네임스페이스 ReplicaSet 현황"
echo "========================================="
echo ""

# 1. 모든 네임스페이스의 ReplicaSet 조회
echo "1️⃣ 현재 ReplicaSet 목록 (모든 네임스페이스)"
echo ""
sudo k3s kubectl get rs --all-namespaces -o wide
echo ""

# 2. replicas=0인 ReplicaSet 찾기 (오래된 버전들)
echo "2️⃣ 삭제 가능한 ReplicaSet (replicas=0)"
echo ""

# namespace와 name을 함께 가져오기
OLD_RS=$(sudo k3s kubectl get rs --all-namespaces -o json | jq -r '.items[] | select(.spec.replicas==0) | "\(.metadata.namespace) \(.metadata.name)"')

if [ -z "$OLD_RS" ]; then
    echo "   ✅ 삭제 가능한 오래된 ReplicaSet이 없습니다."
    echo ""
else
    COUNT=0
    echo "$OLD_RS" | while read ns name; do
        DESIRED=$(sudo k3s kubectl get rs -n "$ns" "$name" -o jsonpath='{.spec.replicas}')
        CURRENT=$(sudo k3s kubectl get rs -n "$ns" "$name" -o jsonpath='{.status.replicas}')
        AGE=$(sudo k3s kubectl get rs -n "$ns" "$name" -o jsonpath='{.metadata.creationTimestamp}')
        echo "   📦 $ns/$name (Desired: $DESIRED, Current: $CURRENT, Created: $AGE)"
        COUNT=$((COUNT + 1))
    done
    echo ""

    TOTAL=$(echo "$OLD_RS" | wc -l)
    echo "   총 $TOTAL 개의 ReplicaSet을 정리할 수 있습니다."
    echo ""
fi

echo "========================================="
echo "💡 정리 방법:"
echo "   ./cleanup-all-replicasets.sh"
echo "========================================="
