package com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EndedStatusDisplayDetailResponse {
    private String result;
    private Long winnerTeamId;
    private String winnerTeamName;
}
