package com.scorenow.scorenow_api.domain.admin.member.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class AdminLoginRequestDto {
    @NotBlank
    @Size(max = 100)
    private String id;

    @NotBlank
    @Size(max = 72)
    private String password;
}
