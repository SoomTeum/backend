package com.comma.soomteum.domain.tour.controller;

import com.comma.soomteum.domain.ai.adapter.AiServiceAdapter;
import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.tour.service.TourService;
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

@Tag(name = "AI & Area Recommended Places", description = "최종 장소 추천 API 2개")
@RestController
@RequestMapping(path = "/api/places", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class TourController {
    private final AiServiceAdapter aiServiceAdapter;
    private final TourService recommendPlacesService;

    @Operation(
            summary = "위치 기반 한적한 장소 추천",
            description = """
                    주어진 위치(경도, 위도)와 반경을 기준으로 장소를 검색하고, 각 장소의 한적함 점수(cnctrRate)를 포함하여 반환합니다.
                    - `cnctrRate`는 관광객 혼잡도를 나타내는 지수로, -1은 데이터가 없음을 의미합니다.
                    - 정렬(arrange) 기본값: O=제목순, Q=수정일순, R=등록일순, S=거리순
                    - `cat1`, `cat2`를 통해 장소 분류 코드로 필터링할 수 있습니다.
                    """,
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
            summary = "지역 기반 한적한 장소 추천",
            description = """
                    각 장소의 한적함 점수(cnctrRate)를 포함하여 반환합니다.

                    **검색 기능:**
                    - `keyword`: 장소명으로 검색 (부분 일치, 대소문자 무관)
                    - 검색어가 없으면 전체 결과 반환

                    **정렬 옵션:**
                    - `arrange=A`: AI 추천순 (기본값) - 종합적인 추천 알고리즘 사용
                    - `arrange=C`: 한적함순 - cnctrRate 높은 순으로 정렬 (AI 정렬 건너뜀)
                    - `arrange=Q`: 수정일순
                    - `arrange=R`: 등록일순

                    **기타 필터:**
                    - `cnctrRate`: 관광객 혼잡도 지수 (-1은 데이터 없음)
                    - `cat1`, `cat2`: 장소 분류 코드로 필터링

                    **예시:**
                    - `/api/places?areaCode=1&keyword=바다` - "바다" 포함 장소 검색
                    - `/api/places?areaCode=1&arrange=C` - 한적함순 정렬
                    - `/api/places?areaCode=1&keyword=카페&arrange=C` - "카페" 검색 + 한적함순
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = TatsCnctrResponse.TatsCnctrResponseDto.class)))
            }
    )
    @GetMapping("")
    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> AreaRecommendPlaces(
            @Valid @ParameterObject TourApiRequestDto.AreaBasedList2 req
    ) {
        return recommendPlacesService.AreaPlaces(req);
    }



}