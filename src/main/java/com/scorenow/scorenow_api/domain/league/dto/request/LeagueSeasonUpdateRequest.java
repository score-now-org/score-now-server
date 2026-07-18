package com.scorenow.scorenow_api.domain.league.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "관리자 리그 시즌 수정 요청")
@Getter
@NoArgsConstructor
public class LeagueSeasonUpdateRequest {

    @Schema(description = "시즌명. null이면 변경하지 않습니다.", example = "2025/26")
    private String seasonName;

    @Schema(description = "시즌 시작일. null이면 변경하지 않습니다.", example = "2025-08-01")
    private LocalDate seasonStartAt;

    @Schema(description = "시즌 종료일. null이면 변경하지 않습니다.", example = "2026-05-31")
    private LocalDate seasonEndAt;

    @Schema(description = "현재 시즌 여부. null이면 변경하지 않습니다. true이면 기존 현재 시즌은 해제됩니다.", example = "true")
    private Boolean isCurrent;
}
