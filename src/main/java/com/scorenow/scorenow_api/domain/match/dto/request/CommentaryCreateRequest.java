package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentaryCreateRequest {
    private String minute;

    @NotBlank(message = "중계 멘트는 필수입니다.")
    private String content;

    private boolean recordEnabled;
}