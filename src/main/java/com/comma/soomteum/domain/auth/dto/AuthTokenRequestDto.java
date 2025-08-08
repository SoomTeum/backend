package com.comma.soomteum.domain.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthTokenRequestDto {
    private String refreshToken;
}
