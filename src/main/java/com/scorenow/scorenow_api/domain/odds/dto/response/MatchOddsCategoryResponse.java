package com.scorenow.scorenow_api.domain.odds.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;

import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record MatchOddsCategoryResponse(
        OddsCategory category,                      // 국내배당, 해외배당
        List<OddsMarketSnapshotResponse> markets    // 마켓타입별 배당률 정보
) {

    public MatchOddsCategoryResponse {
        markets = List.copyOf(markets);
    }

    public static MatchOddsCategoryResponse of(OddsCategory category, List<OddsMarketSnapshotResponse> markets) {
        return new MatchOddsCategoryResponse(category, markets);
    }
}
