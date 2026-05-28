package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.MatchDetailDocument;
import com.scorenow.scorenow_api.domain.match.model.MatchStats;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchDetailUpdateRequest {
    private String status;
    private Integer homeScore;
    private Integer awayScore;
    private MatchStats homeStats;
    private MatchStats awayStats;
    private MatchDetailDocument.MatchClock matchClock;
    private MatchDetailDocument.AdditionalTime additionalTime;
}