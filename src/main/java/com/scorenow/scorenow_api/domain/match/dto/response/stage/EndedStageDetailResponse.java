package com.scorenow.scorenow_api.domain.match.dto.response.stage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EndedStageDetailResponse {
    private String result;
    private Long winnerTeamId;
    private String winnerTeamName;
}
