package com.comma.soomteum.domain.tour.service;

import com.comma.soomteum.domain.place.dto.KorService2Response;
import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.place.service.KorLocationService;
import com.comma.soomteum.domain.place.service.TatsCnctrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RecommendPlacesService {

    private final KorLocationService korLocationService;
    private final TatsCnctrService tatsCnctrService;

    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> recommendPlaces(TourApiRequestDto.LocationBasedList2 request) {
        return korLocationService.locationBasedList(request)
                .flatMapMany(response -> Flux.fromIterable(response.getBody().getItems().getItem()))
                .flatMap(this::addCnctrRateToPlace);
    }

    private Mono<TatsCnctrResponse.TatsCnctrResponseDto> addCnctrRateToPlace(KorService2Response.LocationBasedListResponseDto place) {
        return tatsCnctrService.getCnctrRate(place)
                .map(tatsCnctrDto -> {
                    TatsCnctrResponse.TatsCnctrResponseDto newDto = new TatsCnctrResponse.TatsCnctrResponseDto();
                    newDto.setTitle(place.getTitle());
                    newDto.setContentid(place.getContentid());
                    newDto.setCat1(place.getCat1());
                    newDto.setCat2(place.getCat2());
                    newDto.setFirstimage(place.getFirstimage());
                    newDto.setDist(place.getDist());
                    newDto.setCnctrRate(tatsCnctrDto.getCnctrRate());
                    return newDto;
                })
                .onErrorResume(e -> {
                    TatsCnctrResponse.TatsCnctrResponseDto errorDto = new TatsCnctrResponse.TatsCnctrResponseDto();
                    errorDto.setTitle(place.getTitle());
                    errorDto.setContentid(place.getContentid());
                    errorDto.setCat1(place.getCat1());
                    errorDto.setCat2(place.getCat2());
                    errorDto.setFirstimage(place.getFirstimage());
                    errorDto.setDist(place.getDist());
                    errorDto.setCnctrRate("-1"); // Default value for error
                    return Mono.just(errorDto);
                });
    }
}