#!/bin/bash

# q-app namespace의 오래된 ReplicaSet 정리 스크립트

echo "========================================="
echo "q-app ReplicaSet 정리"
echo "========================================="
echo ""

# 1. 모든 ReplicaSet 조회
echo "1️⃣ 현재 ReplicaSet 목록"
echo ""
sudo k3s kubectl get rs -n q-app -o wide
echo ""

# 2. replicas=0인 ReplicaSet 찾기 (오래된 버전들)
echo "2️⃣ 삭제 대상 ReplicaSet (replicas=0)"
echo ""
OLD_RS=$(sudo k3s kubectl get rs -n q-app -o json | jq -r '.items[] | select(.spec.replicas==0) | .metadata.name')

if [ -z "$OLD_RS" ]; then
    echo "   ✅ 삭제할 오래된 ReplicaSet이 없습니다."
    echo ""
    exit 0
fi

echo "$OLD_RS" | while read rs; do
    DESIRED=$(sudo k3s kubectl get rs -n q-app "$rs" -o jsonpath='{.spec.replicas}')
    CURRENT=$(sudo k3s kubectl get rs -n q-app "$rs" -o jsonpath='{.status.replicas}')
    AGE=$(sudo k3s kubectl get rs -n q-app "$rs" -o jsonpath='{.metadata.creationTimestamp}')
    echo "   📦 $rs (Desired: $DESIRED, Current: $CURRENT, Created: $AGE)"
done
echo ""

# 3. 삭제 확인
echo "========================================="
echo "삭제 진행"
echo "========================================="
echo ""

read -p "위의 ReplicaSet들을 삭제하시겠습니까? (y/N): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "삭제 중..."
    echo ""

    echo "$OLD_RS" | while read rs; do
        echo "   🗑️  Deleting: $rs"
        sudo k3s kubectl delete rs -n q-app "$rs"
    done

    echo ""
    echo "✅ 삭제 완료!"
    echo ""

    # 최종 상태 확인
    echo "========================================="
    echo "최종 ReplicaSet 목록"
    echo "========================================="
    echo ""
    sudo k3s kubectl get rs -n q-app
    echo ""
else
    echo ""
    echo "❌ 삭제 취소됨"
    echo ""
fi

echo "========================================="
echo "완료"
echo "========================================="
