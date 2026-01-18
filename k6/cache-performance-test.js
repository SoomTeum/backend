/**
 * k6 캐시 성능 비교 테스트 스크립트
 *
 * 목적: Caffeine 캐시 적용 전/후 성능 차이 측정
 * 대상: GET /api/tour/area - 지역 기반 추천 API
 *
 * 실행 방법:
 *   # 기본 실행 (10 VUs)
 *   k6 run cache-performance-test.js
 *
 *   # 시나리오 선택 실행
 *   k6 run --env SCENARIO=light cache-performance-test.js    # 10 VUs
 *   k6 run --env SCENARIO=medium cache-performance-test.js   # 50 VUs
 *   k6 run --env SCENARIO=heavy cache-performance-test.js    # 100 VUs
 *   k6 run --env SCENARIO=stress cache-performance-test.js   # 점진적 증가
 *
 *   # 캐시 워밍업 포함
 *   k6 run --env WARMUP=true cache-performance-test.js
 *
 *   # HTML 리포트 생성
 *   k6 run --out json=results.json cache-performance-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 설정
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = __ENV.SCENARIO || 'light';
const WARMUP = __ENV.WARMUP === 'true';

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 커스텀 메트릭
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// 에러율
const errorRate = new Rate('error_rate');

// API별 응답 시간
const areaApiDuration = new Trend('area_api_duration', true);
const cacheStatsApiDuration = new Trend('cache_stats_api_duration', true);

// 요청 카운터
const totalRequests = new Counter('total_requests');
const successRequests = new Counter('success_requests');
const failedRequests = new Counter('failed_requests');

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 시나리오 설정
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

const scenarios = {
    // 가벼운 부하: 10 VUs
    light: {
        executor: 'constant-vus',
        vus: 10,
        duration: '1m',
    },

    // 중간 부하: 50 VUs
    medium: {
        executor: 'constant-vus',
        vus: 50,
        duration: '2m',
    },

    // 높은 부하: 100 VUs
    heavy: {
        executor: 'constant-vus',
        vus: 100,
        duration: '2m',
    },

    // 점진적 증가 (스트레스 테스트)
    stress: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '30s', target: 10 },   // 워밍업
            { duration: '1m', target: 50 },    // 중간 부하
            { duration: '1m', target: 100 },   // 높은 부하
            { duration: '30s', target: 100 },  // 유지
            { duration: '30s', target: 0 },    // 쿨다운
        ],
        gracefulRampDown: '10s',
    },
};

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// k6 옵션
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

export const options = {
    scenarios: {
        default: scenarios[SCENARIO] || scenarios.light,
    },

    // 성능 임계값 (Pass/Fail 기준)
    thresholds: {
        // p95 응답 시간: 3초 이내
        'http_req_duration': ['p(95)<3000'],

        // 에러율: 1% 이하
        'error_rate': ['rate<0.01'],

        // Area API p95: 2초 이내
        'area_api_duration': ['p(95)<2000'],

        // 초당 요청 수: 최소 10 RPS
        'http_reqs': ['rate>10'],
    },

    // 요약 출력 설정
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 테스트 데이터 (다양한 파라미터 조합)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// API 엔드포인트: /api/places
const API_PATH = '/api/places';

const testParams = [
    { areaCode: 32, sigunguCode: 1, cat1: 'A01', cat2: 'A0101' },  // 강원 자연
    { areaCode: 32, sigunguCode: 1, cat1: 'A02', cat2: 'A0201' },  // 강원 인문
    { areaCode: 32, sigunguCode: 5, cat1: 'A01', cat2: 'A0101' },  // 강릉 자연
    { areaCode: 1, sigunguCode: 1, cat1: 'A02', cat2: 'A0201' },   // 서울 인문
    { areaCode: 6, sigunguCode: 1, cat1: 'A01', cat2: 'A0101' },   // 부산 자연
];

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 셋업 (캐시 워밍업)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

export function setup() {
    console.log(`\n========================================`);
    console.log(`  k6 캐시 성능 테스트`);
    console.log(`========================================`);
    console.log(`  Base URL: ${BASE_URL}`);
    console.log(`  Scenario: ${SCENARIO}`);
    console.log(`  Warmup: ${WARMUP}`);
    console.log(`========================================\n`);

    if (WARMUP) {
        console.log('[Setup] 캐시 워밍업 시작...');

        // 모든 테스트 파라미터로 API 호출하여 캐시 워밍업
        testParams.forEach((params, idx) => {
            const url = `${BASE_URL}${API_PATH}?areaCode=${params.areaCode}&sigunguCode=${params.sigunguCode}&cat1=${params.cat1}&cat2=${params.cat2}&pageNo=1&numOfRows=10`;
            const res = http.get(url, { timeout: '30s' });
            console.log(`  [${idx + 1}/${testParams.length}] Warmup: ${res.status} (${res.timings.duration.toFixed(0)}ms)`);
            sleep(0.5);
        });

        console.log('[Setup] 캐시 워밍업 완료\n');

        // 워밍업 후 캐시 통계 확인
        const statsRes = http.get(`${BASE_URL}/api/admin/cache/summary`);
        if (statsRes.status === 200) {
            console.log('[Setup] 캐시 상태:', statsRes.body);
        }
    }

    return { startTime: new Date().toISOString() };
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 메인 테스트 함수
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

export default function () {
    // 랜덤 파라미터 선택
    const params = testParams[Math.floor(Math.random() * testParams.length)];

    group('지역 기반 추천 API', function () {
        const url = `${BASE_URL}${API_PATH}?areaCode=${params.areaCode}&sigunguCode=${params.sigunguCode}&cat1=${params.cat1}&cat2=${params.cat2}&pageNo=1&numOfRows=10`;

        const res = http.get(url, {
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            timeout: '30s',
        });

        // 메트릭 기록
        totalRequests.add(1);
        areaApiDuration.add(res.timings.duration);

        // 성공/실패 체크
        const isSuccess = check(res, {
            'status is 200': (r) => r.status === 200,
            'response time < 3s': (r) => r.timings.duration < 3000,
            'has response body': (r) => r.body && r.body.length > 0,
        });

        if (isSuccess) {
            successRequests.add(1);
            errorRate.add(0);
        } else {
            failedRequests.add(1);
            errorRate.add(1);
            console.log(`[Error] Status: ${res.status}, Duration: ${res.timings.duration.toFixed(0)}ms`);
        }
    });

    // 요청 간 랜덤 대기 (실제 사용자 패턴 시뮬레이션)
    sleep(Math.random() * 2 + 0.5); // 0.5 ~ 2.5초
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 테스트 종료 후 요약
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

export function teardown(data) {
    console.log(`\n========================================`);
    console.log(`  테스트 완료`);
    console.log(`========================================`);
    console.log(`  시작 시간: ${data.startTime}`);
    console.log(`  종료 시간: ${new Date().toISOString()}`);
    console.log(`========================================`);

    // 최종 캐시 통계 확인
    const statsRes = http.get(`${BASE_URL}/api/admin/cache/summary`);
    if (statsRes.status === 200) {
        console.log('\n[Final] 캐시 통계:');
        try {
            const stats = JSON.parse(statsRes.body);
            Object.keys(stats).forEach(key => {
                if (key !== '_overall') {
                    console.log(`  ${key}: size=${stats[key].size}, hitRate=${stats[key].hitRate}`);
                }
            });
            if (stats._overall) {
                console.log(`\n  [Overall] hits=${stats._overall.totalHits}, misses=${stats._overall.totalMisses}, hitRate=${stats._overall.overallHitRate}`);
            }
        } catch (e) {
            console.log(statsRes.body);
        }
    }
    console.log(`========================================\n`);
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 커스텀 요약 핸들러
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// 안전한 값 접근 헬퍼
function safeGet(obj, path, defaultVal) {
    const keys = path.split('.');
    let result = obj;
    for (const key of keys) {
        if (result == null) return defaultVal;
        result = result[key];
    }
    return result != null ? result : defaultVal;
}

export function handleSummary(data) {
    const httpReqs = safeGet(data, 'metrics.http_reqs.values', {});
    const httpDuration = safeGet(data, 'metrics.http_req_duration.values', {});
    const errorRateVal = safeGet(data, 'metrics.error_rate.values', {});
    const areaApiDur = safeGet(data, 'metrics.area_api_duration.values', {});

    const summary = {
        scenario: SCENARIO,
        warmup: WARMUP,
        timestamp: new Date().toISOString(),
        metrics: {
            http_reqs: {
                count: httpReqs.count || 0,
                rate: httpReqs.rate ? httpReqs.rate.toFixed(2) : '0',
            },
            http_req_duration: {
                avg: httpDuration.avg ? httpDuration.avg.toFixed(2) : '0',
                med: httpDuration.med ? httpDuration.med.toFixed(2) : '0',
                p90: httpDuration['p(90)'] ? httpDuration['p(90)'].toFixed(2) : '0',
                p95: httpDuration['p(95)'] ? httpDuration['p(95)'].toFixed(2) : '0',
                p99: httpDuration['p(99)'] ? httpDuration['p(99)'].toFixed(2) : '0',
                max: httpDuration.max ? httpDuration.max.toFixed(2) : '0',
            },
            error_rate: errorRateVal.rate ? errorRateVal.rate.toFixed(4) : '0',
            area_api_duration: {
                avg: areaApiDur.avg ? areaApiDur.avg.toFixed(2) : '0',
                p95: areaApiDur['p(95)'] ? areaApiDur['p(95)'].toFixed(2) : '0',
            },
        },
        thresholds: data.thresholds,
    };

    // 콘솔 출력용 텍스트
    const consoleOutput = `
================================================================================
                         k6 캐시 성능 테스트 결과
================================================================================

  시나리오: ${SCENARIO} | 워밍업: ${WARMUP}

--------------------------------------------------------------------------------
  주요 지표
--------------------------------------------------------------------------------

  [TPS (Throughput)]
    - 총 요청 수: ${summary.metrics.http_reqs.count}
    - 초당 요청 수: ${summary.metrics.http_reqs.rate} req/s

  [응답 시간 (Latency)]
    - 평균: ${summary.metrics.http_req_duration.avg} ms
    - 중앙값: ${summary.metrics.http_req_duration.med} ms
    - p90: ${summary.metrics.http_req_duration.p90} ms
    - p95: ${summary.metrics.http_req_duration.p95} ms  ◀ 핵심 지표
    - p99: ${summary.metrics.http_req_duration.p99} ms
    - 최대: ${summary.metrics.http_req_duration.max} ms

  [에러율]
    - Error Rate: ${(summary.metrics.error_rate * 100).toFixed(2)}%

  [Area API 전용]
    - 평균: ${summary.metrics.area_api_duration.avg} ms
    - p95: ${summary.metrics.area_api_duration.p95} ms

--------------------------------------------------------------------------------
  임계값 검사 (Thresholds)
--------------------------------------------------------------------------------
${Object.entries(data.thresholds || {}).map(([key, val]) =>
    `  ${val.ok ? '✓' : '✗'} ${key}: ${val.ok ? 'PASS' : 'FAIL'}`
).join('\n')}

================================================================================
`;

    return {
        'stdout': consoleOutput,
        [`results-${SCENARIO}-${WARMUP ? 'warm' : 'cold'}.json`]: JSON.stringify(summary, null, 2),
    };
}
