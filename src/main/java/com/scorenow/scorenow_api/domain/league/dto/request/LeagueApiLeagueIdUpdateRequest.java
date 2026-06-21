package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueApiLeagueIdUpdateRequest {

    @NotBlank(message = "외부 API 리그 ID는 필수입니다.")
    @JsonProperty("apiLeagueId")
    private String apiLeagueId;
}
