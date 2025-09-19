package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.domain.place.dto.KorService2Response;
import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.region.entity.Region;
import com.comma.soomteum.domain.region.entity.RegionCnctr;
import com.comma.soomteum.domain.region.repository.RegionCnctrRepository;
import com.comma.soomteum.domain.region.repository.RegionRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TatsCnctrService {

    private final RegionRepository regionRepository;
    private final RegionCnctrRepository regionCnctrRepository;
    private final TatsCnctrApiCaller caller;

    private static final String PATH = "/tatsCnctrRatedList";

    private static final Duration PER_CALL_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration OVERALL_TIMEOUT = Duration.ofSeconds(10);

    @Cacheable(
            cacheNames = "tatsCnctr",
            key = "T(java.util.Objects).hash(#req.getAreacode(), #req.getSigungucode(), #req.getTitle(), #req.getPageNo(), #req.getNumOfRows())"
    )
    public Mono<TatsCnctrResponse.TatsCnctrResponseDto> getCnctrRate(
            KorService2Response.LocationBasedListResponseDto req) {

        return Mono.defer(() -> {
                    log.info("[TatsCnctr] 혼잡도 조회 시작: title='{}', area={}, sigungu={}",
                            req.getTitle(), req.getAreacode(), req.getSigungucode());

                    return Mono.fromCallable(() -> {
                                    String areaCode = req.getAreacode();
                                    String sigunguCode = req.getSigungucode();

                                    log.debug("[TatsCnctr] 입력 코드: areacode='{}', sigungucode='{}'", areaCode, sigunguCode);

                                    // 1. 먼저 정확한 area/sigungu 코드로 조회
                                    var region = regionRepository.findByKorAreaCodeAndKorSigunguCode(areaCode, sigunguCode);
                                    if (region.isPresent()) {
                                        log.debug("[TatsCnctr] 정확한 매칭 성공: {}", region.get().getName());
                                        return region.get();
                                    }

                                    // 2. area 코드만으로 조회 시도
                                    log.warn("[TatsCnctr] 정확한 지역 코드 매칭 실패, area 코드만으로 재시도: area={}, sigungu={}",
                                            areaCode, sigunguCode);

                                    var regionByArea = regionRepository.findByKorAreaCode(areaCode);
                                    if (regionByArea.isPresent()) {
                                        log.info("[TatsCnctr] area 코드로 지역 찾음: {}", regionByArea.get().getName());
                                        return regionByArea.get();
                                    }

                                    // 3. 코드 변환 시도 (예: "32" -> "1", "01" -> "1" 등)
                                    String normalizedAreaCode = normalizeAreaCode(areaCode);
                                    String normalizedSigunguCode = normalizeSigunguCode(sigunguCode);

                                    if (!areaCode.equals(normalizedAreaCode) || !sigunguCode.equals(normalizedSigunguCode)) {
                                        log.info("[TatsCnctr] 코드 정규화 시도: {}→{}, {}→{}",
                                                areaCode, normalizedAreaCode, sigunguCode, normalizedSigunguCode);

                                        var normalizedRegion = regionRepository.findByKorAreaCodeAndKorSigunguCode(
                                                normalizedAreaCode, normalizedSigunguCode);
                                        if (normalizedRegion.isPresent()) {
                                            log.info("[TatsCnctr] 정규화 코드로 지역 찾음: {}", normalizedRegion.get().getName());
                                            return normalizedRegion.get();
                                        }

                                        // area 코드만으로도 시도
                                        var normalizedRegionByArea = regionRepository.findByKorAreaCode(normalizedAreaCode);
                                        if (normalizedRegionByArea.isPresent()) {
                                            log.info("[TatsCnctr] 정규화 area 코드로 지역 찾음: {}", normalizedRegionByArea.get().getName());
                                            return normalizedRegionByArea.get();
                                        }
                                    }

                                    // 4. 모든 시도 실패
                                    log.error("[TatsCnctr] 지역 코드 조회 완전 실패: area={}, sigungu={}",
                                            areaCode, sigunguCode);
                                    throw new CustomException(ErrorCode.REGION_NOT_FOUND);
                            })
                            .subscribeOn(Schedulers.boundedElastic())

                            .flatMap(region -> {
                                return Mono.fromCallable(() ->
                                                regionCnctrRepository.findAllByRegionOrderByPriorityAsc(region)
                                        )
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .map(rcs -> rcs.stream()
                                                .map(RegionCnctr::getCnctrSigunguCode)
                                                .filter(Objects::nonNull)
                                                .map(String::trim)
                                                .filter(s -> !s.isEmpty())
                                                .distinct()
                                                .toList()
                                        )
                                        .publishOn(Schedulers.parallel())
                                        .flatMapMany(codes -> {
                                            if (codes.isEmpty()) {
                                                return Flux.empty();
                                            }
                                            return Flux.fromIterable(codes)
                                                    .concatMap(code -> callOnce(region.getCnctrAreaCode(), code, req), 1)
                                                    .filter(dto -> dto.getCnctrRate() != null);
                                        })
                                        .next()
                                        .switchIfEmpty(Mono.fromSupplier(() -> {
                                            log.warn("[TatsCnctr] 모든 후보 코드로 조회 실패 → -1 반환");
                                            var d = new TatsCnctrResponse.TatsCnctrResponseDto();
                                            d.setCnctrRate("-1");
                                            return d;
                                        }));
                            });
                })
                .timeout(OVERALL_TIMEOUT)
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("[TatsCnctr] 전체 타임아웃 초과({}s) → -2 반환",
                            OVERALL_TIMEOUT.toSeconds(), e);
                    var d = new TatsCnctrResponse.TatsCnctrResponseDto();
                    d.setCnctrRate("-2");
                    return Mono.just(d);
                })
                .onErrorResume(e -> {
                    log.error("[TatsCnctr] 처리 중 예외 발생 → -2 반환", e);
                    var d = new TatsCnctrResponse.TatsCnctrResponseDto();
                    d.setCnctrRate("-2");
                    return Mono.just(d);
                });
    }

    private Mono<TatsCnctrResponse.TatsCnctrResponseDto> callOnce(
            String cnctrAreaCode,
            String cnctrSigunguCode,
            KorService2Response.LocationBasedListResponseDto req) {

        return caller.get(PATH, b -> {
                    b.queryParam("pageNo", req.getPageNo())
                            .queryParam("numOfRows", req.getNumOfRows());
                    qpIfPresent(b, "areaCd", cnctrAreaCode);
                    qpIfPresent(b, "signguCd", cnctrSigunguCode);
                    qpIfPresent(b, "tAtsNm", req.getTitle());
                }, TatsCnctrResponse.class)

                .doOnSubscribe(s -> log.info("[TatsCnctr] API 호출: areaCd={}, sigunguCd={}, title='{}'",
                        cnctrAreaCode, cnctrSigunguCode, req.getTitle()))

                .map(response -> {
                    var items = (response.getBody() != null
                            && response.getBody().getItems() != null)
                            ? response.getBody().getItems().getItem() : null;

                    if (items != null && !items.isEmpty()) {
                        return items.get(0);
                    }

                    log.warn("[TatsCnctr] API 응답은 성공했으나 items 비어있음");
                    var d = new TatsCnctrResponse.TatsCnctrResponseDto();
                    d.setCnctrRate("-1");
                    return d;
                })
                .timeout(PER_CALL_TIMEOUT)
                .doOnError(e -> log.error("[TatsCnctr] API 호출 에러: areaCd={}, sigunguCd={}, title='{}'",
                        cnctrAreaCode, cnctrSigunguCode, req.getTitle(), e))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(300))
                                .filter(TatsCnctrService::isRetryable)
                                .doBeforeRetry(rs -> log.warn(
                                        "[TatsCnctr] API 재시도 attempt={} (areaCd={}, sigunguCd={}, title='{}')",
                                        rs.totalRetries() + 1, cnctrAreaCode, cnctrSigunguCode, req.getTitle()
                                ))
                );
    }

    private static boolean isRetryable(Throwable t) {
        if (t instanceof WebClientResponseException w) {
            int s = w.getRawStatusCode();
            return s == 429 || (s >= 500 && s < 600);
        }
        return t instanceof IOException || t instanceof TimeoutException;
    }

    private static void qpIfPresent(UriBuilder b, String name, String value) {
        if (value != null && !value.isBlank()) b.queryParam(name, value);
    }

    /**
     * 지역 코드를 정규화합니다.
     * 예: "32" -> "1", "01" -> "1"
     */
    private String normalizeAreaCode(String areaCode) {
        if (areaCode == null) return null;

        // 서울: 1
        if ("01".equals(areaCode)) return "1";
        // 인천: 2 (실제 매핑은 데이터에 따라 조정 필요)
        if ("02".equals(areaCode)) return "2";
        // 강원: 32 -> 1 (예시, 실제 데이터에 맞게 조정)
        if ("32".equals(areaCode)) return "1";

        // 기본적으로 앞의 0을 제거하고 반환
        return areaCode.replaceFirst("^0+", "");
    }

    /**
     * 시군구 코드를 정규화합니다.
     */
    private String normalizeSigunguCode(String sigunguCode) {
        if (sigunguCode == null) return null;

        // 기본적으로 앞의 0을 제거하고 반환
        return sigunguCode.replaceFirst("^0+", "");
    }
}
