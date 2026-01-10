package com.comma.soomteum.domain.external.tourapi.service;

import com.comma.soomteum.domain.external.tourapi.dto.response.PlaceDetailResponseDto;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KorDetailService {

    private static final String PATH = "/detailCommon2";
    private final KorApiCaller caller;

    public Mono<PlaceDetailResponseDto> getDetail(TourApiRequestDto.DetailCommon2 req) {
        return caller.get(PATH, b -> {
            b.queryParam("contentId", req.getContentId())
                    .queryParam("pageNo", req.pageNoOrDefault())
                    .queryParam("numOfRows", req.rowsOrDefault());
        }, PlaceDetailResponseDto.class);
    }
}
