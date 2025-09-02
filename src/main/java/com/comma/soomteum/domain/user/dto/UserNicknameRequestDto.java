package com.comma.soomteum.domain.user.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserNicknameRequestDto {

    @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
    private String nickname;
}
