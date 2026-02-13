package com.scorenow.scorenow_api.domain.match.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentaryCreateRequest {
    private String minute;
    private String content;
}