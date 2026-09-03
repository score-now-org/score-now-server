package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchScoreUpdateRequest {

    @NotNull @Min(0)
    private Integer homeScore;

    @NotNull @Min(0)
    private Integer awayScore;
}
