package com.comma.soomteum.domain.region.controller;

import com.comma.soomteum.domain.region.dto.RegionGroupResponseDto;
import com.comma.soomteum.domain.region.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Region", description = "지역 관리 API")
@RestController
@RequestMapping("/api/places/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "전체 지역 목록 조회", description = "지역코드별로 그룹핑된 모든 지역 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<RegionGroupResponseDto>> getAllRegions() {
        List<RegionGroupResponseDto> regions = regionService.getAllRegions();
        return ResponseEntity.ok(regions);
    }
}