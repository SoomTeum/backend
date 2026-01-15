#!/bin/bash

#
# k6 캐시 성능 비교 테스트 실행 스크립트
#
# 사용법:
#   chmod +x run-comparison-test.sh
#   ./run-comparison-test.sh
#

BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="./results"

# 결과 디렉토리 생성
mkdir -p $RESULTS_DIR

echo "========================================"
echo "  k6 캐시 성능 비교 테스트"
echo "========================================"
echo "  Base URL: $BASE_URL"
echo "  Results: $RESULTS_DIR"
echo "========================================"
echo ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 1. 캐시 비우기 (Cold Start 테스트 준비)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "[Step 1] 캐시 비우기..."
curl -X DELETE "$BASE_URL/api/admin/cache/all" -s
echo ""
sleep 2

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 2. Cold Cache 테스트 (캐시 적용 전 시뮬레이션)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "[Step 2] Cold Cache 테스트 (워밍업 없음)..."
k6 run \
  --env BASE_URL=$BASE_URL \
  --env SCENARIO=light \
  --env WARMUP=false \
  --out json=$RESULTS_DIR/cold-cache-light.json \
  cache-performance-test.js

echo ""
echo "캐시 상태 확인:"
curl -s "$BASE_URL/api/admin/cache/summary" | jq '.'
echo ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 3. Warm Cache 테스트 (캐시 적용 후)
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "[Step 3] Warm Cache 테스트 (워밍업 포함)..."
k6 run \
  --env BASE_URL=$BASE_URL \
  --env SCENARIO=light \
  --env WARMUP=true \
  --out json=$RESULTS_DIR/warm-cache-light.json \
  cache-performance-test.js

echo ""
echo "최종 캐시 상태:"
curl -s "$BASE_URL/api/admin/cache/summary" | jq '.'
echo ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 4. 결과 비교 출력
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

echo "========================================"
echo "  테스트 완료!"
echo "========================================"
echo ""
echo "결과 파일:"
ls -la $RESULTS_DIR/
echo ""
echo "결과 비교를 위해 JSON 파일을 확인하세요."
echo "========================================"
