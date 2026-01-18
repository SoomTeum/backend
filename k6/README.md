# k6 캐시 성능 비교 테스트

Caffeine 캐시 적용 전/후 성능 차이를 측정하기 위한 k6 부하 테스트 스크립트입니다.

---

## 설치

```bash
# macOS
brew install k6

# Windows (Chocolatey)
choco install k6

# Windows (winget)
winget install k6

# Docker
docker pull grafana/k6
```

---

## 실행 방법

### 1. 기본 실행

```bash
cd k6

# 기본 실행 (10 VUs, 1분)
k6 run cache-performance-test.js
```

### 2. 시나리오 선택 실행

```bash
# Light (10 VUs, 1분) - 기본 성능 측정
k6 run --env SCENARIO=light cache-performance-test.js

# Medium (50 VUs, 2분) - 중간 부하
k6 run --env SCENARIO=medium cache-performance-test.js

# Heavy (100 VUs, 2분) - 높은 부하
k6 run --env SCENARIO=heavy cache-performance-test.js

# Stress (점진적 증가: 0→10→50→100→0)
k6 run --env SCENARIO=stress cache-performance-test.js
```

### 3. 캐시 워밍업 제어

```bash
# 워밍업 없이 실행 (Cold Cache - 캐시 미적용 시뮬레이션)
k6 run --env WARMUP=false cache-performance-test.js

# 워밍업 포함 실행 (Warm Cache - 캐시 적용 후)
k6 run --env WARMUP=true cache-performance-test.js
```

### 4. 전체 비교 테스트 실행

```bash
# Windows
run-comparison-test.bat

# Linux/macOS
chmod +x run-comparison-test.sh
./run-comparison-test.sh
```

---

## 캐시 OFF / ON 비교 전략

### 전략 1: Cold vs Warm Cache 비교

```bash
# Step 1: 캐시 비우기
curl -X DELETE "http://localhost:8080/api/admin/cache/all"

# Step 2: Cold Cache 테스트 (첫 요청 - 캐시 미스)
k6 run --env WARMUP=false --env SCENARIO=light cache-performance-test.js

# Step 3: Warm Cache 테스트 (캐시 히트)
k6 run --env WARMUP=true --env SCENARIO=light cache-performance-test.js
```

### 전략 2: 동일 조건 반복 비교

```bash
# 캐시 비우기
curl -X DELETE "http://localhost:8080/api/admin/cache/all"

# 첫 번째 실행 (대부분 캐시 미스)
k6 run --env SCENARIO=medium cache-performance-test.js

# 두 번째 실행 (대부분 캐시 히트)
k6 run --env SCENARIO=medium cache-performance-test.js
```

### 전략 3: 코드 레벨에서 캐시 비활성화

`CacheConfig.java`에서 캐시를 비활성화하고 테스트:

```java
// 캐시 비활성화 (테스트용)
@Bean
public CacheManager cacheManager() {
    return new NoOpCacheManager(); // 캐시 동작 안 함
}
```

---

## 측정 지표 설명

| 지표 | 설명 | 목표 |
|------|------|------|
| **p95 Latency** | 95%의 요청이 이 시간 내에 완료됨 | < 2,000ms |
| **TPS (req/s)** | 초당 처리 요청 수 | > 10 req/s |
| **Error Rate** | 실패한 요청 비율 | < 1% |
| **Cache Hit Rate** | 캐시에서 응답한 비율 | > 60% |

---

## 결과 해석 가이드

### 콘솔 출력 예시

```
================================================================================
                         k6 캐시 성능 테스트 결과
================================================================================

  시나리오: light | 워밍업: true

--------------------------------------------------------------------------------
  주요 지표
--------------------------------------------------------------------------------

  [TPS (Throughput)]
    - 총 요청 수: 342
    - 초당 요청 수: 5.70 req/s

  [응답 시간 (Latency)]
    - 평균: 156.23 ms
    - 중앙값: 98.45 ms
    - p90: 312.67 ms
    - p95: 428.91 ms  ◀ 핵심 지표
    - p99: 892.34 ms
    - 최대: 1523.12 ms

  [에러율]
    - Error Rate: 0.00%

--------------------------------------------------------------------------------
  임계값 검사 (Thresholds)
--------------------------------------------------------------------------------
  ✓ http_req_duration: PASS
  ✓ error_rate: PASS
  ✓ area_api_duration: PASS
  ✓ http_reqs: PASS

================================================================================
```

### 비교 결과 해석

| 지표 | Cold Cache | Warm Cache | 개선율 |
|------|------------|------------|--------|
| p95 Latency | 2,400ms | 560ms | **76.7% ↓** |
| 평균 응답 시간 | 1,800ms | 156ms | **91.3% ↓** |
| TPS | 2.1 req/s | 5.7 req/s | **171% ↑** |
| Error Rate | 0.5% | 0.0% | **100% ↓** |

---

## 포트폴리오 활용 가이드

### 1. 성능 개선 수치 강조

```
✅ Caffeine 캐시 적용으로 API 응답 시간 77% 개선
   - Before: p95 2,400ms → After: p95 560ms
   - 외부 API 호출 800ms + N+1 DB 쿼리 1,200ms 제거
```

### 2. 그래프 생성을 위한 데이터 수집

```bash
# JSON 출력으로 결과 저장
k6 run --out json=cold-results.json --env WARMUP=false cache-performance-test.js
k6 run --out json=warm-results.json --env WARMUP=true cache-performance-test.js
```

### 3. 주요 비교 포인트

1. **p95 응답 시간**: 사용자 체감 성능의 핵심 지표
2. **TPS 향상**: 동일 서버로 더 많은 요청 처리 가능
3. **캐시 히트율**: 캐시 설계의 효율성 증명
4. **안정성**: 높은 부하에서도 에러율 유지

### 4. 발표/문서용 요약

```markdown
## 성능 최적화 결과

### 문제
- 지역 기반 추천 API 응답 시간 평균 2.4초
- 외부 Tour API 호출 (800ms)
- 한적함 점수 N+1 조회 (1,200ms)
- 메타데이터 DB 조회 (400ms)

### 해결
- Caffeine 로컬 캐시 5계층 적용
- Reactive 환경 안전한 캐싱 (ReactiveCacheHelper)

### 결과
| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| p95 Latency | 2,400ms | 560ms | 77% ↓ |
| TPS | 2.1 req/s | 5.7 req/s | 171% ↑ |
| Cache Hit Rate | - | 67% | - |
```

---

## 문제 해결

### k6 실행 시 연결 오류

```bash
# 서버가 실행 중인지 확인
curl http://localhost:8080/actuator/health
```

### 캐시 통계가 0으로 나오는 경우

`CacheConfig.java`에서 `recordStats()` 활성화 확인:

```java
Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .maximumSize(500)
    .recordStats()  // ← 필수!
    .build()
```

### Windows에서 jq 명령어 오류

```bash
# jq 설치 (Chocolatey)
choco install jq

# 또는 jq 없이 실행
curl -s "http://localhost:8080/api/admin/cache/summary"
```

---

## 파일 구조

```
k6/
├── cache-performance-test.js   # 메인 테스트 스크립트
├── run-comparison-test.sh      # Linux/macOS 실행 스크립트
├── run-comparison-test.bat     # Windows 실행 스크립트
├── README.md                   # 이 문서
└── results/                    # 테스트 결과 저장 (자동 생성)
    ├── cold-cache-light.json
    └── warm-cache-light.json
```
