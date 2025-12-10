package com.comma.soomteum.domain.parking.controller;

import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.service.PublicParkingService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "공영주차장", description = "공영주차장 정보 조회 API")
@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final PublicParkingService publicParkingService;

    @Operation(summary = "지역별 공영주차장 조회", description = "특정 지역의 모든 공영주차장을 조회합니다.")
    @GetMapping("/region/{regionCode}")
    public ApiResponse<List<PublicParkingResponseDto>> getParkingByRegion(
            @Parameter(description = "지역 코드 (강릉시: 32230)") @PathVariable String regionCode) {
        
        List<PublicParkingResponseDto> parkingLots = publicParkingService.findByRegion(regionCode);
        return ApiResponse.ok(parkingLots);
    }

    @Operation(summary = "주변 공영주차장 조회", description = "특정 좌표 주변의 공영주차장을 거리 순으로 조회합니다.")
    @GetMapping("/nearby")
    public ApiResponse<List<PublicParkingResponseDto>> getNearbyParking(
            @Parameter(description = "위도", example = "37.7519") @RequestParam BigDecimal latitude,
            @Parameter(description = "경도", example = "128.8761") @RequestParam BigDecimal longitude,
            @Parameter(description = "지역 코드", example = "32230") @RequestParam String regionCode,
            @Parameter(description = "조회 개수", example = "5") @RequestParam(defaultValue = "5") int limit) {
        
        List<PublicParkingResponseDto> nearbyParking = publicParkingService.findNearbyParking(
                latitude, longitude, regionCode, limit);
        return ApiResponse.ok(nearbyParking);
    }

    @Operation(summary = "주차장 실시간 정보 업데이트", description = "공영주차장 API에서 실시간 정보를 가져와 업데이트합니다.")
    @PostMapping("/update")
    public ApiResponse<String> updateParkingInfo() {
        publicParkingService.updateParkingAvailability();
        return ApiResponse.ok("주차장 정보가 업데이트되었습니다.");
    }
}