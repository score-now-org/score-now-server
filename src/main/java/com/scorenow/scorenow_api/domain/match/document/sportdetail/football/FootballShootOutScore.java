package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
public class FootballShootOutScore {
    private Integer homeScore;
    private Integer awayScore;

    public static FootballShootOutScore empty() {
        return FootballShootOutScore.builder().build();
    }

    public boolean isShootOutScoreChanged(FootballShootOutScore newShootOutScore) {
        if (newShootOutScore == null) {
            return false;
        }

        Integer newHomeScore = newShootOutScore.getHomeScore();
        Integer newAwayScore = newShootOutScore.getAwayScore();

        return !Objects.equals(homeScore, newHomeScore) || !Objects.equals(awayScore, newAwayScore);
    }
}
