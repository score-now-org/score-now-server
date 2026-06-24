package com.scorenow.scorenow_api.domain.match.dto.sse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchScoreChangedPayload {
    private Integer homeScore;
    private Integer awayScore;
}
