package com.scorenow.scorenow_api.domain.match.dto.response.stage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchStageResponse {
    private String displayText;
    private Object detail;

    public static MatchStageResponse empty() {
        return MatchStageResponse.builder().build();
    }
}
