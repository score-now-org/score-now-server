package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
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
            if (hits == null || errors == null || basesOnBalls == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "안타, 에러, 사사구 정보는 필수입니다.");
            }

            return new BaseballStats(hits, errors, basesOnBalls);
        }
    }
}
