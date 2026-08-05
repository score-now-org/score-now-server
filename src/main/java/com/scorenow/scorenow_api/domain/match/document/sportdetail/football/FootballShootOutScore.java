package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FootballShootOutScore {
    private Integer homeScore;
    private Integer awayScore;

    public boolean isShootOutScoreChanged(FootballShootOutScore newShootOutScore) {
        if (newShootOutScore == null) {
            return false;
        }

        Integer newHomeScore = newShootOutScore.getHomeScore() == null ? homeScore : newShootOutScore.getHomeScore();
        Integer newAwayScore = newShootOutScore.getAwayScore() == null ? awayScore : newShootOutScore.getAwayScore();

        return !this.homeScore.equals(newHomeScore) || !this.awayScore.equals(newAwayScore);
    }
}
