package com.comma.soomteum.domain.tour.service;

import com.comma.soomteum.domain.ai.adapter.AiServiceAdapter;
import com.comma.soomteum.domain.place.dto.KorService2Response;
import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.ai.dto.AiRecommendationRequest; // AI 요청 DTO 임포트
import com.comma.soomteum.domain.place.service.KorLocationService;
import com.comma.soomteum.domain.place.service.TatsCnctrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendPlacesService {

    private final KorLocationService korLocationService;
    private final TatsCnctrService tatsCnctrService;
    private final AiServiceAdapter aiServiceAdapter;

    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> recommendPlaces(TourApiRequestDto.LocationBasedList2 request) {
        return korLocationService.locationBasedList(request)
                .flatMapMany(response -> Flux.fromIterable(response.getBody().getItems().getItem()))
                .flatMap(this::addCnctrRateToPlace)
                // 1. 모든 후보지 데이터를 하나의 리스트로 수집
                .collectList()
                // 2. 수집된 리스트를 AI 엔진에 전달하여 재정렬
                .flatMap(candidateList -> Mono.fromCallable(() -> {

                    // 3. AI 서비스의 입력 DTO(AiRecommendationRequest) 리스트로 변환
                    List<AiRecommendationRequest> aiRequestItems = candidateList.stream()
                            .map(dto -> new AiRecommendationRequest(
                                    dto.getTitle(),
                                    dto.getContentid(),
                                    dto.getCat1(),
                                    dto.getCat2(),
                                    dto.getFirstimage(),
                                    dto.getDist(),
                                    dto.getCnctrRate()
                            ))
                            .collect(Collectors.toList());

                    // 4. AI 서비스 호출하여 정렬된 결과 (AiRecommendationResponse 리스트) 받기
                    return aiServiceAdapter.createRankedRecommendations(aiRequestItems);
                }))
                // 5. 정렬한 최종 리스트를 다시 스트림(Flux)으로 변환
                .flatMapMany(Flux::fromIterable)
                // 6. AI 출력 DTO(AiRecommendationResponse)를 최종 DTO(TatsCnctrResponseDto)로 변환
                .map(aiResponse -> {
                    TatsCnctrResponse.TatsCnctrResponseDto finalDto = new TatsCnctrResponse.TatsCnctrResponseDto();
                    finalDto.setTitle(aiResponse.getTitle());
                    finalDto.setContentid(aiResponse.getContentid());
                    finalDto.setCat1(aiResponse.getCat1());
                    finalDto.setCat2(aiResponse.getCat2());
                    finalDto.setFirstimage(aiResponse.getFirstimage());
                    finalDto.setDist(aiResponse.getDist());
                    finalDto.setCnctrRate(aiResponse.getCnctrRate());
                    finalDto.setCongestionLevel(aiResponse.getCongestionLevel());
                    return finalDto;
                });
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
                    errorDto.setCnctrRate("-2"); // Default value for error
                    return Mono.just(errorDto);
                });
    }
}