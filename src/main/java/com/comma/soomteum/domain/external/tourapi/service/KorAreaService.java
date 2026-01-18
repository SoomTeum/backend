package com.comma.soomteum.domain.external.tourapi.service;

import com.comma.soomteum.config.CacheConfig;
import com.comma.soomteum.config.ReactiveCacheHelper;
import com.comma.soomteum.domain.external.tourapi.dto.KorService2Response;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.comma.soomteum.domain.external.tourapi.service.KorApiCaller.qpIfPresent;

@Service
@RequiredArgsConstructor
@Slf4j
public class KorAreaService {

    private static final String PATH = "/areaBasedList2";
    private final KorApiCaller caller;
    private final ReactiveCacheHelper cacheHelper;

    /**
     * 지역 기반 관광정보 목록 조회 (캐시 적용)
     *
     * 캐시 키: areaCode:sigunguCode:cat1:cat2:pageNo:numOfRows
     * TTL: 1시간
     */
    public Mono<KorService2Response> areaBasedList(TourApiRequestDto.AreaBasedList2 req) {
        String cacheKey = buildCacheKey(req);

        return cacheHelper.cacheMono(
                CacheConfig.TOUR_API_CACHE,
                cacheKey,
                () -> fetchAreaBasedList(req)
        );
    }

    /**
     * 실제 Tour API 호출
     */
    private Mono<KorService2Response> fetchAreaBasedList(TourApiRequestDto.AreaBasedList2 req) {
        log.info("[KorAreaService] Tour API 호출: areaCode={}, sigunguCode={}, cat1={}, cat2={}",
                req.getAreaCode(), req.getSigunguCode(), req.getCat1(), req.getCat2());

        return caller.get(PATH, b -> {
            b.queryParam("pageNo", req.pageNoOrDefault())
                    .queryParam("numOfRows", req.rowsOrDefault());
            // arrange (A/C/D 또는 O/Q/R)
            if (req.arrangeOrDefault() != null) {
                b.queryParam("arrange", req.arrangeOrDefault());
            }
            // 선택 파라미터
            qpIfPresent(b, "areaCode", req.getAreaCode());
            qpIfPresent(b, "sigunguCode", req.getSigunguCode());
            qpIfPresent(b, "cat1", req.getCat1());
            qpIfPresent(b, "cat2", req.getCat2());
        }, KorService2Response.class);
    }

    /**
     * 캐시 키 생성
     * 형식: areaCode:sigunguCode:cat1:cat2:pageNo:numOfRows
     */
    private String buildCacheKey(TourApiRequestDto.AreaBasedList2 req) {
        return String.format("%s:%s:%s:%s:%d:%d",
                nvl(req.getAreaCode()),
                nvl(req.getSigunguCode()),
                nvl(req.getCat1()),
                nvl(req.getCat2()),
                req.pageNoOrDefault(),
                req.rowsOrDefault());
    }

    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
