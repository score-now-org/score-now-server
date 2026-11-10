package com.scorenow.scorenow_api.domain.match.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "리그 엠블럼 이미지 URL. 미등록 시 null", nullable = true)
    private String leagueImageUrl;

    @Schema(description = "경기 정보")
    private MatchAppItemResponse match;
}
