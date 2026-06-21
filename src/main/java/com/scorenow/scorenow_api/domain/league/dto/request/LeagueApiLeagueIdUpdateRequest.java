package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "외부 API 리그 ID 수정 요청")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueApiLeagueIdUpdateRequest {

    @NotBlank(message = "외부 API 리그 ID는 필수입니다.")
    @JsonProperty("apiLeagueId")
    @Schema(description = "외부 API 리그 ID", example = "94", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiLeagueId;
}
