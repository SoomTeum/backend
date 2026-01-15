package com.comma.soomteum.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Reactive 환경에서 안전한 캐싱을 위한 헬퍼 클래스
 *
 * Spring Cache의 @Cacheable은 Mono/Flux를 직접 캐싱하면 문제가 발생합니다.
 * - Mono 자체가 캐싱되어 실제 값이 아닌 미완료 Mono가 저장됨
 * - 매번 새로운 구독이 발생하여 캐시 효과가 없음
 *
 * 이 헬퍼는 Mono의 결과값을 실제로 구독한 후 캐싱하여 안전하게 처리합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReactiveCacheHelper {

    private final CacheManager cacheManager;

    /**
     * Mono 결과를 캐싱합니다.
     *
     * @param cacheName 캐시 이름
     * @param key 캐시 키
     * @param monoSupplier 캐시 미스 시 실행할 Mono 공급자
     * @param <T> 반환 타입
     * @return 캐시된 값 또는 새로 조회한 값을 포함한 Mono
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> cacheMono(String cacheName, Object key, Supplier<Mono<T>> monoSupplier) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("[ReactiveCacheHelper] 캐시를 찾을 수 없음: {}", cacheName);
            return monoSupplier.get();
        }

        return Mono.defer(() -> {
            // 캐시에서 먼저 조회
            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null) {
                log.debug("[ReactiveCacheHelper] 캐시 히트: cache={}, key={}", cacheName, key);
                return Mono.just((T) cached.get());
            }

            log.debug("[ReactiveCacheHelper] 캐시 미스: cache={}, key={}", cacheName, key);
            // 캐시 미스 시 Mono 실행 후 결과 캐싱
            return monoSupplier.get()
                    .doOnNext(value -> {
                        if (value != null) {
                            cache.put(key, value);
                            log.debug("[ReactiveCacheHelper] 캐시 저장: cache={}, key={}", cacheName, key);
                        }
                    });
        });
    }

    /**
     * 특정 캐시에서 항목을 제거합니다.
     *
     * @param cacheName 캐시 이름
     * @param key 캐시 키
     */
    public void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("[ReactiveCacheHelper] 캐시 삭제: cache={}, key={}", cacheName, key);
        }
    }

    /**
     * 특정 캐시 전체를 비웁니다.
     *
     * @param cacheName 캐시 이름
     */
    public void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("[ReactiveCacheHelper] 캐시 전체 삭제: cache={}", cacheName);
        }
    }
}
