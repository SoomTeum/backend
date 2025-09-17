package com.comma.soomteum.domain.tour.service;

import com.comma.soomteum.domain.ai.adapter.AiServiceAdapter;
import com.comma.soomteum.domain.place.dto.KorService2Response;
import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.ai.dto.AiRecommendationRequest; // AI 요청 DTO 임포트
import com.comma.soomteum.domain.place.service.KorAreaService;
import com.comma.soomteum.domain.place.service.KorLocationService;
import com.comma.soomteum.domain.place.service.TatsCnctrService;
import com.comma.soomteum.domain.place.service.PlaceService;
import com.comma.soomteum.domain.theme.repository.ThemeRepository;
import com.comma.soomteum.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourService {

    private final KorLocationService korLocationService;
    private final KorAreaService korAreaService;
    private final TatsCnctrService tatsCnctrService;
    private final AiServiceAdapter aiServiceAdapter;
    private final ThemeRepository themeRepository;
    private final RegionRepository regionRepository;
    private final PlaceService placeService;

    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> locationPlaces(TourApiRequestDto.LocationBasedList2 request) {
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
                    finalDto.setQuietnessLevel(aiResponse.getQuietnessLevel());
                    
                    // 새로운 필드들 설정 (locationPlaces는 areaCode, sigunguCode가 없어서 null 전달)
                    setCatNameAndAreaInfo(finalDto, null, null);
                    
                    return finalDto;
                });
    }

    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> AreaPlaces(TourApiRequestDto.AreaBasedList2 request) {
        return korAreaService.areaBasedList(request)
                .flatMapMany(response -> Flux.fromIterable(response.getBody().getItems().getItem()))
                .flatMap(this::addCnctrRateToPlace)
                .collectList()
                .flatMap(candidateList -> Mono.fromCallable(() -> {

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

                    return aiServiceAdapter.createRankedRecommendations(aiRequestItems);
                }))
                .flatMapMany(Flux::fromIterable)
                .map(aiResponse -> {
                    TatsCnctrResponse.TatsCnctrResponseDto finalDto = new TatsCnctrResponse.TatsCnctrResponseDto();
                    finalDto.setTitle(aiResponse.getTitle());
                    finalDto.setContentid(aiResponse.getContentid());
                    finalDto.setCat1(aiResponse.getCat1());
                    finalDto.setCat2(aiResponse.getCat2());
                    finalDto.setFirstimage(aiResponse.getFirstimage());
                    finalDto.setDist(aiResponse.getDist());
                    finalDto.setCnctrRate(aiResponse.getCnctrRate());
                    finalDto.setQuietnessLevel(aiResponse.getQuietnessLevel());
                    
                    // 새로운 필드들 설정
                    setCatNameAndAreaInfo(finalDto, request.getAreaCode(), request.getSigunguCode());
                    
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
                    
                    // 새로운 필드들 설정 (addCnctrRateToPlace는 areaCode, sigunguCode가 없어서 null 전달)
                    setCatNameAndAreaInfo(newDto, null, null);
                    
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
                    
                    // 새로운 필드들 설정 (오류 케이스에서도 catName만 설정)
                    setCatNameAndAreaInfo(errorDto, null, null);
                    
                    return Mono.just(errorDto);
                });
    }

    private void setCatNameAndAreaInfo(TatsCnctrResponse.TatsCnctrResponseDto dto, Integer areaCode, Integer sigunguCode) {
        // catName 설정
        if (dto.getCat1() != null && dto.getCat2() != null) {
            themeRepository.findByCat1AndCat2(dto.getCat1(), dto.getCat2())
                    .ifPresent(theme -> dto.setCatName(theme.getName()));
        }
        
        // likeCount 설정
        if (dto.getContentid() != null) {
            placeService.findByContentId(dto.getContentid())
                    .ifPresent(place -> dto.setLikeCount(place.getLikeCount()));
        }
        
        // areaCode, sigunguCode, areaName 설정
        if (areaCode != null && sigunguCode != null) {
            dto.setAreaCode(areaCode);
            dto.setSigunguCode(sigunguCode);
            
            // areaName 설정
            regionRepository.findByKorAreaCodeAndKorSigunguCode(
                    String.valueOf(areaCode), 
                    String.valueOf(sigunguCode)
            ).ifPresent(region -> dto.setAreaName(region.getName()));
        }
    }
}