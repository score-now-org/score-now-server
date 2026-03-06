package com.scorenow.scorenow_api.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserDto {
    @JsonProperty("socialId")
    private String userId;
    private String nickname;
    private String profileImageUrl;
}
