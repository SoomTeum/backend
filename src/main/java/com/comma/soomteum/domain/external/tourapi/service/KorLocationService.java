package com.comma.soomteum.domain.external.tourapi.service;

import com.comma.soomteum.domain.external.tourapi.dto.KorService2Response;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.comma.soomteum.domain.external.tourapi.service.KorApiCaller.qpIfPresent;

@Service
@RequiredArgsConstructor
public class KorLocationService {

    private static final String PATH = "/locationBasedList2";
    private final KorApiCaller caller;

    public Mono<KorService2Response> locationBasedList(TourApiRequestDto.LocationBasedList2 req) {
        return caller.get(PATH, b -> {
            b.queryParam("mapX", req.getMapX())
                    .queryParam("mapY", req.getMapY())
                    .queryParam("radius", req.getRadius())
                    .queryParam("pageNo", req.pageNoOrDefault())
                    .queryParam("numOfRows", req.rowsOrDefault())
                    .queryParam("arrange", req.arrangeOrDefault());
            qpIfPresent(b, "cat1", req.getCat1());
            qpIfPresent(b, "cat2", req.getCat2());
        }, KorService2Response.class);
    }
}
