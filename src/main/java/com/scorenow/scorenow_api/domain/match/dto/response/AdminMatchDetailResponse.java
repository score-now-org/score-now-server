package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "관리자 경기 상세 응답")
public class AdminMatchDetailResponse {

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

    @Schema(description = "현재 앱에 보여줄 중계 멘트", example = "홈팀이 선제골 이후 흐름을 잡습니다.")
    private String currentCommentary;

    @Schema(description = "현재 중계 멘트 강조 여부", example = "true")
    private boolean currentCommentaryHighlighted;

    @Schema(description = "축구 경기 상세 정보")
    private FootballDetailResponse footballDetail;

    @Getter
    @Builder
    @Schema(description = "축구 경기 상세 응답")
    public static class FootballDetailResponse {
        @Schema(description = "축구 경기 구간 코드", example = "SECOND_HALF")
        private String phaseCode;

        @Schema(description = "축구 경기 구간명", example = "후반")
        private String phaseName;

        @Schema(description = "승부차기 점수")
        private FootballShootOutScoreResponse shootOutScore;

        @Schema(description = "추가시간 정보")
        private FootballAdditionalTimeResponse additionalTime;

        @Schema(description = "홈팀 축구 스탯")
        private FootballStatsResponse homeStats;

        @Schema(description = "원정팀 축구 스탯")
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
    @Schema(description = "축구 승부차기 점수 응답")
    public static class FootballShootOutScoreResponse {
        @Schema(description = "홈팀 승부차기 점수", example = "4")
        private Integer homeScore;

        @Schema(description = "원정팀 승부차기 점수", example = "3")
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
    @Schema(description = "축구 추가시간 응답")
    public static class FootballAdditionalTimeResponse {
        @Schema(description = "전반 추가시간", example = "2")
        private Integer firstHalf;

        @Schema(description = "후반 추가시간", example = "4")
        private Integer secondHalf;

        @Schema(description = "연장 전반 추가시간", example = "1")
        private Integer extraFirstHalf;

        @Schema(description = "연장 후반 추가시간", example = "1")
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
    @Schema(description = "축구 스탯 응답")
    public static class FootballStatsResponse {
        @Schema(description = "경고 수", example = "2")
        private Integer yellowCards;

        @Schema(description = "퇴장 수", example = "0")
        private Integer redCards;

        @Schema(description = "슈팅 수", example = "12")
        private Integer shots;

        @Schema(description = "유효 슈팅 수", example = "5")
        private Integer shotsOnTarget;

        @Schema(description = "점유율", example = "55")
        private Integer possession;

        @Schema(description = "오프사이드 수", example = "1")
        private Integer offsides;

        @Schema(description = "파울 수", example = "8")
        private Integer fouls;

        @Schema(description = "코너킥 수", example = "6")
        private Integer corners;

        @Schema(description = "프리킥 수", example = "10")
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
