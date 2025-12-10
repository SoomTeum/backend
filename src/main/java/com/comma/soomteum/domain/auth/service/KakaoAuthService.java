package com.comma.soomteum.domain.auth.service;


import com.comma.soomteum.domain.auth.dto.KakaoTokenResponseDto;
import com.comma.soomteum.domain.auth.dto.KakaoUserResponseDto;
import com.comma.soomteum.domain.auth.dto.LoginResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final AuthService authService;
    private final WebClient webClient = WebClient.create();

    // TODO: Move these to a configuration file (e.g., application.yml)
    @Value("${spring.security.oauth2.client.provider.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:YOUR_CLIENT_ID}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri:http://localhost:8080/api/auth/kakao/callback}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;

    public LoginResponseDto loginWithCode(String accessCode) {
        String kakaoAccessToken = getKakaoAccessToken(accessCode).block();
        return processKakaoLogin(kakaoAccessToken);
    }

    public LoginResponseDto processKakaoLogin(String kakaoAccessToken) {
        // 1. 전달받은 액세스 토큰으로 사용자 정보 요청
        KakaoUserResponseDto userResponse = getKakaoUserInfo(kakaoAccessToken).block();

        // 2. 받은 정보로 우리 서비스에 로그인/회원가입 처리
        String providerId = String.valueOf(userResponse.getId());
        String email = userResponse.getKakaoAccount().getEmail();

        return authService.socialLogin(providerId, email);
    }

    private Mono<String> getKakaoAccessToken(String accessCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", accessCode);

        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(KakaoTokenResponseDto.class)
                .map(KakaoTokenResponseDto::getAccessToken);
    }

    private Mono<KakaoUserResponseDto> getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponseDto.class);
    }

}
