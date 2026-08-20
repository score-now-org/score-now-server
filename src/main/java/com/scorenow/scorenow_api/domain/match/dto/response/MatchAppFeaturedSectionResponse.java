package com.scorenow.scorenow_api.domain.match.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "상단고정 및 핫매치 경기 목록")
public class MatchAppFeaturedSectionResponse {

    @Schema(description = "상단고정 경기 목록. 관리자가 지정한 노출 순서대로 반환됩니다.")
    private List<MatchAppFeaturedItemResponse> pinnedMatches;

    @Schema(description = "핫매치 경기 목록. 관리자가 지정한 노출 순서대로 반환됩니다.")
    private List<MatchAppFeaturedItemResponse> hotMatches;

}
