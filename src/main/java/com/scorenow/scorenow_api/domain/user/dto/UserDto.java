package com.scorenow.scorenow_api.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String socialId;
    private String nickname;
    private String profileImageUrl;
}
