# Caffeine 캐시 기반 API 성능 최적화

> Spring Cache + Caffeine을 활용한 지역 기반 추천 API 성능 개선 사례

---

## 1. 문제 정의

### 1.1 기존 시스템의 병목

지역 기반 추천 API(`GET /api/places`)의 단일 요청 처리 흐름:

```
┌─────────────────────────────────────────────────────────────────────┐
│  TourService.AreaPlaces() 실행 흐름                                  │
├─────────────────────────────────────────────────────────────────────┤
│  1. KorAreaService.areaBasedList()                                  │
│     → 외부 Tour API 호출                              약 800ms      │
│                                                                     │
│  2. TatsCnctrService.getCnctrRate() × N개 장소                      │
│     → 장소별 한적함 점수 외부 API (N+1 문제)          약 1,200ms    │
│                                                                     │
│  3. setCatNameAndAreaInfo() × N개 장소                              │
│     → Theme/Region/Place DB 조회 (N+1 문제)           약 400ms     │
│                                                                     │
│  총 응답 시간                                         약 2,400ms    │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 병목 원인 분석

| 구간 | 원인 | 영향 |
|------|------|------|
| 외부 Tour API | 네트워크 I/O, 공공데이터 API 응답 지연 | 800ms 고정 지연 |
| 한적함 점수 API | 장소 N개 × 개별 API 호출 (N+1) | N × 200~400ms |
| 메타데이터 조회 | 장소 N개 × DB 쿼리 3종 (N+1) | N × 10~50ms |

**핵심 문제**: 동일한 파라미터로 반복 호출 시에도 매번 외부 API와 DB를 조회

---

## 2. 해결 전략

### 2.1 기술 선택: Caffeine (로컬 캐시)

**Redis 대신 Caffeine을 선택한 이유:**

| 기준 | Redis | Caffeine | 선택 근거 |
|------|-------|----------|-----------|
| 응답 속도 | ~1ms (네트워크) | ~0.01ms (메모리) | ✅ 극한의 저지연 필요 |
| 인프라 | 별도 서버 필요 | JVM 내장 | ✅ 운영 복잡도 최소화 |
| 일관성 | 분산 환경 일관성 | 인스턴스별 독립 | 현재 단일 인스턴스 운영 |
| 데이터 특성 | 세션, 분산 락 | 읽기 전용 캐시 | ✅ 공공데이터 = 변경 빈도 낮음 |
| TTL 정밀도 | 초 단위 | 나노초 단위 | ✅ 정밀한 만료 제어 |

**Caffeine 선택 결론:**
- 현재 단일 서버 환경에서 네트워크 오버헤드 없이 최고 성능 확보
- 공공데이터 API 응답은 실시간성보다 일관성이 중요 (1시간 TTL 허용)
- 향후 스케일아웃 시 Redis로 전환 가능 (Spring Cache 추상화 활용)

### 2.2 캐시 계층 설계

```
┌────────────────────────────────────────────────────────────────────┐
│                        5계층 캐시 아키텍처                          │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│   [L1] tourApiCache        TTL: 1시간   최대: 500                  │
│         └─ 외부 Tour API 응답 전체 캐싱                            │
│         └─ 키: areaCode:sigunguCode:cat1:cat2:pageNo:numOfRows     │
│                                                                    │
│   [L2] cnctrRateCache      TTL: 30분    최대: 1,000                │
│         └─ 한적함 점수 (contentId 기준)                            │
│         └─ N+1 문제의 핵심 해결 지점                               │
│                                                                    │
│   [L3] themeCache          TTL: 24시간  최대: 100                  │
│         └─ 테마 메타데이터 (cat1:cat2)                             │
│                                                                    │
│   [L4] regionCache         TTL: 24시간  최대: 300                  │
│         └─ 지역 메타데이터 (areaCode:sigunguCode)                  │
│                                                                    │
│   [L5] placeLikeCache      TTL: 10분    최대: 500                  │
│         └─ 장소 좋아요 수 (contentId)                              │
│         └─ 짧은 TTL로 실시간성 유지                                │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 2.3 Reactive 환경 캐싱 문제 해결

**문제**: Spring `@Cacheable`은 `Mono/Flux`를 직접 캐싱하면 의도대로 동작하지 않음

```java
// ❌ 잘못된 방식: Mono 객체 자체가 캐싱됨 (값이 아님)
@Cacheable("cache")
public Mono<Data> getData() { ... }

// ✅ 해결: ReactiveCacheHelper로 값을 구독 후 캐싱
public Mono<Data> getData() {
    return cacheHelper.cacheMono("cache", key, () -> fetchData());
}
```

**구현**: `ReactiveCacheHelper` 클래스로 Mono 결과값을 안전하게 캐싱

---

## 3. 성능 측정 방법

### 3.1 캐시 비활성화 방법 (Before 측정용)

**권장: Profile 기반 NoOpCacheManager 적용**

```java
// application-no-cache.properties
spring.profiles.active=no-cache

// CacheConfig.java
@Profile("no-cache")
@Bean
public CacheManager noOpCacheManager() {
    return new NoOpCacheManager(); // 캐시 동작 안 함
}
```

**실행:**
```bash
# 캐시 비활성화 상태 테스트
./gradlew bootRun -Dspring.profiles.active=no-cache

# k6 테스트 실행
k6 run --env SCENARIO=light cache-performance-test.js
```

### 3.2 k6 테스트 시나리오

| 시나리오 | VUs | 지속 시간 | 목적 |
|----------|-----|-----------|------|
| light | 10 | 1분 | 기본 성능 측정 |
| medium | 50 | 2분 | 중간 부하 |
| heavy | 100 | 2분 | 고부하 안정성 |
| stress | 0→100→0 | 3분 | 점진적 부하 증가 |

---

## 4. 성능 비교 결과

### 4.1 Before vs After 비교

| 지표 | Before (캐시 미적용) | After (캐시 적용) | 개선율 |
|------|---------------------|-------------------|--------|
| **p95 응답 시간** | ~2,400ms | **15.29ms** | **99.4% ↓** |
| **평균 응답 시간** | ~1,800ms | **27.47ms** | **98.5% ↓** |
| **TPS** | ~2 req/s | **6.34 req/s** | **217% ↑** |
| **에러율** | 0.5~2% (타임아웃) | **0.00%** | **100% ↓** |
| **캐시 히트율** | - | **99.27%** | - |

### 4.2 응답 시간 분포 비교

```
Before (캐시 미적용)
├─ p50: ~1,500ms
├─ p90: ~2,200ms
├─ p95: ~2,400ms  ← 대부분의 요청이 2초 이상
└─ p99: ~3,500ms

After (캐시 적용)
├─ p50: 7.33ms
├─ p90: 11.50ms
├─ p95: 15.29ms   ← 99.4% 개선
└─ p99: 797.12ms  ← 캐시 미스 시에만 발생
```

### 4.3 캐시별 히트율 상세

| 캐시 | 히트율 | 역할 | 절감 효과 |
|------|--------|------|-----------|
| tourApiCache | 97.22% | 외부 API 응답 | 800ms × 97% = **776ms 절감** |
| cnctrRateCache | 97.99% | 한적함 점수 | N+1 문제 해결 |
| themeCache | 99.96% | 테마 정보 | DB 쿼리 제거 |
| regionCache | 99.81% | 지역 정보 | DB 쿼리 제거 |
| placeLikeCache | 99.06% | 좋아요 수 | DB 쿼리 제거 |

---

## 5. 외부 API 병목 제거 증거

### 5.1 로그 기반 확인

```
# 캐시 미스 시 (최초 요청)
[KorAreaService] Tour API 호출: areaCode=32, sigunguCode=1  ← 실제 호출
[ReactiveCacheHelper] 캐시 미스: cache=tourApiCache, key=32:1:A01:A0101:1:10
[ReactiveCacheHelper] 캐시 저장: cache=tourApiCache, key=32:1:A01:A0101:1:10

# 캐시 히트 시 (이후 요청)
[ReactiveCacheHelper] 캐시 히트: cache=tourApiCache, key=32:1:A01:A0101:1:10
# → Tour API 호출 로그 없음 = 외부 호출 제거됨
```

### 5.2 캐시 통계 API로 확인

```bash
GET /api/admin/cache/stats/tourApiCache

{
  "name": "tourApiCache",
  "size": 6,
  "hitCount": 350,
  "missCount": 10,
  "hitRate": "97.22%",        ← 97%의 요청에서 외부 API 호출 제거
  "averageLoadPenalty": "812.35 ms"  ← 미스 시 평균 800ms 소요
}
```

### 5.3 성능 개선 수식

```
Before: 응답 시간 = Tour API(800ms) + 한적함 API(N×200ms) + DB(N×10ms)
                 = 800 + 1200 + 400 = 약 2,400ms

After:  응답 시간 = 캐시 조회(0.01ms × 5계층)
                 = 약 0.05ms (캐시 히트 시)

개선율: (2400 - 0.05) / 2400 × 100 = 99.99%
```

---

## 6. 포트폴리오 정리

### 6.1 기술 스택

- **Spring Boot 3.5** + Spring Cache 추상화
- **Caffeine 3.1.8** - 고성능 로컬 캐시
- **WebFlux** - Reactive 스트림 처리
- **k6** - 부하 테스트 및 성능 측정

### 6.2 핵심 성과 (한 줄 요약)

> **Caffeine 캐시 5계층 적용으로 지역 기반 추천 API p95 응답 시간을 2,400ms → 15ms로 99.4% 개선**

### 6.3 상세 성과

```
✅ 외부 Tour API 호출 97% 제거 (800ms → 0ms)
✅ 한적함 점수 N+1 문제 해결 (1,200ms → 0ms)
✅ 메타데이터 DB 쿼리 99% 캐시 히트
✅ TPS 217% 향상 (2 → 6.34 req/s)
✅ 에러율 0% 달성 (타임아웃 제거)
```

### 6.4 기술 선택 이유 (면접 답변용)

**Q: 왜 Redis가 아닌 Caffeine을 선택했나요?**

> "현재 단일 서버 환경에서 네트워크 오버헤드 없이 최고 성능이 필요했습니다.
> 캐싱 대상이 공공데이터 API 응답으로 변경 빈도가 낮고,
> 인스턴스 간 일관성보다 응답 속도가 우선이었습니다.
>
> Caffeine은 JVM 힙 내에서 나노초 단위 응답이 가능하고,
> Spring Cache 추상화를 사용하므로 향후 Redis 전환도 설정 변경만으로 가능합니다.
>
> 다만 스케일아웃 시에는 캐시 일관성을 위해 Redis 또는
> Caffeine + Redis 2계층 구조로 전환을 고려하고 있습니다."

**Q: N+1 문제를 어떻게 해결했나요?**

> "한적함 점수 조회에서 장소 N개 × 외부 API 호출이 발생하는 N+1 문제가 있었습니다.
> contentId를 키로 하는 cnctrRateCache를 적용하여
> 동일 장소의 반복 조회를 캐시로 대체했습니다.
>
> 결과적으로 97.99%의 히트율을 달성하여
> N개의 외부 API 호출이 거의 제거되었습니다."

**Q: Reactive 환경에서 캐싱은 어떻게 처리했나요?**

> "Spring의 @Cacheable은 Mono를 반환하는 메서드에 적용하면
> Mono 객체 자체가 캐싱되어 의도대로 동작하지 않습니다.
>
> 이를 해결하기 위해 ReactiveCacheHelper를 구현하여
> Mono.defer() 내에서 캐시를 먼저 조회하고,
> 미스 시에만 실제 로직을 실행한 뒤 결과를 캐싱하도록 했습니다."

---

## 7. 모니터링 및 운영

### 7.1 캐시 통계 API

```bash
# 전체 캐시 요약
GET /api/admin/cache/summary

# 특정 캐시 상세
GET /api/admin/cache/stats/tourApiCache

# 캐시 수동 비우기 (배포 시)
DELETE /api/admin/cache/all
```

### 7.2 주요 모니터링 지표

| 지표 | 정상 범위 | 경고 임계값 | 대응 |
|------|-----------|-------------|------|
| 캐시 히트율 | > 90% | < 70% | TTL 또는 maxSize 조정 |
| 캐시 크기 | < maxSize의 80% | > 90% | maxSize 증가 검토 |
| Eviction 수 | 낮음 | 급증 시 | 캐시 크기 부족 |

---

## 8. 향후 개선 계획

1. **2계층 캐시**: Caffeine (L1) + Redis (L2) 구조로 확장성 확보
2. **캐시 프리로딩**: 서버 시작 시 인기 지역 데이터 사전 캐싱
3. **Cache-Aside → Write-Through**: 데이터 변경 시 캐시 자동 갱신
4. **분산 환경 대응**: Redis Cluster로 스케일아웃 지원

---

## 부록: 테스트 실행 방법

```bash
# 서버 실행
./gradlew bootRun

# k6 테스트 (Warm Cache)
cd k6
./k6-v0.48.0-windows-amd64/k6.exe run --env SCENARIO=light --env WARMUP=true cache-performance-test.js

# 캐시 통계 확인
curl http://localhost:8080/api/admin/cache/summary
```
