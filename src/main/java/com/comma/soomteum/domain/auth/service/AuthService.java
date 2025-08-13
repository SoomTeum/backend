package com.comma.soomteum.domain.auth.service;

import com.comma.soomteum.domain.auth.JwtTokenManager;
import com.comma.soomteum.domain.auth.dto.LoginResponseDto;
import com.comma.soomteum.domain.auth.dto.AuthTokenRequestDto;
import com.comma.soomteum.domain.token.dto.TokenDto;
import com.comma.soomteum.domain.token.entity.Token;
import com.comma.soomteum.domain.token.repository.TokenRepository;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.user.repository.UserRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenManager jwtTokenManager;

    @Transactional
    public LoginResponseDto socialLogin(String providerId, String email) {
        // 1) providerId로 user 조회 또는 신규 생성
        Optional<User> userOptional = userRepository.findByProviderId(providerId);

        boolean isNewUser = userOptional.isEmpty();

        User user = userOptional.orElseGet(() -> userRepository.save(
                User.builder()
                        .providerId(providerId)
                        .nickname("유저" + providerId)
                        .email(email)
                        .isActive(true)
                        .build()
        ));


        // 2) JWT 생성
        TokenDto tokenDto = jwtTokenManager.generateTokens(user);

        // 3) Token 엔티티로 저장
        Token token = Token.builder()
                .user(user)
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .accessExpiresAt(LocalDateTime.now().plus(jwtTokenManager.getAccessExpiration(), ChronoUnit.MILLIS))
                .refreshToken(tokenDto.getRefreshToken())
                .refreshExpiresAt(LocalDateTime.now().plus(jwtTokenManager.getRefreshExpiration(), ChronoUnit.MILLIS))
                .issuedAt(LocalDateTime.now())
                .build();
        tokenRepository.save(token);

        // 4) 응답
        return LoginResponseDto.builder()
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(tokenDto.getRefreshToken())
                .isNewUser(isNewUser)
                .build();
    }

    @Transactional
    public TokenDto reissue(AuthTokenRequestDto requestDto) {
        // 1) DB에서 refreshToken 조회
        Token token = tokenRepository.findByRefreshToken(requestDto.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 2) 리프레시 토큰 검증
        if (!jwtTokenManager.validateToken(token.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        // 3) 새 JWT 생성
        TokenDto newTokens = jwtTokenManager.generateTokens(token.getUser());

        // 4) refresh 토큰 만료 시각 계산
        LocalDateTime refreshExpiresAt = LocalDateTime.now()
                .plus(jwtTokenManager.getRefreshExpiration(), ChronoUnit.MILLIS);

        // 5) Token 엔티티 업데이트
        token.updateRefreshToken(newTokens.getRefreshToken(), refreshExpiresAt);

        return newTokens;
    }

}

