package com.comma.soomteum.domain.userPlace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

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

    @Schema(description = "장소 ID", example = "123")
    private Long placeId;

    @Schema(description = "장소 이름/타이틀", example = "경복궁")
    private String title;

    @Schema(description = "대표 이미지 URL", example = "https://.../image.jpg")
    private String imageUrl;

    @Schema(description = "주소 또는 지역명", example = "서울 종로구 사직로 161")
    private String address;

    @Schema(description = "저장한 시각", example = "2025-08-19T11:54:00")
    private LocalDateTime savedAt;

}
