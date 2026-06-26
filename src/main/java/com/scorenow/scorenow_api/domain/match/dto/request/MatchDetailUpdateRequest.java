package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import com.scorenow.scorenow_api.domain.match.model.MatchStats;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchDetailUpdateRequest {
    private MatchStatus status;
    private Integer homeScore;
    private Integer awayScore;
    private MatchStats homeStats;
    private MatchStats awayStats;
    private MatchDetailDocument.MatchClock matchClock;
    private MatchDetailDocument.AdditionalTime additionalTime;
}