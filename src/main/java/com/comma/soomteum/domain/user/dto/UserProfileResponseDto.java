package com.comma.soomteum.domain.user.dto;

import com.comma.soomteum.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {

    private Long userId;
    private String email;
    private String nickname;

    public static UserProfileResponseDto fromEntity(User user) {
        return UserProfileResponseDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }
}
