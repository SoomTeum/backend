package com.comma.soomteum.domain.userPlace.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * 저장 목록 페이지 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserPlacePageResponse", description = "내 저장 목록(페이지)")
public class UserPlacePageResponseDto {

    @ArraySchema(schema = @Schema(implementation = UserPlaceItemDto.class))
    private List<UserPlaceItemDto> content;

    @Schema(description = "현재 페이지(0-base)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 요소 수", example = "134")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "7")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;

    public static UserPlacePageResponseDto of(List<UserPlaceItemDto> content,
                                              int page, int size,
                                              long totalElements, int totalPages,
                                              boolean first, boolean last,
                                              boolean hasNext, boolean hasPrevious) {
        return UserPlacePageResponseDto.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(first)
                .last(last)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
