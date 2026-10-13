package com.scorenow.scorenow_api.domain.user.dto;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenRequestDto {
    @NotBlank(message = "RefreshToken이 필요합니다.")
    @Size(max = 8192)
    private String refreshToken;

}
