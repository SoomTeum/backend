package com.comma.soomteum.domain.user.controller;

import com.comma.soomteum.domain.auth.annotation.LoginUser;
import com.comma.soomteum.domain.user.dto.UserNicknameRequestDto;
import com.comma.soomteum.domain.user.dto.UserProfileResponseDto;
import com.comma.soomteum.domain.user.service.UserService;
import com.comma.soomteum.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPage", description = "마이페이지")
@RestController
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserProfileResponseDto> getUserProfile(@LoginUser Long userId) {
        return ApiResponse.ok(userService.getUserProfile(userId));
    }

    @Operation(summary = "닉네임 수정", description = "사용자의 닉네임을 수정합니다.")
    @PatchMapping("/me/nickname")
    public ApiResponse<UserProfileResponseDto> updateNickname(@LoginUser Long userId, @Valid @RequestBody UserNicknameRequestDto userNicknameRequestDto) {
        return ApiResponse.ok(userService.updateNickname(userId, userNicknameRequestDto));
    }
}
