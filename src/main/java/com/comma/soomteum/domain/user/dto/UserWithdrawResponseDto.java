package com.comma.soomteum.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithdrawResponseDto {

    private String message;

    public static UserWithdrawResponseDto of() {
        return UserWithdrawResponseDto.builder()
                .message("회원 탈퇴가 완료되었습니다.")
                .build();
    }
}