package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.*;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BaseballDetailChangedPayload {
    private CurrentInning currentInning;
    private Integer homeScore;
    private Integer awayScore;
    private List<InningScore> inningScores;

    private String pitcher;
    private String batter;
    private List<BaseState> baseStates;
    private Count count;

    private Stats homeStats;
    private Stats awayStats;

    public static BaseballDetailChangedPayload from(BaseballDetail baseballDetail) {
        if (baseballDetail == null) {
            throw new IllegalArgumentException("야구 경기 상세 정보는 필수입니다.");
        }

        return BaseballDetailChangedPayload.builder()
                .currentInning(CurrentInning.from(baseballDetail.getCurrentHalfInning()))
                .homeScore(baseballDetail.getInningScores() == null ? null : baseballDetail.getInningScores().totalHomeScore())
                .awayScore(baseballDetail.getInningScores() == null ? null : baseballDetail.getInningScores().totalAwayScore())
                .inningScores(resolveInningScores(baseballDetail.getInningScores()))
                .pitcher(baseballDetail.getPitcher())
                .batter(baseballDetail.getBatter())
                .baseStates(resolveBaseStates(baseballDetail.getBaseStates()))
                .count(Count.from(baseballDetail.getCount()))
                .homeStats(Stats.from(baseballDetail.getHomeStats()))
                .awayStats(Stats.from(baseballDetail.getAwayStats()))
                .build();
    }

    private static List<InningScore> resolveInningScores(BaseballInningScores inningScores) {
        if (inningScores == null) {
            return null;
        }

        Map<BaseballHalfInning, Integer> scores = inningScores.getScoresByHalfInning();

        List<InningScore> result = new ArrayList<>();

        for (int inning = BaseballHalfInning.getMinInning(); inning <= BaseballHalfInning.getMaxInning(); inning++) {
            BaseballHalfInning top = BaseballHalfInning.getTopInning(inning);
            BaseballHalfInning bottom = BaseballHalfInning.getBottomInning(inning);

            result.add(InningScore.builder()
                    .inningNumber(inning)
                    .awayScore(scores.get(top))
                    .homeScore(scores.get(bottom))
                    .build()
            );
        }

        return result;
    }

    private static List<BaseState> resolveBaseStates(BaseballBaseStates baseStates) {
        if (baseStates == null) {
            return null;
        }

        Map<BaseballBase, BaseballBaseStates.BaseballBaseState> states = baseStates.getBaseStates();

        return Arrays.stream(BaseballBase.values())
                .map(base -> BaseState.from(base, states.get(base)))
                .toList();
    }

    @Getter
    @Builder
    public static class CurrentInning {
        private String code;
        private Integer inningNumber;
        private String half;
        private String displayText;

        public static CurrentInning from(BaseballHalfInning halfInning) {
            if (halfInning == null) {
                return null;
            }

            return CurrentInning.builder()
                    .code(halfInning.name())
                    .inningNumber(halfInning.getInningNumber())
                    .half(halfInning.getHalf().name())
                    .displayText(halfInning.getDescription())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class InningScore {
        private Integer inningNumber;
        private Integer homeScore;
        private Integer awayScore;
    }

    @Getter
    @Builder
    public static class BaseState {
        private Integer baseNumber;
        private boolean runnerOnBase;
        private String runnerName;

        public static BaseState from(BaseballBase base, BaseballBaseStates.BaseballBaseState state) {
            if (state == null) {
                return BaseState.builder()
                        .baseNumber(base.getBaseNumber())
                        .runnerOnBase(false)
                        .runnerName(null)
                        .build();
            }

            return BaseState.builder()
                    .baseNumber(base.getBaseNumber())
                    .runnerOnBase(state.isRunnerOnBase())
                    .runnerName(state.getRunnerName())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Count {
        private Integer balls;
        private Integer strikes;
        private Integer outs;

        public static Count from(BaseballCount count) {
            if (count == null) {
                return null;
            }

            return Count.builder()
                    .balls(count.getBalls())
                    .strikes(count.getStrikes())
                    .outs(count.getOuts())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Stats {
        private Integer hits;
        private Integer errors;
        private Integer basesOnBalls;

        public static Stats from(BaseballStats stats) {
            if (stats == null) {
                return null;
            }

            return Stats.builder()
                    .hits(stats.getHits())
                    .errors(stats.getErrors())
                    .basesOnBalls(stats.getBaseOnBalls())
                    .build();
        }
    }
}
