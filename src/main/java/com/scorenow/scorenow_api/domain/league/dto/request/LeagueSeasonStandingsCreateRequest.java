package com.scorenow.scorenow_api.domain.league.dto.request;

import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "관리자 리그 시즌 순위 관리 등록 요청")
@Getter
@NoArgsConstructor
public class LeagueSeasonStandingsCreateRequest {

    @NotNull(message = "리그 시즌 ID는 필수입니다.")
    @Schema(description = "순위 관리를 등록할 리그 시즌 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long leagueSeasonId;

    @NotNull(message = "리그 시즌 순위 관리 타입은 필수입니다.")
    @Schema(description = "리그 시즌 순위 관리 타입", example = "EXTERNAL_DATA", allowableValues = {"IMAGE", "EXTERNAL_DATA"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private LeagueSeasonStandingsType standingsType;
}
