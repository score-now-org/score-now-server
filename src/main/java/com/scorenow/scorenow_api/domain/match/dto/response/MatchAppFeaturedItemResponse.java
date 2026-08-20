package com.scorenow.scorenow_api.domain.match.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "앱 주요 경기 응답")
public class MatchAppFeaturedItemResponse {

    @Schema(description = "리그 ID", example = "2")
    private Long leagueId;

    @Schema(description = "리그 이름", example = "프리미어리그")
    private String leagueName;

    @Schema(description = "경기 정보")
    private MatchAppItemResponse match;
}
