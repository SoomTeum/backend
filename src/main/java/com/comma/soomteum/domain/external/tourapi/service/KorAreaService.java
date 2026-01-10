package com.comma.soomteum.domain.external.tourapi.service;

import com.comma.soomteum.domain.external.tourapi.dto.KorService2Response;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.comma.soomteum.domain.external.tourapi.service.KorApiCaller.qpIfPresent;

@Service
@RequiredArgsConstructor
public class KorAreaService {

    private static final String PATH = "/areaBasedList2";
    private final KorApiCaller caller;

    public Mono<KorService2Response> areaBasedList(TourApiRequestDto.AreaBasedList2 req) {
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
}
