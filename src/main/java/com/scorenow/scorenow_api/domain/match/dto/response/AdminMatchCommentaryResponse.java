package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.match.document.MatchCommentaryDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "관리자 경기 중계 멘트 응답")
public class AdminMatchCommentaryResponse {

    @Schema(description = "중계 멘트", example = "홈팀이 공격 흐름을 잡습니다.")
    private String content;

    @Schema(description = "강조 중계 멘트 여부", example = "true")
    private boolean highlighted;

    @Schema(description = "중계 멘트 등록 일자", example = "2026-06-05T20:35:00")
    private LocalDateTime createdAt;

    public static AdminMatchCommentaryResponse from(MatchCommentaryDocument document) {
        return AdminMatchCommentaryResponse.builder()
                .content(document.getContent())
                .highlighted(document.isHighlighted())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
