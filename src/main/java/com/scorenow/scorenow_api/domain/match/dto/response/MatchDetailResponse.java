package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MatchDetailResponse {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;
    private String statusCode;
    private String statusName;
    // TODO: 중계 상태 문구 관련 필드 추가 필요
    // TODO: 추가시간 관련 필드 추가 필요
    private Integer homeScore;
    private Integer awayScore;
    private String currentCommentary;
    private boolean currentCommentaryHighlighted;

    // TODO: 스텟 관련 필드 추가 필요
}
