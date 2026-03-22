package com.scorenow.scorenow_api.domain.admin.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class LoginHistoryDto {

    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private String lastLoginCountryCode;

}
