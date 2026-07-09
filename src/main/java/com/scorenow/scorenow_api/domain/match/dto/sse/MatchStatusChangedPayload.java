package com.scorenow.scorenow_api.domain.match.dto.sse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchStatusChangedPayload {
    private String statusCode;
    private String statusName;

    private String displayText;
}
