package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FootballShootOutScoreChangePayload {
    private Integer homeShootOutScore;
    private Integer awayShootOutScore;
}
