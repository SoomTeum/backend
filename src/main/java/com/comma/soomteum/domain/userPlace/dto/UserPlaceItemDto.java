package com.comma.soomteum.domain.userPlace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * “내 저장 목록” 한 줄 아이템 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserPlaceItem", description = "내가 저장한 장소 아이템")
public class UserPlaceItemDto {

    @Schema(description = "Place 혼잡도 수준", example = "3.5")
    private BigDecimal cnctrLevel;

    @Schema(description = "Place 콘텐츠 ID", example = "content123")
    private String contentId;

    @Schema(description = "여행지 이름", example = "경복궁")
    private String placeName;

    @Schema(description = "Place 좋아요 수", example = "42")
    private Long likeCount;

    @Schema(description = "테마 이름", example = "자연")
    private String themeName;

    @Schema(description = "저장한 시각", example = "2025-08-19T11:54:00")
    private LocalDateTime savedAt;

}
