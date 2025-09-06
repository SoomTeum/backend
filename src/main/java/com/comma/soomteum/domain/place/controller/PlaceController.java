package com.comma.soomteum.domain.place.controller;

import com.comma.soomteum.domain.place.dto.response.PlaceDetailWithParkingDto;
import com.comma.soomteum.domain.place.service.PlaceService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "여행지", description = "여행지와 관련된 로직")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "여행지 상세 조회", description = "여행지 상세 정보와 주변 공영주차장 정보를 함께 조회합니다.")
    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDetailWithParkingDto> getPlaceDetail(
            @Parameter(description = "여행지 ID") @PathVariable Long placeId) {
        
        PlaceDetailWithParkingDto placeDetail = placeService.getPlaceDetailWithParking(placeId);
        return ApiResponse.ok(placeDetail);
    }

    @Operation(summary = "여행지 상세 조회 (가장 가까운 주차장)", description = "contentId로 여행지를 조회하고 가장 가까운 공영주차장 1개의 실시간 정보를 제공합니다.")
    @GetMapping("/content/{contentId}")
    public ApiResponse<PlaceDetailWithParkingDto> getPlaceDetailByContentId(
            @Parameter(description = "여행지 contentId", example = "264670") @PathVariable String contentId) {
        
        PlaceDetailWithParkingDto placeDetail = placeService.getPlaceDetailWithNearestParking(contentId);
        return ApiResponse.ok(placeDetail);
    }
}
