package com.scorenow.scorenow_api.domain.admin.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminLoginRequestDto {
    private String id;
    private String password;
}
