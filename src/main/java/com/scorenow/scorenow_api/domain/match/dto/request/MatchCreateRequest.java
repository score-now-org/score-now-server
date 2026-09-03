package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 경기 수동 등록 요청")
public class MatchCreateRequest {

    @Schema(description = "종목 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "종목 ID는 필수입니다.")
    private Long sportId;

    @Schema(description = "리그 ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "리그 ID는 필수입니다.")
    private Long leagueId;

    @Schema(description = "홈팀 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "홈팀 ID는 필수입니다.")
    private Long homeId;

    @Schema(description = "원정팀 ID", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "원정팀 ID는 필수입니다.")
    private Long awayId;

    @Schema(description = "경기 시작 시간", example = "2026-06-05T20:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "경기 시작 시간은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;
}
