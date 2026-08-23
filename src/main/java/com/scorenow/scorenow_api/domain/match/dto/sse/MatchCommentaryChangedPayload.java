package com.scorenow.scorenow_api.domain.match.dto.sse;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchCommentaryChangedPayload {
    private String commentaryId;
    private String content;
    private boolean highlighted;
    private String imageUrl;
}
