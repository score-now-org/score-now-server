package com.scorenow.scorenow_api.domain.match.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentaryCreateRequest {
    @Schema(description = "중계 멘트", example = "손흥민 오늘 굉장한 퍼포먼스를 보여줍니다.")
    @NotBlank(message = "중계 멘트는 필수입니다.")
    private String content;

    @Schema(description = "중계 멘트 강조 여부", example = "true/false")
    private boolean highlighted;

    @Schema(description = "중계글 기록 저장 ON/OFF", example = "true/false")
    private boolean recordEnabled;
}