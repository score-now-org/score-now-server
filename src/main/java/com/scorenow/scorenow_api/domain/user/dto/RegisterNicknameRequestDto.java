package com.scorenow.scorenow_api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegisterNicknameRequestDto {
    private String provider;
    private String socialId;
    private String nickname;
}
