package com.scorenow.scorenow_api.domain.odds.dto.response;

import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;

import java.time.Instant;
import java.util.List;

public record OddsMarketSnapshotResponse(
        OddsMarketType marketType,
        Instant publishedAt,
        List<OddsCellResponse> cells
) {

    public OddsMarketSnapshotResponse {
        cells = List.copyOf(cells);
    }
}
