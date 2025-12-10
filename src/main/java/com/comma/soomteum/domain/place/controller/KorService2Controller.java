package com.comma.soomteum.domain.place.controller;

import com.comma.soomteum.domain.place.dto.response.PlaceDetailResponseDto;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.place.dto.KorService2Response;
import com.comma.soomteum.domain.place.service.KorAreaService;
import com.comma.soomteum.domain.place.service.KorDetailService;
import com.comma.soomteum.domain.place.service.KorLocationService;
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
import reactor.core.publisher.Mono;

@Tag(name = "디버그- KorService2", description = "한국관광공사 데이터 확인용 API (개발환경에서만 사용)")
@RestController
@RequestMapping(path = "/api/debug", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class KorService2Controller {

    private final KorAreaService areaService;
    private final KorLocationService locationService;
    private final KorDetailService detailService;

    @Operation(
            summary = "위치기반 관광정보 조회 (/locationBasedList2)",
            description = "경도(mapX)·위도(mapY)와 반경(radius) 기준으로 관광정보를 조회합니다. "
                    + "옵션: cat1/cat2(분류), pageNo/numOfRows(페이징), arrange(정렬; 기본값 서비스 내부 적용).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = KorService2Response.class))),
            }
    )
    @GetMapping("/location-based")
    public Mono<KorService2Response> locationBasedList(
            @Valid @ParameterObject TourApiRequestDto.LocationBasedList2 req
    ) {
        // DTO 내부의 pageNoOrDefault, rowsOrDefault, arrangeOrDefault 등을
        // 서비스에서 사용 가능 (컨트롤러에서는 그대로 전달)
        return locationService.locationBasedList(req);
    }

    @Operation(
            summary = "지역기반 관광정보 조회 (/areaBasedList2)",
            description = "지역과 테마를 기반으로 관광지를 조회합니다. "
                    + " 옵션: cat1/cat2(분류), pageNo/numOfRows(페이징), arrange(정렬; 기본값 서비스 내부 적용)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = KorService2Response.class))),
            }
    )
    @GetMapping(path = "/area-based")
    public Mono<KorService2Response> areaBasedList(
            @Valid @ParameterObject TourApiRequestDto.AreaBasedList2 req
    ) {
        return areaService.areaBasedList(req);
    }

    @Operation(
            summary = "여행지 공통정보 조회 (/detailCommon2)",
            description = "contentId로 공통정보를 조회하고, 이름/대표이미지/위치/주소/소개만 추려 반환합니다."
    )
    @ApiResponse(responseCode = "200", description = "성공",
            content = @Content(schema = @Schema(implementation = PlaceDetailResponseDto.class)))

    @GetMapping("/detail")
    public Mono<PlaceDetailResponseDto> detail(
            @Valid @ParameterObject TourApiRequestDto.DetailCommon2 req
    ) {
        return detailService.getDetail(req);
    }
}
