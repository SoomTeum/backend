package com.comma.soomteum.domain.theme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "테마 응답")
public class ThemeResponseDto {

    @Schema(description = "테마 ID", example = "1")
    private Long themeId;

    @Schema(description = "대분류 코드", example = "A01")
    private String cat1;

    @Schema(description = "대분류명", example = "자연")
    private String cat1Name;

    @Schema(description = "중분류 코드", example = "A0101")
    private String cat2;

    @Schema(description = "중분류명", example = "자연관광지")
    private String cat2Name;
}