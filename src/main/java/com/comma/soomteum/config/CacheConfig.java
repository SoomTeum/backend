package com.comma.soomteum.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 캐시 설정
 *
 * 캐시 종류:
 * - tourApiCache: Tour API 응답 캐싱 (TTL: 1시간, 최대: 500)
 * - cnctrRateCache: 한적함 점수 캐싱 (TTL: 30분, 최대: 1000)
 * - themeCache: 테마 메타데이터 캐싱 (TTL: 24시간, 최대: 100)
 * - regionCache: 지역 메타데이터 캐싱 (TTL: 24시간, 최대: 300)
 * - placeLikeCache: 장소 좋아요 수 캐싱 (TTL: 10분, 최대: 500)
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * 캐시 이름 상수
     */
    public static final String TOUR_API_CACHE = "tourApiCache";
    public static final String CNCTR_RATE_CACHE = "cnctrRateCache";
    public static final String THEME_CACHE = "themeCache";
    public static final String REGION_CACHE = "regionCache";
    public static final String PLACE_LIKE_CACHE = "placeLikeCache";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
                buildTourApiCache(),
                buildCnctrRateCache(),
                buildThemeCache(),
                buildRegionCache(),
                buildPlaceLikeCache()
        ));

        log.info("[CacheConfig] Caffeine 캐시 매니저 초기화 완료");
        return cacheManager;
    }

    /**
     * Tour API 응답 캐시
     * - TTL: 1시간
     * - 최대 크기: 500
     * - 키: areaCode:sigunguCode:contentTypeId:cat1:cat2:pageNo:numOfRows
     */
    private CaffeineCache buildTourApiCache() {
        return new CaffeineCache(TOUR_API_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(500)
                        .recordStats()
                        .build());
    }

    /**
     * 한적함 점수 캐시
     * - TTL: 30분
     * - 최대 크기: 1000
     * - 키: contentId
     */
    private CaffeineCache buildCnctrRateCache() {
        return new CaffeineCache(CNCTR_RATE_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());
    }

    /**
     * 테마 메타데이터 캐시
     * - TTL: 24시간
     * - 최대 크기: 100
     * - 키: cat1:cat2
     */
    private CaffeineCache buildThemeCache() {
        return new CaffeineCache(THEME_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(100)
                        .recordStats()
                        .build());
    }

    /**
     * 지역 메타데이터 캐시
     * - TTL: 24시간
     * - 최대 크기: 300
     * - 키: areaCode:sigunguCode
     */
    private CaffeineCache buildRegionCache() {
        return new CaffeineCache(REGION_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(300)
                        .recordStats()
                        .build());
    }

    /**
     * 장소 좋아요 수 캐시
     * - TTL: 10분
     * - 최대 크기: 500
     * - 키: contentId
     */
    private CaffeineCache buildPlaceLikeCache() {
        return new CaffeineCache(PLACE_LIKE_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());
    }
}
