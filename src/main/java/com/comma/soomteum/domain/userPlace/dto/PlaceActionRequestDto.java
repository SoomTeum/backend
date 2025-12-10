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
    @Schema(description = "공공데이터 API의 컨텐츠 ID", example = "128758")
    private String contentId;

    @NotBlank
    @Schema(description = "지역명", example = "강릉시")
    private String regionName;

    @NotBlank
    @Schema(description = "테마명", example = "자연관광지")
    private String themeName;

    @Schema(description = "여행지 이름", example = "경복궁")
    private String placeName;

    @NotNull
    @Schema(description = "혼잡도 레벨", example = "3.5")
    private BigDecimal cnctrLevel;
}