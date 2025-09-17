package com.comma.soomteum.domain.theme.controller;

import com.comma.soomteum.domain.theme.dto.ThemeGroupResponseDto;
import com.comma.soomteum.domain.theme.service.ThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Theme", description = "테마 관리 API")
@RestController
@RequestMapping("/api/places/themes")
@RequiredArgsConstructor
public class ThemeController {

    private final ThemeService themeService;

    @Operation(summary = "전체 테마 목록 조회", description = "대분류별로 그룹핑된 모든 테마 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ThemeGroupResponseDto>> getAllThemes() {
        List<ThemeGroupResponseDto> themes = themeService.getAllThemes();
        return ResponseEntity.ok(themes);
    }
}