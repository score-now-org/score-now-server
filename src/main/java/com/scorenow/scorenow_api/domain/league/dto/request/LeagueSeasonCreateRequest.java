package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "관리자 리그 시즌 생성 요청")
@Getter
@NoArgsConstructor
public class LeagueSeasonCreateRequest {

    @NotNull(message = "리그 ID 는 필수 입니다.")
    @JsonProperty(value = "leagueId")
    @Schema(description = "시즌을 등록할 리그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long leagueId;

    @NotBlank(message = "시즌명은 필수 입니다.")
    @JsonProperty(value = "seasonName")
    @Schema(description = "시즌명", example = "2025/26", requiredMode = Schema.RequiredMode.REQUIRED)
    private String seasonName;

    @NotNull(message = "시즌 시작일은 필수 입니다.")
    @JsonProperty(value = "seasonStartAt")
    @Schema(description = "시즌 시작일", example = "2025-08-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate seasonStartAt;

    @NotNull(message = "시즌 종료일은 필수 입니다.")
    @JsonProperty(value = "seasonEndAt")
    @Schema(description = "시즌 종료일", example = "2026-05-31", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate seasonEndAt;

    @JsonProperty(value = "isCurrent")
    @Schema(description = "현재 시즌 여부. true이면 기존 현재 시즌은 해제됩니다.", example = "true")
    private boolean isCurrent;
}
