package com.comma.soomteum.domain.auth.controller;

import com.comma.soomteum.domain.auth.dto.AuthTokenRequestDto;
import com.comma.soomteum.domain.auth.dto.KakaoTokenRequestDto;
import com.comma.soomteum.domain.auth.dto.LoginResponseDto;
import com.comma.soomteum.domain.auth.service.AuthService;
import com.comma.soomteum.domain.auth.service.KakaoAuthService;
import com.comma.soomteum.domain.token.dto.TokenDto;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 API (로그인, 토큰 재발급)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KakaoAuthService kakaoAuthService;

    @Operation(
            summary = "카카오 로그인",
            description = """
            카카오 OAuth 액세스 토큰을 받아 앱 로그인/회원가입을 처리합니다.
            - 성공 시 앱용 액세스/리프레시 토큰 등을 반환합니다.
            """,
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
    )
    @PostMapping("/login/kakao")
    public ApiResponse<LoginResponseDto> kakaoLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = KakaoTokenRequestDto.class),
                            examples = @ExampleObject(
                                    name = "카카오 로그인 요청 예시",
                                    value = """
                            {
                              "accessToken": "kakao_access_token_here"
                            }
                            """
                            )
                    )
            )
            @RequestBody KakaoTokenRequestDto requestDto
    ) {
        LoginResponseDto responseDto = kakaoAuthService.processKakaoLogin(requestDto.getAccessToken());
        return ApiResponse.ok(responseDto);
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
            리프레시 토큰으로 앱 액세스/리프레시 토큰을 재발급합니다.
            """,
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "재발급 성공",
            content = @Content(schema = @Schema(implementation = TokenDto.class))
    )
    @PostMapping("/reissue")
    public ApiResponse<TokenDto> reissue(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AuthTokenRequestDto.class),
                            examples = @ExampleObject(
                                    name = "재발급 요청 예시",
                                    value = """
                            {
                              "refreshToken": "your_refresh_token_here"
                            }
                            """
                            )
                    )
            )
            @RequestBody AuthTokenRequestDto requestDto
    ) {
        TokenDto tokenDto = authService.reissue(requestDto);
        return ApiResponse.ok(tokenDto);
    }

    @Operation(
            summary = "카카오 로그인 콜백",
            description = """
            카카오 OAuth 인가 코드를 받아 앱 로그인/회원가입을 처리합니다.
            - 프론트엔드에서 카카오 인가 코드를 받아 이 API로 전달합니다.
            - 성공 시 앱용 액세스/리프레시 토큰 등을 반환합니다.
            """,
            security = {}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
    )
    @GetMapping("/kakao/callback")
    public ApiResponse<LoginResponseDto> kakaoCallback(
            @Parameter(description = "카카오 인가 코드", required = true, example = "sample_auth_code_here")
            @RequestParam("code") String code
    ) {
        LoginResponseDto responseDto = kakaoAuthService.loginWithCode(code);
        return ApiResponse.ok(responseDto);
    }

}
