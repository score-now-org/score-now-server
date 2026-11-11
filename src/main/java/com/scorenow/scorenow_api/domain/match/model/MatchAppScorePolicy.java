package com.scorenow.scorenow_api.domain.match.model;

import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;

import java.util.EnumSet;
import java.util.Set;

public final class MatchAppScorePolicy {

    private static final Set<MatchStatus> SCORE_HIDDEN_STATUSES = EnumSet.of(
            MatchStatus.POSTPONED,
            MatchStatus.CANCELLED,
            MatchStatus.ABANDONED
    );

    private MatchAppScorePolicy() {
    }

    public static Integer resolveScore(MatchStatus status, Integer score) {
        return SCORE_HIDDEN_STATUSES.contains(status) ? null : score;
    }
}
