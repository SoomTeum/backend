package com.comma.soomteum.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 캐시 통계 확인 및 관리 API
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cache Admin", description = "캐시 통계 및 관리 API")
public class CacheStatsController {

    private final CacheManager cacheManager;

    /**
     * 모든 캐시의 통계 조회
     */
    @GetMapping("/stats")
    @Operation(summary = "전체 캐시 통계", description = "모든 캐시의 히트율, 미스율 등 통계 정보를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getAllCacheStats() {
        Map<String, Object> result = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (springCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();

                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("size", nativeCache.estimatedSize());
                cacheInfo.put("hitCount", stats.hitCount());
                cacheInfo.put("missCount", stats.missCount());
                cacheInfo.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                cacheInfo.put("missRate", String.format("%.2f%%", stats.missRate() * 100));
                cacheInfo.put("evictionCount", stats.evictionCount());
                cacheInfo.put("loadSuccessCount", stats.loadSuccessCount());
                cacheInfo.put("loadFailureCount", stats.loadFailureCount());
                cacheInfo.put("averageLoadPenalty", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0));

                result.put(cacheName, cacheInfo);
            }
        }

        log.info("[CacheStats] 캐시 통계 조회");
        return ResponseEntity.ok(result);
    }

    /**
     * 특정 캐시의 통계 조회
     */
    @GetMapping("/stats/{cacheName}")
    @Operation(summary = "특정 캐시 통계", description = "지정한 캐시의 상세 통계 정보를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getCacheStats(@PathVariable String cacheName) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);

        if (springCache == null) {
            return ResponseEntity.notFound().build();
        }

        if (springCache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats stats = nativeCache.stats();

            Map<String, Object> cacheInfo = new HashMap<>();
            cacheInfo.put("name", cacheName);
            cacheInfo.put("size", nativeCache.estimatedSize());
            cacheInfo.put("hitCount", stats.hitCount());
            cacheInfo.put("missCount", stats.missCount());
            cacheInfo.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
            cacheInfo.put("missRate", String.format("%.2f%%", stats.missRate() * 100));
            cacheInfo.put("requestCount", stats.requestCount());
            cacheInfo.put("evictionCount", stats.evictionCount());
            cacheInfo.put("evictionWeight", stats.evictionWeight());
            cacheInfo.put("loadSuccessCount", stats.loadSuccessCount());
            cacheInfo.put("loadFailureCount", stats.loadFailureCount());
            cacheInfo.put("totalLoadTime", String.format("%.2f ms", stats.totalLoadTime() / 1_000_000.0));
            cacheInfo.put("averageLoadPenalty", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0));

            log.info("[CacheStats] 캐시 통계 조회: {}", cacheName);
            return ResponseEntity.ok(cacheInfo);
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * 전체 캐시 요약
     */
    @GetMapping("/summary")
    @Operation(summary = "캐시 요약", description = "모든 캐시의 요약 정보 (크기, 히트율)를 반환합니다.")
    public ResponseEntity<Map<String, Object>> getCacheSummary() {
        Map<String, Object> summary = new HashMap<>();

        long totalHits = 0;
        long totalMisses = 0;
        long totalSize = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
            if (springCache instanceof CaffeineCache caffeineCache) {
                Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();

                totalHits += stats.hitCount();
                totalMisses += stats.missCount();
                totalSize += nativeCache.estimatedSize();

                summary.put(cacheName, Map.of(
                        "size", nativeCache.estimatedSize(),
                        "hitRate", String.format("%.2f%%", stats.hitRate() * 100)
                ));
            }
        }

        double overallHitRate = (totalHits + totalMisses) > 0
                ? (double) totalHits / (totalHits + totalMisses) * 100
                : 0.0;

        summary.put("_overall", Map.of(
                "totalSize", totalSize,
                "totalHits", totalHits,
                "totalMisses", totalMisses,
                "overallHitRate", String.format("%.2f%%", overallHitRate)
        ));

        log.info("[CacheStats] 캐시 요약 조회 - 전체 히트율: {}", String.format("%.2f%%", overallHitRate));
        return ResponseEntity.ok(summary);
    }

    /**
     * 특정 캐시 비우기
     */
    @DeleteMapping("/{cacheName}")
    @Operation(summary = "캐시 비우기", description = "지정한 캐시의 모든 항목을 삭제합니다.")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            return ResponseEntity.notFound().build();
        }

        cache.clear();
        log.info("[CacheStats] 캐시 삭제: {}", cacheName);

        return ResponseEntity.ok(Map.of(
                "message", "캐시가 성공적으로 삭제되었습니다.",
                "cacheName", cacheName
        ));
    }

    /**
     * 모든 캐시 비우기
     */
    @DeleteMapping("/all")
    @Operation(summary = "전체 캐시 비우기", description = "모든 캐시의 항목을 삭제합니다.")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }

        log.info("[CacheStats] 전체 캐시 삭제");
        return ResponseEntity.ok(Map.of(
                "message", "모든 캐시가 성공적으로 삭제되었습니다."
        ));
    }
}
