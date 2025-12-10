package com.comma.soomteum.domain.recommendation.controller;

import com.comma.soomteum.domain.ai.adapter.AiServiceAdapter;
import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import com.comma.soomteum.domain.recommendation.service.TourService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "여행지 추천", description = "위치/지역 기반 추천 API")
@RestController
@RequestMapping(path = "/api/places", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class PlaceRecommendationController {
    private final AiServiceAdapter aiServiceAdapter;
    private final TourService recommendPlacesService;

    @Operation(
            summary = "위치 기반 여행지 추천",
            description = "경도·위도와 반경을 기준으로 검색한 여행지에 한적함 점수(cnctrRate)를 포함해 추천합니다. "
                    + "cat1/cat2로 분류 필터링 가능하며, pageNo/numOfRows, arrange로 페이징 및 정렬을 설정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = TatsCnctrResponse.TatsCnctrResponseDto.class)))
            }
    )
    @GetMapping("/ai")
    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> locationRecommendPlaces(
            @Valid @ParameterObject TourApiRequestDto.LocationBasedList2 req
    ) {
        return recommendPlacesService.locationPlaces(req);
    }

    @Operation(
            summary = "지역 기반 여행지 추천",
            description = "지역·테마 조건으로 검색하고, 한적함 점수(cnctrRate)와 AI 추천 순서를 포함해 반환합니다. "
                    + "keyword, cat1, cat2 필터 및 arrange 옵션(A: AI 추천순, C: 한적함순 등)으로 정렬을 제어합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = TatsCnctrResponse.TatsCnctrResponseDto.class)))
            }
    )
    @GetMapping("")
    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> areaRecommendPlaces(
            @Valid @ParameterObject TourApiRequestDto.AreaBasedList2 req
    ) {
        return recommendPlacesService.AreaPlaces(req);
    }
}
