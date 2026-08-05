package com.scorenow.scorenow_api.domain.match.service.sportdetail.result;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.SportDetailType;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.entity.MatchResult;
import com.scorenow.scorenow_api.domain.team.entity.Team;

public interface MatchResultResolver {
    SportDetailType type();

    MatchResult resolveResult(Match match, MatchDetailDocument detail);

    default Long resolveWinnerTeamId(Match match, MatchResult result) {
        if (result == null) {
            return null;
        }

        return switch (result) {
            case HOME_WIN -> match.getHomeId();
            case AWAY_WIN -> match.getAwayId();
            case DRAW, UNKNOWN -> null;
        };
    }

    default String resolveWinnerTeamName(Match match, MatchResult result) {
        if (result == null) {
            return null;
        }

        Team winnerTeam = switch (result) {
            case HOME_WIN -> match.getHomeTeam();
            case AWAY_WIN -> match.getAwayTeam();
            case DRAW, UNKNOWN -> null;
        };

        return winnerTeam != null ? winnerTeam.resolveTeamName() : null;
    }
}
