package com.scorenow.scorenow_api.domain.match.entity;

public enum MatchResult {
    HOME_WIN,
    AWAY_WIN,
    DRAW,
    UNKNOWN;

    public static MatchResult fromScore(Integer homeScore, Integer awayScore) {
        if (homeScore == null || awayScore == null) {
            return UNKNOWN;
        }

        if (homeScore.equals(awayScore)) {
            return DRAW;
        }

        return homeScore > awayScore ? HOME_WIN : AWAY_WIN;
    }

}
