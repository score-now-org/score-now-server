package com.scorenow.scorenow_api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNicknameRequestDto {
    public static final String NICKNAME_PATTERN = "(?:\\X){1,8}";
    private String provider;
    private String socialId;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Size(max = 255, message = "닉네임이 너무 깁니다.")
    @Pattern(regexp = NICKNAME_PATTERN, message = "닉네임은 8자 이하로 입력해 주세요.")
    private String nickname;
}
