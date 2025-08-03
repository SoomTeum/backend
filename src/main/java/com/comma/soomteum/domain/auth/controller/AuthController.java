package com.comma.soomteum.domain.auth.controller;

import com.comma.soomteum.domain.auth.service.AuthService;
import com.comma.soomteum.domain.auth.service.KakaoAuthService;
import com.comma.soomteum.domain.auth.dto.KakaoTokenRequestDto;
import com.comma.soomteum.domain.auth.dto.LoginResponseDto;
import com.comma.soomteum.domain.auth.dto.TokenRequestDto;
import com.comma.soomteum.domain.token.dto.TokenDto;
import com.comma.soomteum.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KakaoAuthService kakaoAuthService;

    @PostMapping("/login/kakao")
    public ApiResponse<LoginResponseDto> kakaoLogin(@RequestBody KakaoTokenRequestDto requestDto) {
        LoginResponseDto responseDto = kakaoAuthService.processKakaoLogin(requestDto.getAccessToken());
        return ApiResponse.ok(responseDto);
    }

    @PostMapping("/reissue")
    public ApiResponse<TokenDto> reissue(@RequestBody TokenRequestDto requestDto) {
        TokenDto tokenDto = authService.reissue(requestDto);
        return ApiResponse.ok(tokenDto);
    }

}
