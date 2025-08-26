package com.comma.soomteum.domain.user.service;

import com.comma.soomteum.domain.user.dto.UserNicknameRequestDto;
import com.comma.soomteum.domain.user.dto.UserProfileResponseDto;
import com.comma.soomteum.domain.user.entity.User;
import com.comma.soomteum.domain.user.repository.UserRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 마이페이지 - 이메일, 닉네임 조회
     **/
    public UserProfileResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponseDto.fromEntity(user);
    }

    /**
     * 유저 닉네임 수정
     **/
    @Transactional
    public UserProfileResponseDto updateNickname(Long userId, UserNicknameRequestDto userNicknameRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(userNicknameRequestDto.getNickname());
        return UserProfileResponseDto.fromEntity(user);
    }
}
