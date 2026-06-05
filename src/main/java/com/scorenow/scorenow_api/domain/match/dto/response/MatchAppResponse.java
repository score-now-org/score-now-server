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
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchResult.*;

@Getter
@Builder
public class MatchAppResponse {
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private Long leagueId;
    private String leagueName;

    private AppTeamResponse homeTeam;
    private AppTeamResponse awayTeam;
    private Integer homeScore;
    private Integer awayScore;

    private String statusCode;
    private String statusName;
    private String currentCommentary;
    private AppMatchTimeInfoResponse timeInfo;
    private List<String> teamDisplayOrder;

    @Getter
    @Builder
    public static class AppTeamResponse {
        private Long id;
        private String name;
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
    public static class AppMatchTimeInfoResponse {
        private String type;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startAt;

        private Integer elapsedMinutes;
        private Integer elapsedSeconds;
        private String periodCode;
        private String periodName;
        private Boolean running;

        private String result;
        private Long winnerTeamId;
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
                    .type(MatchStatus.IN_PLAY.name())
                    .elapsedMinutes(matchClock != null ? matchClock.getElapsedMinutes() : null)
                    .elapsedSeconds(matchClock != null ? matchClock.getElapsedSeconds() : null)
                    .periodCode(period != null ? period.name() : null)
                    .periodName(period != null ? period.getDescription() : null)
                    .running(matchClock != null ? matchClock.getRunning() : null)
                    .build();
        }

        private static AppMatchTimeInfoResponse toNotStartedTimeInfo(Match match) {
            return AppMatchTimeInfoResponse.builder()
                    .type(MatchStatus.NOT_STARTED.name())
                    .startAt(match.getStartAt())
                    .build();
        }

        private static AppMatchTimeInfoResponse toResultTimeInfo(Match match, Integer homeScore, Integer awayScore) {
            MatchResult matchResult = fromScore(homeScore, awayScore);

            return AppMatchTimeInfoResponse.builder()
                    .type("ENDED")
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
