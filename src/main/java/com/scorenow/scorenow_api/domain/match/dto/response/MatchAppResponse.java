package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchPeriod;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.match.model.MatchAppStatusGroup;
import com.scorenow.scorenow_api.domain.match.service.MatchDisplayTextResolver;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.entity.MatchResult.*;

@Getter
@Builder
@Schema(description = "앱 리그별 경기 목록 응답")
public class MatchAppResponse {
    @Schema(description = "리그 ID", example = "2")
    private Long leagueId;

    @Schema(description = "리그 이름", example = "프리미어리그")
    private String leagueName;

    @Schema(description = "리그에 속한 경기 목록")
    private List<MatchItemResponse> matches;

    public static MatchAppResponse from(League league, List<MatchItemResponse> matches) {
        return MatchAppResponse.builder()
                .leagueId(league.getId())
                .leagueName(resolveLeagueName(league))
                .matches(matches)
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "앱 경기 응답")
    public static class MatchItemResponse {
        @Schema(description = "경기 ID", example = "1001")
        private Long id;

        @Schema(description = "경기 날짜", example = "2026-06-05")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        @Schema(description = "홈팀 정보")
        private TeamResponse homeTeam;

        @Schema(description = "어웨이팀 정보")
        private TeamResponse awayTeam;

        @Schema(description = "홈팀 점수", example = "1")
        private Integer homeScore;

        @Schema(description = "홈팀 승부차기 점수", example = "1")
        private Integer homeShootOutScore;

        @Schema(description = "어웨이팀 점수", example = "0")
        private Integer awayScore;

        @Schema(description = "어웨이팀 승부차기 점수", example = "0")
        private Integer awayShootOutScore;

        @Schema(description = "경기 상태 코드", example = "NOT_STARTED, IN_PLAY, ENDED, INTERRUPTED, POSTPONED, CANCELLED, ABANDONED")
        private String statusCode;

        @Schema(description = "경기 상태명", example = "경기전, 진행중, 종료, 중지, 연기, 취소, 취소")
        private String statusName;

        @Schema(description = "현재 앱에 보여줄 중계 멘트", example = "홈팀이 선제골 이후 흐름을 잡습니다.")
        private String currentCommentary;

        @Schema(description = "경기 시간 정보")
        private MatchTimeInfoResponse timeInfo;

        @Schema(description = "팀 표시 순서", example = "[\"HOME\", \"AWAY\"]")
        private List<String> teamDisplayOrder;

        public static MatchItemResponse from(
                Match match,
                MatchDetailDocument detail,
                MatchDisplayTextResolver matchDisplayTextResolver) {

            return MatchItemResponse.builder()
                    .id(match.getId())
                    .date(match.getStartAt().toLocalDate())
                    .homeTeam(TeamResponse.from(match.getHomeId(), match.getHomeTeam()))
                    .awayTeam(TeamResponse.from(match.getAwayId(), match.getAwayTeam()))
                    .homeScore(match.getHomeScore())
                    .homeShootOutScore(match.getHomeShootOutScore())
                    .awayScore(match.getAwayScore())
                    .awayShootOutScore(match.getAwayShootOutScore())
                    .statusCode(match.getStatusCode().name())
                    .statusName(match.getStatusCode().getDescription())
                    .currentCommentary(detail != null ? detail.getCurrentCommentary() : null)
                    .timeInfo(MatchTimeInfoResponse.from(match, detail, matchDisplayTextResolver))
                    .teamDisplayOrder(resolveTeamDisplayOrder(match))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "앱 경기 팀 응답")
    public static class TeamResponse {
        @Schema(description = "팀 ID", example = "10")
        private Long id;

        @Schema(description = "팀 이름", example = "대한민국")
        private String name;

        @Schema(description = "팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/676363.png")
        private String imageUrl;

        public static TeamResponse from(Long teamId, Team team) {
            return TeamResponse.builder()
                    .id(teamId)
                    .name(resolveTeamName(team))
                    .imageUrl(team != null ? team.getImageUrl() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "앱 경기 시간 정보 응답")
    public static class MatchTimeInfoResponse {

        @Schema(description = "앱 표시용 경기 시간 문구", example = "경기중: 전반 12 / 경기전: 20:30 / 경기종료: 홈팀 패")
        private String displayText;

        @Schema(description = "경기 예정 시작 시간.", example = "2026-06-05T20:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startAt;

        @Schema(description = "경기 경과 시간 분 (누적)", example = "75")
        private Integer elapsedMinutes;

        @Schema(description = "경기 경과 시간 분 (구간)", example = "25")
        private Integer displayElapsedMinutes;

        @Schema(description = "경기 경과 시간 초", example = "14")
        private Integer elapsedSeconds;

        @Schema(description = "경기 구간 코드", example = "FIRST_HALF, SECOND_HALF, EXTRA_FIRST_HALF, EXTRA_SECOND_HALF, PENALTY_SHOOTOUT")
        private String periodCode;

        @Schema(description = "경기 구간명", example = "전반, 후반, 연장 전반, 연장 후반, 승부차기")
        private String periodName;

        @Schema(description = "타이머 진행 여부", example = "true")
        private Boolean running;

        @Schema(description = "경기 결과", example = "HOME_WIN, AWAY_WIN, DRAW, UNKNOWN")
        private String result;

        @Schema(description = "승리팀 ID. 무승부나 결과 미확정이면 null", example = "10")
        private Long winnerTeamId;

        @Schema(description = "승리팀 이름. 무승부나 결과 미확정이면 null", example = "대한민국")
        private String winnerTeamName;

        public static MatchTimeInfoResponse from(
                Match match,
                MatchDetailDocument detail,
                MatchDisplayTextResolver matchDisplayTextResolver) {

            MatchAppStatusGroup statusGroup = MatchAppStatusGroup.findBy(match.getStatusCode()).orElse(null);

            if (statusGroup == MatchAppStatusGroup.IN_PLAY) {
                return toInPlayMatchTimeInfo(detail, matchDisplayTextResolver);
            }

            if (statusGroup == MatchAppStatusGroup.SCHEDULED) {
                return toScheduledMatchTimeInfo(match, matchDisplayTextResolver);
            }

            if (statusGroup == MatchAppStatusGroup.ENDED) {
                return toEndedMatchTimeInfo(match, matchDisplayTextResolver);
            }

            return MatchTimeInfoResponse.builder().build();
        }

        private static MatchTimeInfoResponse toInPlayMatchTimeInfo(
                MatchDetailDocument detail,
                MatchDisplayTextResolver matchDisplayTextResolver) {

            MatchDetailDocument.MatchClock matchClock = detail != null ? detail.getMatchClock() : null;
            MatchPeriod period = matchClock != null ? matchClock.getPeriod() : null;

            // 시간 관련 정보가 없을 때 보여줄 default 문구
            if (matchClock == null || matchClock.getPeriod() == null) {
                return MatchTimeInfoResponse.builder()
                        .displayText("경기중")
                        .build();
            }

            return MatchTimeInfoResponse.builder()
                    .displayText(matchDisplayTextResolver.resolveInPlayDisplayText(matchClock))
                    .elapsedMinutes(matchClock.getElapsedMinutes())
                    .displayElapsedMinutes(matchDisplayTextResolver.resolvePeriodElapsedMinutes(matchClock))
                    .elapsedSeconds(matchClock.getElapsedSeconds())
                    .periodCode(period.name())
                    .periodName(period.getDescription())
                    .running(matchClock.getRunning())
                    .build();
        }

        private static MatchTimeInfoResponse toScheduledMatchTimeInfo(
                Match match,
                MatchDisplayTextResolver matchDisplayTextResolver) {

            return MatchTimeInfoResponse.builder()
                    .displayText(matchDisplayTextResolver.resolveScheduledDisplayText(match))
                    .startAt(match.getStartAt())
                    .build();
        }

        private static MatchTimeInfoResponse toEndedMatchTimeInfo(
                Match match,
                MatchDisplayTextResolver matchDisplayTextResolver) {

            MatchResult matchResult = match.getResultByScore();

            return MatchTimeInfoResponse.builder()
                    .displayText(matchDisplayTextResolver.resolveEndedDisplayText(matchResult))
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
