package com.comma.soomteum.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
}