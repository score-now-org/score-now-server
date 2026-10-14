package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBase;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballBaseStates;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballCount;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballDetail;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballInningScores;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballStats;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public record BaseballAppMatchDetailResponse(
        CurrentInningResponse currentInning,
        List<InningScoreResponse> inningScores,
        boolean detailedBroadcastEnabled,
        @JsonInclude(JsonInclude.Include.NON_NULL) DetailedResponse detailed
) implements MatchAppSportDetailResponse {

    public static BaseballAppMatchDetailResponse from(BaseballDetail baseballDetail) {
        if (baseballDetail == null) {
            return empty();
        }

        boolean detailedBroadcastEnabled = baseballDetail.isDetailedBroadcastEnabled();

        return new BaseballAppMatchDetailResponse(
                CurrentInningResponse.from(baseballDetail.getCurrentHalfInning()),
                resolveInningScores(baseballDetail.getInningScores()),
                detailedBroadcastEnabled,
                detailedBroadcastEnabled ? DetailedResponse.from(baseballDetail) : null
        );
    }

    public static BaseballAppMatchDetailResponse empty() {
        return new BaseballAppMatchDetailResponse(null, null, false, null);
    }

    private static List<InningScoreResponse> resolveInningScores(BaseballInningScores inningScores) {
        if (inningScores == null || inningScores.getScoresByHalfInning() == null) {
            return null;
        }

        Map<BaseballHalfInning, Integer> scores = inningScores.getScoresByHalfInning();

        return IntStream.rangeClosed(BaseballHalfInning.getMinInning(), BaseballHalfInning.getMaxInning())
                .mapToObj(inning -> new InningScoreResponse(
                        inning,
                        scores.get(BaseballHalfInning.getBottomInning(inning)),
                        scores.get(BaseballHalfInning.getTopInning(inning))
                ))
                .toList();
    }

    public record CurrentInningResponse(
            String code,
            Integer inningNumber,
            String half,
            String displayText
    ) {

        private static CurrentInningResponse from(BaseballHalfInning halfInning) {
            if (halfInning == null) {
                return null;
            }

            return new CurrentInningResponse(
                    halfInning.name(),
                    halfInning.getInningNumber(),
                    halfInning.getHalf().name(),
                    halfInning.getDescription()
            );
        }
    }

    public record InningScoreResponse(
            Integer inningNumber,
            Integer homeScore,
            Integer awayScore
    ) {
    }

    public record DetailedResponse(
            String pitcher,
            String batter,
            List<BaseStateResponse> baseStates,
            CountResponse count,
            StatsResponse homeStats,
            StatsResponse awayStats
    ) {

        private static DetailedResponse from(BaseballDetail baseballDetail) {
            return new DetailedResponse(
                    baseballDetail.getPitcher(),
                    baseballDetail.getBatter(),
                    resolveBaseStates(baseballDetail.getBaseStates()),
                    CountResponse.from(baseballDetail.getCount()),
                    StatsResponse.from(baseballDetail.getHomeStats()),
                    StatsResponse.from(baseballDetail.getAwayStats())
            );
        }

        private static List<BaseStateResponse> resolveBaseStates(BaseballBaseStates baseStates) {
            if (baseStates == null || baseStates.getBaseStates() == null) {
                return null;
            }

            Map<BaseballBase, BaseballBaseStates.BaseballBaseState> states = baseStates.getBaseStates();

            return Arrays.stream(BaseballBase.values())
                    .map(base -> BaseStateResponse.from(base, states.get(base)))
                    .toList();
        }
    }

    public record BaseStateResponse(
            Integer baseNumber,
            boolean runnerOnBase,
            String runnerName
    ) {

        private static BaseStateResponse from(
                BaseballBase base,
                BaseballBaseStates.BaseballBaseState baseState) {

            if (baseState == null) {
                return new BaseStateResponse(base.getBaseNumber(), false, null);
            }

            return new BaseStateResponse(
                    base.getBaseNumber(),
                    baseState.isRunnerOnBase(),
                    baseState.getRunnerName()
            );
        }
    }

    public record CountResponse(
            Integer balls,
            Integer strikes,
            Integer outs
    ) {

        private static CountResponse from(BaseballCount count) {
            if (count == null) {
                return null;
            }

            return new CountResponse(count.getBalls(), count.getStrikes(), count.getOuts());
        }
    }

    public record StatsResponse(
            Integer hits,
            Integer errors,
            Integer basesOnBalls
    ) {

        private static StatsResponse from(BaseballStats stats) {
            if (stats == null) {
                return null;
            }

            return new StatsResponse(stats.getHits(), stats.getErrors(), stats.getBaseOnBalls());
        }
    }
}
