package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class BaseballAdminMatchDetailResponse {

    @Schema(description = "경기 시작 시간", example = "2026-06-05T20:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @Schema(description = "경기 상태 코드", example = "IN_PLAY")
    private String statusCode;

    @Schema(description = "경기 상태명", example = "진행중")
    private String statusName;

    @Schema(description = "홈팀 점수", example = "1")
    private Integer homeScore;

    @Schema(description = "원정팀 점수", example = "0")
    private Integer awayScore;

    @Schema(description = "현재 앱에 보여줄 중계 멘트", example = "담장을 넘어갑니다~")
    private String currentCommentary;

    @Schema(description = "현재 중계 멘트 강조 여부", example = "true")
    private boolean currentCommentaryHighlighted;

    @Schema(description = "야구 경기 상세 정보")
    private BaseballDetailResponse baseballDetail;

    @Getter
    @Builder
    public static class BaseballDetailResponse {
        private Inning currentInning;
        private List<InningScore> inningScores;
        private Boolean detailedBroadcastEnabled;
        private String pitcher;
        private String batter;
        private List<BaseState> baseStates;
        private Integer balls;
        private Integer strikes;
        private Integer outs;
        private Stats homeStats;
        private Stats awayStats;

        public static BaseballDetailResponse fromBasic(BaseballDetail baseballDetail) {
            if (baseballDetail == null) {
                return BaseballDetailResponse.builder().build();
            }

            return BaseballDetailResponse.builder()
                    .currentInning(Inning.from(baseballDetail.getCurrentHalfInning()))
                    .inningScores(resolveInningScores(baseballDetail.getInningScores()))
                    .detailedBroadcastEnabled(baseballDetail.isDetailedBroadcastEnabled())
                    .build();
        }

        public static BaseballDetailResponse fromDetailed(BaseballDetail baseballDetail) {
            if (baseballDetail == null) {
                return BaseballDetailResponse.builder().build();
            }

            BaseballCount count = baseballDetail.getCount();

            return BaseballDetailResponse.builder()
                    .currentInning(Inning.from(baseballDetail.getCurrentHalfInning()))
                    .inningScores(resolveInningScores(baseballDetail.getInningScores()))
                    .detailedBroadcastEnabled(baseballDetail.isDetailedBroadcastEnabled())
                    .pitcher(baseballDetail.getPitcher())
                    .batter(baseballDetail.getBatter())
                    .baseStates(resolveBaseStates(baseballDetail.getBaseStates()))
                    .balls(count == null ? null : count.getBalls())
                    .strikes(count == null ? null : count.getStrikes())
                    .outs(count == null ? null : count.getOuts())
                    .homeStats(Stats.from(baseballDetail.getHomeStats()))
                    .awayStats(Stats.from(baseballDetail.getAwayStats()))
                    .build();
        }

        public static List<InningScore> resolveInningScores(BaseballInningScores scores) {
            List<InningScore> resolved = new ArrayList<>();

            if (scores == null || scores.getScoresByHalfInning() == null) {
                scores = BaseballInningScores.initialize();
            }

            Map<BaseballHalfInning, Integer> scoresByHalfInning = scores.getScoresByHalfInning();

            int minInning = BaseballHalfInning.getMinInning();
            int maxInning = BaseballHalfInning.getMaxInning();
            for (int inning = minInning; inning <= maxInning; inning++) {
                List<BaseballHalfInning> inningCodes = BaseballHalfInning.getInningCodes(inning);

                Integer homeScore = null;
                Integer awayScore = null;

                for (BaseballHalfInning inningCode : inningCodes) {
                    if (inningCode.isTop()) {
                        awayScore = scoresByHalfInning.get(inningCode);
                    }
                    if (inningCode.isBottom()) {
                        homeScore = scoresByHalfInning.get(inningCode);
                    }
                }

                resolved.add(new InningScore(inning, homeScore, awayScore));
            }

            return resolved;
        }

        public static List<BaseState> resolveBaseStates(BaseballBaseStates states) {
            List<BaseState> resolved = new ArrayList<>();

            if (states == null || states.getBaseStates() == null) {
                states = BaseballBaseStates.initialize();
            }

            Map<BaseballBase, BaseballBaseStates.BaseballBaseState> baseStates = states.getBaseStates();
            for (var entry : baseStates.entrySet()) {
                resolved.add(BaseState.from(entry.getKey(), entry.getValue()));
            }

            return resolved;
        }

    }

    @Getter
    @Builder
    public static class Inning {
        private String halfInningCode;
        private int currentInningNumber;
        private String currentHalfInning;

        public static Inning from(BaseballHalfInning halfInning) {
            if (halfInning == null) {
                return null;
            }

            return Inning.builder()
                    .halfInningCode(halfInning.name())
                    .currentInningNumber(halfInning.getInningNumber())
                    .currentHalfInning(halfInning.getHalf().name())
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
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
        private String runner;

        public static BaseState from(BaseballBase base, BaseballBaseStates.BaseballBaseState baseState) {
            return BaseState.builder()
                    .baseNumber(base.getBaseNumber())
                    .runnerOnBase(baseState.isRunnerOnBase())
                    .runner(baseState.getRunnerName())
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
