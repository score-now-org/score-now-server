package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FootballAdditionalTimeUpdateRequest {

    @NotNull @Min(0)
    private Integer firstHalf;

    @NotNull @Min(0)
    private Integer secondHalf;

    @NotNull @Min(0)
    private Integer extraFirstHalf;

    @NotNull @Min(0)
    private Integer extraSecondHalf;
}
