package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.scorenow.scorenow_api.domain.league.service.standings.model.TeamStandingSummary;
import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.MatchStatusDisplayResponse;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.domain.stadium.entity.Stadium;
import com.scorenow.scorenow_api.domain.stadium.entity.TemporaryStadium;
import com.scorenow.scorenow_api.domain.team.entity.Team;

import java.time.LocalDate;
import java.util.List;

public record MatchAppDetailResponse(
        Long id,
        SportCode sportCode,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        StadiumResponse stadium,
        TeamResponse homeTeam,
        TeamResponse awayTeam,
        Integer homeScore,
        Integer awayScore,
        String statusCode,
        String statusName,
        String currentCommentary,
        Boolean isCurrentCommentaryHighlighted,
        MatchStatusDisplayResponse statusDisplay,
        List<String> teamDisplayOrder,
        @JsonInclude(JsonInclude.Include.NON_NULL) MatchAppSportDetailResponse sportDetail
) {

    public static MatchAppDetailResponse from(
            Match match,
            Stadium registeredStadium,
            MatchDetailDocument matchDetail,
            MatchStatusDisplayResponse statusDisplay,
            List<TeamStandingSummary> homeTeamStandings,
            List<TeamStandingSummary> awayTeamStandings,
            MatchAppSportDetailResponse sportDetail) {

        return new MatchAppDetailResponse(
                match.getId(),
                resolveSportCode(match, matchDetail),
                match.getStartAt().toLocalDate(),
                StadiumResponse.from(match, registeredStadium),
                TeamResponse.from(match.getHomeId(), match.getHomeTeam(), homeTeamStandings),
                TeamResponse.from(match.getAwayId(), match.getAwayTeam(), awayTeamStandings),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getStatusCode().name(),
                match.getStatusCode().getDescription(),
                resolveCurrentCommentary(matchDetail),
                resolveCurrentCommentaryHighlighted(matchDetail),
                statusDisplay,
                match.resolveTeamDisplayOrder(),
                sportDetail
        );
    }

    private static SportCode resolveSportCode(Match match, MatchDetailDocument matchDetail) {
        if (match.getSport() != null) {
            return match.getSport().getSportCode();
        }

        return matchDetail == null ? null : matchDetail.getType();
    }

    private static String resolveCurrentCommentary(MatchDetailDocument matchDetail) {
        return matchDetail == null ? null : matchDetail.getCurrentCommentary();
    }

    private static Boolean resolveCurrentCommentaryHighlighted(MatchDetailDocument matchDetail) {
        if (matchDetail == null || matchDetail.getCurrentCommentary() == null) {
            return null;
        }

        return matchDetail.isCurrentCommentaryHighlighted();
    }

    public record StadiumResponse(
            String name,
            String city
    ) {

        public static StadiumResponse from(Match match, Stadium registeredStadium) {
            TemporaryStadium temporaryStadium = match.getTemporaryStadium();
            if (temporaryStadium != null) {
                return new StadiumResponse(
                        temporaryStadium.getTemporaryStadiumName(),
                        temporaryStadium.getTemporaryStadiumCity()
                );
            }

            if (registeredStadium == null) {
                return null;
            }

            return new StadiumResponse(
                    registeredStadium.getName(),
                    registeredStadium.getCity()
            );
        }
    }

    public record TeamResponse(
            Long id,
            String name,
            String imageUrl,
            List<TeamStandingResponse> standings
    ) {

        public static TeamResponse from(
                Long teamId,
                Team team,
                List<TeamStandingSummary> teamStandingSummaries) {

            return new TeamResponse(
                    teamId,
                    team.resolveTeamName(),
                    team.getImageUrl(),
                    teamStandingSummaries.stream()
                            .map(TeamStandingResponse::from)
                            .toList()
            );
        }
    }

    public record TeamStandingResponse(
            String groupName,
            Integer rank
    ) {

        private static TeamStandingResponse from(TeamStandingSummary summary) {
            return new TeamStandingResponse(summary.getGroupName(), summary.getRank());
        }
    }
}
