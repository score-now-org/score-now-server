package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class BaseballInningScoresUpdateRequest {
    @NotNull
    private List<@NotNull @Valid InningScore> inningScores;

    @Getter
    public static class InningScore {
        @NotNull
        @Min(1)
        @Max(20)
        private Integer inningNumber;

        @PositiveOrZero
        private Integer homeScore;

        @PositiveOrZero
        private Integer awayScore;
    }

    public BaseballInningScores toBaseballInningScores() {
        Map<BaseballHalfInning, Integer> scoresByHalfInning = new EnumMap<>(BaseballHalfInning.class);

        for (InningScore inningScore : inningScores) {
            Integer inningNumber = inningScore.inningNumber;

            if (inningNumber == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "이닝 번호는 필수입니다.");
            }

            BaseballHalfInning homeHalfInning = BaseballHalfInning.getHomeHalfInning(inningNumber);
            BaseballHalfInning awayHalfInning = BaseballHalfInning.getAwayHalfInning(inningNumber);

            if (scoresByHalfInning.containsKey(homeHalfInning)) {
                throw new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        homeHalfInning.getDescription() + "는 중복되는 이닝 정보입니다.");
            }

            if (scoresByHalfInning.containsKey(awayHalfInning)) {
                throw new BusinessException(
                        ErrorCode.INVALID_PARAMETER,
                        awayHalfInning.getDescription() + "는 중복되는 이닝 정보입니다.");
            }

            scoresByHalfInning.put(homeHalfInning, inningScore.getHomeScore());
            scoresByHalfInning.put(awayHalfInning, inningScore.getAwayScore());
        }

        return BaseballInningScores.of(scoresByHalfInning);
    }
}
