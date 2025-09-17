package com.comma.soomteum.domain.region.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "지역 그룹 응답")
public class RegionGroupResponseDto {

    @Schema(description = "지역 코드", example = "32")
    private String areaCode;

    @Schema(description = "지역명", example = "강원")
    private String areaName;

    @Schema(description = "시군구 목록")
    private List<SigunguInfo> sigunguList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "시군구 정보")
    public static class SigunguInfo {
        @Schema(description = "시군구 코드", example = "1")
        private String sigunguCode;

        @Schema(description = "시군구명", example = "강릉시")
        private String sigunguName;
    }
}