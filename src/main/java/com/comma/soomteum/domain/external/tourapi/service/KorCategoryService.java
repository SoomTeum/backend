package com.comma.soomteum.domain.external.tourapi.service;

import com.comma.soomteum.domain.external.tourapi.dto.CategoryCodeResponse;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.comma.soomteum.domain.external.tourapi.service.KorApiCaller.qpIfPresent;

@Service
@RequiredArgsConstructor
public class KorCategoryService {

    private static final String PATH = "/categoryCode2";
    private final KorApiCaller caller;

    public Mono<CategoryCodeResponse> getCategoryCode(TourApiRequestDto.CategoryCode req) {
        return caller.get(PATH, b -> {
            b.queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100);
            // 선택 파라미터
            qpIfPresent(b, "cat1", req.getCat1());
            qpIfPresent(b, "cat2", req.getCat2());
        }, CategoryCodeResponse.class);
    }
}
