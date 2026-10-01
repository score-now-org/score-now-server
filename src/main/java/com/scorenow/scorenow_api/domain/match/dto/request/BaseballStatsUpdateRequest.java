package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BaseballStatsUpdateRequest {
    @NotNull
    @Valid
    private TeamStats homeStats;

    @NotNull
    @Valid
    private TeamStats awayStats;

    @Getter
    @NoArgsConstructor
    public static class TeamStats {
        @NotNull
        @PositiveOrZero
        private Integer hits;

        @NotNull
        @PositiveOrZero
        private Integer errors;

        @NotNull
        @PositiveOrZero
        private Integer basesOnBalls;

        public BaseballStats toBaseballStats() {
            return new BaseballStats(hits, errors, basesOnBalls);
        }
    }
}
