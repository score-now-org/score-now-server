package com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchStatusDisplayResponse {
    private String displayText;
    private Object detail;

    public static MatchStatusDisplayResponse empty() {
        return MatchStatusDisplayResponse.builder().build();
    }
}
