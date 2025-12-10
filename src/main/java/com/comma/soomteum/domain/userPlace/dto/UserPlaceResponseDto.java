package com.comma.soomteum.domain.userPlace.dto;

import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 좋아요/저장 토글 공통 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserPlaceResponse", description = "장소 좋아요/저장 요청에 대한 응답")
public class UserPlaceResponseDto {

    @Schema(description = "장소 ID", example = "123")
    private Long placeId;

    @Schema(description = "액션 타입", example = "LIKE")
    private UserActionType type;

    @Schema(description = "현재 유저 관점 상태 (true=켜짐: 좋아요/저장됨)", example = "true")
    private boolean enabled;

    @Schema(description = "이번 호출로 실제로 상태가 바뀌었는지", example = "true")
    private boolean changed;

    @Schema(description = "좋아요 수(Like일 때만 제공)", example = "42", nullable = true)
    private Long likeCount;

    @Schema(description = "메시지(선택)", example = "LIKE ON")
    private String message;

    @Schema(description = "생성 시각(선택)", example = "2025-08-19T11:54:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각(선택)", example = "2025-08-19T11:55:10")
    private LocalDateTime updatedAt;

    public static UserPlaceResponseDto of(Long placeId,
                                          UserActionType type,
                                          boolean enabled,
                                          boolean changed,
                                          Long likeCount,
                                          String message,
                                          LocalDateTime createdAt,
                                          LocalDateTime updatedAt) {
        return UserPlaceResponseDto.builder()
                .placeId(placeId)
                .type(type)
                .enabled(enabled)
                .changed(changed)
                .likeCount(likeCount)
                .message(message)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
