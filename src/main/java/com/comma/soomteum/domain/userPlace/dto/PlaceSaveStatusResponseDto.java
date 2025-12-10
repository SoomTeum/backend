package com.comma.soomteum.domain.userPlace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 저장 상태 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PlaceSaveStatusResponse", description = "장소 저장 상태 조회 응답")
public class PlaceSaveStatusResponseDto {

    @Schema(description = "저장 여부", example = "true")
    private boolean isSave;

    @Schema(description = "콘텐츠 ID", example = "128758")
    private String contentId;

    public static PlaceSaveStatusResponseDto of(boolean isSave, String contentId) {
        return PlaceSaveStatusResponseDto.builder()
                .isSave(isSave)
                .contentId(contentId)
                .build();
    }
}