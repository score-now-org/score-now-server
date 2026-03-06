package com.scorenow.scorenow_api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SocialLoginDto {
    private String accessToken;
    private String refreshToken;
    private Boolean isNewUser;
    private UserDto user;
}
