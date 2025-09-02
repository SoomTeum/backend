package com.comma.soomteum.domain.tour.controller;

import com.comma.soomteum.domain.place.dto.TatsCnctrResponse;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.tour.service.RecommendPlacesService;
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

@Tag(name = "Recommended Places", description = "한적한 장소 추천 API")
@RestController
@RequestMapping(path = "/api/places", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class RecommendPlacesController {

    private final RecommendPlacesService recommendPlacesService;

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
    @GetMapping("")
    public Flux<TatsCnctrResponse.TatsCnctrResponseDto> recommendPlaces(
            @Valid @ParameterObject TourApiRequestDto.LocationBasedList2 req
    ) {
        return recommendPlacesService.recommendPlaces(req);
    }
}