package com.scorenow.scorenow_api.domain.league.dto.request;

import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "리그 시즌 순위 관리 타입 수정 요청")
public class LeagueSeasonStandingsTypeUpdateRequest {

    @NotNull(message = "리그 순위 관리 타입은 필수입니다.")
    @Schema(description = "리그 순위 관리 타입", example = "IMAGE")
    private LeagueSeasonStandingsType standingsType;
}