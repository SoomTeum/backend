package com.comma.soomteum.domain.userPlace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 좋아요 상태 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PlaceLikeStatusResponse", description = "장소 좋아요 상태 조회 응답")
public class PlaceLikeStatusResponseDto {

    @Schema(description = "좋아요 여부", example = "true")
    private boolean isLike;

    @Schema(description = "콘텐츠 ID", example = "128758")
    private String contentId;

    public static PlaceLikeStatusResponseDto of(boolean isLike, String contentId) {
        return PlaceLikeStatusResponseDto.builder()
                .isLike(isLike)
                .contentId(contentId)
                .build();
    }
}