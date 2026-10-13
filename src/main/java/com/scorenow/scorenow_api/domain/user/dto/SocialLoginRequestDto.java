package com.scorenow.scorenow_api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLoginRequestDto {
    @NotBlank
    @Pattern(regexp = "(?i)google|kakao|apple", message = "지원하지 않는 소셜 로그인입니다.")
    private String provider;

    @Schema(description = "기존 요청 호환용 필드이며 인증에는 사용하지 않습니다.", deprecated = true)
    @Size(max = 255)
    private String socialId;

    @Size(max = 100)
    private String name;

    @Schema(description = "Google 또는 Apple에서 발급한 ID 토큰")
    @Size(max = 16384)
    private String idToken;

    @Schema(description = "Kakao에서 발급한 Access 토큰")
    @Size(max = 8192)
    private String accessToken;
}
