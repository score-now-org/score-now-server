package com.scorenow.scorenow_api.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterNicknameDto {
    private UserDto user;
    private String newNickname;

}
