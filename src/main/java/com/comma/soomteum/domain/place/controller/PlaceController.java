package com.comma.soomteum.domain.place.controller;

import com.comma.soomteum.domain.place.dto.response.PlaceDetailIntegratedResponseDto;
import com.comma.soomteum.domain.place.service.PlaceDetailIntegratedService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "여행지 상세", description = "여행지 통합 상세 조회 전용 API")
@RestController
@RequestMapping(path = "/api/places", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceDetailIntegratedService placeDetailIntegratedService;

    @Operation(
            summary = "여행지 통합 상세 조회",
            description = "여행지 상세 화면에서 필요한 정보를 한 번에 제공합니다. "
                    + "기본 프로필(이름, 사진, 주소, 지역, 테마, 소개), 한적함 등급, 좋아요 수, AI 꿀팁(강릉시), 근처 공영주차장(강릉시)까지 통합 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "통합 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = PlaceDetailIntegratedResponseDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "여행지를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    @GetMapping("/integrated/{contentId}")
    public Mono<ApiResponse<PlaceDetailIntegratedResponseDto>> getIntegratedPlaceDetail(
            @Parameter(description = "공공데이터 API의 컨텐츠 ID", required = true, example = "128758")
            @PathVariable String contentId) {

        return placeDetailIntegratedService.getIntegratedPlaceDetail(contentId)
                .map(ApiResponse::ok)
                .onErrorReturn(new ApiResponse<>(null, false, null, null));
    }
}
