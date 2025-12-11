package com.comma.soomteum.domain.external.tourapi;

import com.comma.soomteum.domain.external.tourapi.dto.KorService2Response;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import com.comma.soomteum.domain.external.tourapi.dto.response.PlaceDetailResponseDto;
import com.comma.soomteum.domain.external.tourapi.service.KorAreaService;
import com.comma.soomteum.domain.external.tourapi.service.KorDetailService;
import com.comma.soomteum.domain.external.tourapi.service.KorLocationService;
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

@Tag(name = "Debug - TourAPI", description = "한국관광공사 TourAPI 응답 구조 확인·디버그용 (개발 환경 전용)")
@RestController
@RequestMapping(path = "/api/debug/tour", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class TourApiDebugController {

    private final KorAreaService areaService;
    private final KorLocationService locationService;
    private final KorDetailService detailService;

    @Operation(
            summary = "위치 기반 관광정보 원본 확인",
            description = "공공데이터 TourAPI(locationBasedList2)를 호출해 경도·위도와 반경 기준 원본 응답을 반환합니다. "
                    + "디버그 용도로 cat1/cat2, pageNo/numOfRows, arrange 옵션을 그대로 전달합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "성공",
                            content = @Content(schema = @Schema(implementation = KorService2Response.class))),
            }
    )
    @GetMapping("/location-based")
    public Mono<KorService2Response> locationBasedList(
            @Valid @ParameterObject TourApiRequestDto.LocationBasedList2 req
    ) {
        return locationService.locationBasedList(req);
    }

    @Operation(
            summary = "지역 기반 관광정보 원본 확인",
            description = "지역·테마 기반으로 TourAPI(areaBasedList2)를 호출한 원본 응답을 반환합니다. "
                    + "cat1/cat2, pageNo/numOfRows, arrange 옵션을 그대로 전달해 디버그에 활용합니다.",
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
            summary = "여행지 공통정보 원본 확인",
            description = "contentId로 TourAPI(detailCommon2)를 호출한 원본 데이터에서 기본 필드만 추려 반환합니다."
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
