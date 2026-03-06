package com.scorenow.scorenow_api.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLoginRequestDto {
    private String provider;
    private String socialId;
    private String name;

}
