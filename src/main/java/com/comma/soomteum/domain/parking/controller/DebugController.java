package com.comma.soomteum.domain.parking.controller;

import com.comma.soomteum.domain.parking.repository.PublicParkingRepository;
import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "디버그- parking", description = "데이터 확인용 API (개발환경에서만 사용)")
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Profile("!prod") // 운영환경에서는 접근 불가
public class DebugController {

    private final PlaceRepository placeRepository;
    private final PublicParkingRepository publicParkingRepository;

    @Operation(summary = "데이터 개수 확인", description = "테스트 데이터가 제대로 생성되었는지 확인합니다.")
    @GetMapping("/counts")
    public ApiResponse<Map<String, Long>> getDataCounts() {
        Map<String, Long> counts = Map.of(
            "places", placeRepository.count(),
            "parkingLots", publicParkingRepository.count()
        );
        return ApiResponse.ok(counts);
    }

    @Operation(summary = "모든 여행지 조회", description = "등록된 모든 여행지를 조회합니다.")
    @GetMapping("/places")
    public ApiResponse<?> getAllPlaces() {
        return ApiResponse.ok(placeRepository.findAll());
    }

    @Operation(summary = "모든 주차장 조회", description = "등록된 모든 주차장을 조회합니다.")
    @GetMapping("/parking")
    public ApiResponse<?> getAllParking() {
        return ApiResponse.ok(publicParkingRepository.findAll());
    }
}