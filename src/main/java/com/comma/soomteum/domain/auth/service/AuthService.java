package com.comma.soomteum.domain.auth.service;

import com.comma.soomteum.domain.auth.JwtTokenManager;
import com.comma.soomteum.domain.auth.dto.LoginResponseDto;
import com.comma.soomteum.domain.auth.dto.AuthTokenRequestDto;
import com.comma.soomteum.domain.token.dto.TokenDto;
import com.comma.soomteum.domain.token.entity.Token;
import com.comma.soomteum.domain.token.repository.TokenRepository;
import com.comma.soomteum.domain.user.dto.UserProfileResponseDto;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.user.repository.UserRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        User user = userRepository.findByProviderId(providerId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .providerId(providerId)
                                .nickname("유저" + providerId)
                                .email(email)
                                .isActive(true)
                                .build()
                ));

        boolean isNewUser = user.getCreatedAt() != null &&
                user.getUpdatedAt() != null &&
                user.getCreatedAt().equals(user.getUpdatedAt());

        return issueTokensAndSave(user, isNewUser);
    }

    @Transactional
    public LoginResponseDto devLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 개발자 로그인은 항상 기존 유저로 간주
        return issueTokensAndSave(user, false);
    }

    private LoginResponseDto issueTokensAndSave(User user, boolean isNewUser) {
        TokenDto tokenDto = jwtTokenManager.generateTokens(user);

        // Instant -> LocalDateTime 변환 (서버 표준 Zone 사용)
        ZoneId zone = ZoneId.systemDefault();
        Token token = Token.builder()
                .user(user)
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .accessExpiresAt(LocalDateTime.ofInstant(tokenDto.getAccessExpiresAt(), zone))
                .refreshToken(tokenDto.getRefreshToken())
                .refreshExpiresAt(LocalDateTime.ofInstant(tokenDto.getRefreshExpiresAt(), zone))
                .issuedAt(LocalDateTime.ofInstant(tokenDto.getIssuedAt(), zone))
                .build();

        tokenRepository.save(token);

        return LoginResponseDto.builder()
                .grantType(tokenDto.getGrantType())
                .accessToken(tokenDto.getAccessToken())
                .refreshToken(tokenDto.getRefreshToken())
                .isNewUser(isNewUser)
                .user(UserProfileResponseDto.fromEntity(user))
                .build();
    }


    @Transactional
    public TokenDto reissue(AuthTokenRequestDto requestDto) {
        Token token = tokenRepository.findByRefreshToken(requestDto.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!jwtTokenManager.validateToken(token.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        TokenDto newTokens = jwtTokenManager.generateTokens(token.getUser());

        ZoneId zone = ZoneId.systemDefault();
        token.updateTokens(
                newTokens.getAccessToken(),
                LocalDateTime.ofInstant(newTokens.getAccessExpiresAt(), zone),
                newTokens.getRefreshToken(),
                LocalDateTime.ofInstant(newTokens.getRefreshExpiresAt(), zone)
        );

        return newTokens;
    }

}

