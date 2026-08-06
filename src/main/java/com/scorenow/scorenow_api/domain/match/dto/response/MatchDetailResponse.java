package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MatchDetailResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    private String statusCode;
    private String statusName;

    private Integer homeScore;
    private Integer awayScore;

    private String currentCommentary;
    private boolean currentCommentaryHighlighted;

    private FootballDetailResponse footballDetail;

    @Getter
    @Builder
    public static class FootballDetailResponse {
        private String phaseCode;
        private String phaseName;

        private FootballShootOutScoreResponse shootOutScore;
        private FootballAdditionalTimeResponse additionalTime;
        private FootballStatsResponse homeStats;
        private FootballStatsResponse awayStats;

        public static FootballDetailResponse from(FootballDetail footballDetail) {
            if (footballDetail == null) {
                return FootballDetailResponse.builder().build();
            }

            FootballPhase phase = footballDetail.getClock() != null
                    ? footballDetail.getClock().getPhase()
                    : null;

            return FootballDetailResponse.builder()
                    .phaseCode(phase != null ? phase.name() : null)
                    .phaseName(phase != null ? phase.getDescription() : null)
                    .shootOutScore(FootballShootOutScoreResponse.from(footballDetail.getShootOutScore()))
                    .additionalTime(FootballAdditionalTimeResponse.from(footballDetail.getAdditionalTime()))
                    .homeStats(FootballStatsResponse.from(footballDetail.getHomeStats()))
                    .awayStats(FootballStatsResponse.from(footballDetail.getAwayStats()))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FootballShootOutScoreResponse {
        private Integer homeScore;
        private Integer awayScore;

        public static FootballShootOutScoreResponse from(FootballShootOutScore shootOutScore) {
            if (shootOutScore == null) {
                return null;
            }

            return FootballShootOutScoreResponse.builder()
                    .homeScore(shootOutScore.getHomeScore())
                    .awayScore(shootOutScore.getAwayScore())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FootballAdditionalTimeResponse {
        private Integer firstHalf;
        private Integer secondHalf;
        private Integer extraFirstHalf;
        private Integer extraSecondHalf;

        public static FootballAdditionalTimeResponse from(FootballAdditionalTime additionalTime) {
            if (additionalTime == null) {
                return null;
            }

            return FootballAdditionalTimeResponse.builder()
                    .firstHalf(additionalTime.getFirstHalf())
                    .secondHalf(additionalTime.getSecondHalf())
                    .extraFirstHalf(additionalTime.getExtraFirstHalf())
                    .extraSecondHalf(additionalTime.getExtraSecondHalf())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FootballStatsResponse {
        private Integer yellowCards;
        private Integer redCards;
        private Integer shots;
        private Integer shotsOnTarget;
        private Integer possession;
        private Integer offsides;
        private Integer fouls;
        private Integer corners;
        private Integer freeKicks;

        public static FootballStatsResponse from(FootballStats stats) {
            if (stats == null) {
                return null;
            }

            return FootballStatsResponse.builder()
                    .yellowCards(stats.getYellowCards())
                    .redCards(stats.getRedCards())
                    .shots(stats.getShots())
                    .shotsOnTarget(stats.getShotsOnTarget())
                    .possession(stats.getPossession())
                    .offsides(stats.getOffsides())
                    .fouls(stats.getFouls())
                    .corners(stats.getCorners())
                    .freeKicks(stats.getFreeKicks())
                    .build();
        }
    }
}
