package com.scorenow.scorenow_api.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchResult {
    HOME_WIN("홈팀 승"),
    AWAY_WIN("홈팀 패"),
    DRAW("무승부"),
    UNKNOWN("");

    private final String description;

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
