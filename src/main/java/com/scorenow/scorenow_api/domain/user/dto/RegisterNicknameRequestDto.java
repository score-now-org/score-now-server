package com.scorenow.scorenow_api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNicknameRequestDto {
    private String provider;
    private String socialId;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요.")
    private String nickname;
}
