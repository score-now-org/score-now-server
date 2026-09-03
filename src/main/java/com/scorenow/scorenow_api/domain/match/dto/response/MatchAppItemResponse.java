package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.TemporaryStadium;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "앱 경기 응답")
public class MatchAppItemResponse {
    @Schema(description = "경기 ID", example = "1001")
    private Long id;

    @Schema(description = "경기 날짜", example = "2026-06-05")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "경기장 정보")
    private StadiumResponse stadium;

    @Schema(description = "홈팀 정보")
    private TeamResponse homeTeam;

    @Schema(description = "어웨이팀 정보")
    private TeamResponse awayTeam;

    @Schema(description = "홈팀 점수", example = "1")
    private Integer homeScore;

    @Schema(description = "어웨이팀 점수", example = "0")
    private Integer awayScore;

    @Schema(
            description = "경기 상태 코드",
            example = "IN_PLAY",
            allowableValues = {
                    "NOT_STARTED", "IN_PLAY", "ENDED", "INTERRUPTED",
                    "POSTPONED", "CANCELLED", "ABANDONED"
            }
    )
    private String statusCode;

    @Schema(description = "경기 상태명", example = "진행중")
    private String statusName;

    @Schema(description = "현재 앱에 보여줄 중계 멘트", example = "홈팀이 선제골 이후 흐름을 잡습니다.")
    private String currentCommentary;

    @Schema(description = "현재 앱에 보여줄 중계 멘트 강조 여부. currentCommentary가 없으면 null입니다.", example = "true")
    private Boolean isCurrentCommentaryHighlighted;

    @Schema(
            description = """
                    경기 상태 표시 정보입니다.
                    앱의 시간/결과 표시 영역은 statusDisplay.displayText를 우선 사용합니다.
                    detail은 경기 상태와 종목에 따라 구조가 달라질 수 있습니다.
                    """
    )
    private MatchStatusDisplayResponse statusDisplay;

    @Schema(description = "팀 표시 순서", example = "[\"HOME\", \"AWAY\"]")
    private List<String> teamDisplayOrder;

    @Getter
    @Builder
    @Schema(description = "앱 경기 경기장 응답")
    public static class StadiumResponse {
        @Schema(description = "경기장명", example = "서울월드컵경기장")
        private String name;

        @Schema(description = "도시명", example = "서울")
        private String city;

        public static StadiumResponse from(Match match, Stadium registeredStadium) {
            TemporaryStadium temporaryStadium = match.getTemporaryStadium();
            if (temporaryStadium != null) {
                return StadiumResponse.builder()
                        .name(temporaryStadium.getTemporaryStadiumName())
                        .city(temporaryStadium.getTemporaryStadiumCity())
                        .build();
            }

            if (registeredStadium == null) {
                return null;
            }

            return StadiumResponse.builder()
                    .name(registeredStadium.getName())
                    .city(registeredStadium.getCity())
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

        @Schema(description = "경기 목록에 노출할 순위 그룹별 팀 순위")
        private List<TeamStandingResponse> standings;

        public static TeamResponse from(Long teamId, Team team, List<TeamStandingSummary> teamStandingSummaries) {
            return TeamResponse.builder()
                    .id(teamId)
                    .name(team.resolveTeamName())
                    .imageUrl(team.getImageUrl())
                    .standings(teamStandingSummaries.stream()
                            .map(TeamStandingResponse::from)
                            .toList())
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "경기 목록용 팀 순위 응답")
    public static class TeamStandingResponse {
        @Schema(description = "순위 그룹 표시 이름", example = "서부")
        private String groupName;

        @Schema(description = "팀 순위", example = "1")
        private Integer rank;

        private static TeamStandingResponse from(TeamStandingSummary summary) {
            return TeamStandingResponse.builder()
                    .groupName(summary.getGroupName())
                    .rank(summary.getRank())
                    .build();
        }
    }

    public static MatchAppItemResponse from(
            Match match,
            Stadium registeredStadium,
            MatchDetailDocument matchDetail,
            MatchStatusDisplayResponse matchStatusDisplayResponse,
            List<TeamStandingSummary> homeTeamStandings,
            List<TeamStandingSummary> awayTeamStandings) {

        return MatchAppItemResponse.builder()
                .id(match.getId())
                .date(match.getStartAt().toLocalDate())
                .stadium(StadiumResponse.from(match, registeredStadium))
                .homeTeam(TeamResponse.from(match.getHomeId(), match.getHomeTeam(), homeTeamStandings))
                .awayTeam(TeamResponse.from(match.getAwayId(), match.getAwayTeam(), awayTeamStandings))
                .homeScore(match.getHomeScore())
                .awayScore(match.getAwayScore())
                .statusCode(match.getStatusCode().name())
                .statusName(match.getStatusCode().getDescription())
                .currentCommentary(resolveCurrentCommentary(matchDetail))
                .isCurrentCommentaryHighlighted(resolveCurrentCommentaryHighlighted(matchDetail))
                .statusDisplay(matchStatusDisplayResponse)
                .teamDisplayOrder(match.resolveTeamDisplayOrder())
                .build();
    }

    private static String resolveCurrentCommentary(MatchDetailDocument matchDetail) {
        if (matchDetail == null) {
            return null;
        }

        return matchDetail.getCurrentCommentary();
    }

    private static Boolean resolveCurrentCommentaryHighlighted(MatchDetailDocument matchDetail) {
        if (matchDetail == null || matchDetail.getCurrentCommentary() == null) {
            return null;
        }

        return matchDetail.isCurrentCommentaryHighlighted();
    }
}
