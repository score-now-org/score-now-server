package com.scorenow.scorenow_api.domain.match.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "상단고정/핫매치 목록 응답")
public class FeaturedMatchesResponse {

    @Schema(description = "조회 날짜. yyyyMMdd 형식입니다.", example = "20260723")
    private final String date;

    @Schema(description = "핫매치 목록. displayOrder 오름차순입니다.")
    private final List<FeaturedMatchResponse> hotMatches;

    @Schema(description = "상단고정 목록. displayOrder 오름차순입니다.")
    private final List<FeaturedMatchResponse> pinnedMatches;

    public FeaturedMatchesResponse(
            String date,
            List<FeaturedMatchResponse> hotMatches,
            List<FeaturedMatchResponse> pinnedMatches) {

        this.date = date;
        this.hotMatches = hotMatches == null ? List.of() : List.copyOf(hotMatches);
        this.pinnedMatches = pinnedMatches == null ? List.of() : List.copyOf(pinnedMatches);
    }
}
