package com.comma.soomteum.domain.auth.dto;

import com.comma.soomteum.domain.user.dto.UserProfileResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
    private UserProfileResponseDto user;
}