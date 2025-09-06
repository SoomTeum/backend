package com.comma.soomteum.domain.userPlace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "장소 좋아요/저장 요청 DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceActionRequestDto {

    @NotBlank
    @Schema(description = "공공데이터 API의 컨텐츠 ID", example = "123456")
    private String contentId;

    @NotNull
    @Schema(description = "지역 ID", example = "1")
    private Long regionId;

    @NotNull
    @Schema(description = "테마 ID", example = "2")
    private Long themeId;

    @NotNull
    @Schema(description = "혼잡도 레벨", example = "3.5")
    private BigDecimal cnctrLevel;
}