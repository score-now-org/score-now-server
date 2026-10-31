package com.scorenow.scorenow_api.domain.odds.dto.response;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;

import java.util.List;

public record MatchOddsHistorySliceResponse(
        OddsCategory category,
        OddsMarketType marketType,
        List<OddsRowResponse> rows,
        String nextCursor,
        boolean hasNext
) {
    public MatchOddsHistorySliceResponse {
        rows = List.copyOf(rows);
    }

}
