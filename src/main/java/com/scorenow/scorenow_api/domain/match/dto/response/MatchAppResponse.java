package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchResult.*;

@Getter
@Builder
@Schema(description = "앱 경기 목록 응답")
public class MatchAppResponse {
    @Schema(description = "경기 ID", example = "1001")
    private Long id;

    @Schema(description = "경기 날짜", example = "2026-06-05")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "리그 ID", example = "2")
    private Long leagueId;

    @Schema(description = "리그 이름", example = "프리미어리그")
    private String leagueName;

    @Schema(description = "홈팀 정보")
    private AppTeamResponse homeTeam;

    @Schema(description = "어웨이팀 정보")
    private AppTeamResponse awayTeam;

    @Schema(description = "홈팀 점수", example = "1")
    private Integer homeScore;

    @Schema(description = "어웨이팀 점수", example = "0")
    private Integer awayScore;

    @Schema(description = "경기 상태 코드", example = "NOT_STARTED, IN_PLAY, ENDED")
    private String statusCode;

    @Schema(description = "경기 상태명", example = "경기전, 진행중, 종료")
    private String statusName;

    @Schema(description = "현재 앱에 보여줄 중계 멘트", example = "홈팀이 선제골 이후 흐름을 잡습니다.")
    private String currentCommentary;

    @Schema(description = "경기 시간 정보")
    private AppMatchTimeInfoResponse timeInfo;

    @Schema(description = "팀 표시 순서", example = "[\"HOME\", \"AWAY\"]")
    private List<String> teamDisplayOrder;

    @Getter
    @Builder
    @Schema(description = "앱 경기 팀 응답")
    public static class AppTeamResponse {
        @Schema(description = "팀 ID", example = "10")
        private Long id;

        @Schema(description = "팀 이름", example = "대한민국")
        private String name;

        @Schema(description = "팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/676363.png")
        private String imageUrl;

        public static AppTeamResponse from(Long teamId, Team team) {
            return AppTeamResponse.builder()
                    .id(teamId)
                    .name(team.getKName())
                    .imageUrl(team.getImageUrl())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "앱 경기 시간 정보 응답")
    public static class AppMatchTimeInfoResponse {
        @Schema(description = "경기 예정 시작 시간.", example = "2026-06-05T20:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startAt;

        @Schema(description = "경기 경과 시간 분", example = "27")
        private Integer elapsedMinutes;

        @Schema(description = "경기 경과 시간 초", example = "14")
        private Integer elapsedSeconds;

        @Schema(description = "경기 구간 코드", example = "FIRST_HALF")
        private String periodCode;

        @Schema(description = "경기 구간명", example = "전반전")
        private String periodName;

        @Schema(description = "타이머 진행 여부", example = "true")
        private Boolean running;

        @Schema(description = "경기 결과. HOME_WIN, AWAY_WIN, DRAW, UNKNOWN 중 하나입니다.", example = "HOME_WIN")
        private String result;

        @Schema(description = "승리팀 ID. 무승부나 결과 미확정이면 null입니다.", example = "10")
        private Long winnerTeamId;

        @Schema(description = "승리팀 이름. 무승부나 결과 미확정이면 null입니다.", example = "대한민국")
        private String winnerTeamName;

        public static AppMatchTimeInfoResponse from(Match match, MatchDetailDocument detail, Integer homeScore, Integer awayScore) {
            MatchStatus status = match.getStatusCode();

            if (status == MatchStatus.IN_PLAY) {
                return toInPlayTimeInfo(detail);
            }

            if (status == MatchStatus.NOT_STARTED) {
                return toNotStartedTimeInfo(match);
            }

            if (status == MatchStatus.ENDED) {
                return toResultTimeInfo(match, homeScore, awayScore);
            }

            return AppMatchTimeInfoResponse.builder().build();
        }

        private static AppMatchTimeInfoResponse toInPlayTimeInfo(MatchDetailDocument detail) {
            MatchDetailDocument.MatchClock matchClock = detail != null ? detail.getMatchClock() : null;
            MatchPeriod period = matchClock != null ? matchClock.getPeriod() : null;

            return AppMatchTimeInfoResponse.builder()
                    .elapsedMinutes(matchClock != null ? matchClock.getElapsedMinutes() : null)
                    .elapsedSeconds(matchClock != null ? matchClock.getElapsedSeconds() : null)
                    .periodCode(period != null ? period.name() : null)
                    .periodName(period != null ? period.getDescription() : null)
                    .running(matchClock != null ? matchClock.getRunning() : null)
                    .build();
        }

        private static AppMatchTimeInfoResponse toNotStartedTimeInfo(Match match) {
            return AppMatchTimeInfoResponse.builder()
                    .startAt(match.getStartAt())
                    .build();
        }

        private static AppMatchTimeInfoResponse toResultTimeInfo(Match match, Integer homeScore, Integer awayScore) {
            MatchResult matchResult = fromScore(homeScore, awayScore);

            return AppMatchTimeInfoResponse.builder()
                    .result(matchResult.name())
                    .winnerTeamId(resolveWinnerTeamId(match, matchResult))
                    .winnerTeamName(resolveWinnerTeamName(match, matchResult))
                    .build();
        }

        private static Long resolveWinnerTeamId(Match match, MatchResult matchResult) {
            if (matchResult == HOME_WIN) {
                return match.getHomeId();
            }

            if (matchResult == AWAY_WIN) {
                return match.getAwayId();
            }

            return null;
        }

        private static String resolveWinnerTeamName(Match match, MatchResult matchResult) {
            if (matchResult == HOME_WIN) {
                return resolveTeamName(match.getHomeTeam());
            }

            if (matchResult == AWAY_WIN) {
                return resolveTeamName(match.getAwayTeam());
            }

            return null;
        }
    }

    public static MatchAppResponse from(Match match, MatchDetailDocument detail) {
        Integer homeScore = resolveHomeScore(match, detail);
        Integer awayScore = resolveAwayScore(match, detail);

        return MatchAppResponse.builder()
                .id(match.getId())
                .date(match.getStartAt().toLocalDate())
                .leagueId(match.getLeagueId())
                .leagueName(resolveLeagueName(match.getLeague()))
                .homeTeam(AppTeamResponse.from(match.getHomeId(), match.getHomeTeam()))
                .awayTeam(AppTeamResponse.from(match.getAwayId(), match.getAwayTeam()))
                .homeScore(homeScore)
                .awayScore(awayScore)
                .statusCode(match.getStatusCode().name())
                .statusName(match.getStatusCode().getDescription())
                .currentCommentary(detail != null ? detail.getCurrentCommentary() : null)
                .timeInfo(AppMatchTimeInfoResponse.from(match, detail, homeScore, awayScore))
                .teamDisplayOrder(resolveTeamDisplayOrder(match))
                .build();
    }

    private static Integer resolveHomeScore(Match match, MatchDetailDocument detail) {
        if (detail != null && detail.getHomeScore() != null) {
            return detail.getHomeScore();
        }

        return match.getHomeScore();
    }

    private static Integer resolveAwayScore(Match match, MatchDetailDocument detail) {
        if (detail != null && detail.getAwayScore() != null) {
            return detail.getAwayScore();
        }

        return match.getAwayScore();
    }

    private static String resolveLeagueName(League league) {
        if (league == null) {
            return "";
        }

        if (StringUtils.hasText(league.getKName())) {
            return league.getKName();
        }

        if (StringUtils.hasText(league.getEName())) {
            return league.getEName();
        }

        return league.getSName();
    }

    private static String resolveTeamName(Team team) {
        if (team == null) {
            return "";
        }

        if (StringUtils.hasText(team.getKName())) {
            return team.getKName();
        }

        if (StringUtils.hasText(team.getEName())) {
            return team.getEName();
        }

        return team.getSName();
    }

    private static List<String> resolveTeamDisplayOrder(Match match) {
        return match.getTeamDisplayOrder() != null
                ? match.getTeamDisplayOrder().toDisplaySides()
                : TeamDisplayOrder.HOME_AWAY.toDisplaySides();
    }

}
