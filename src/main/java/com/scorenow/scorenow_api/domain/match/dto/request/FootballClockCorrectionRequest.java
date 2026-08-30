package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class FootballClockCorrectionRequest {

    @NotNull @Min(0)
    private Integer elapsedMinutes;

    @NotNull @Min(0) @Max(59)
    private Integer elapsedSeconds;
}
