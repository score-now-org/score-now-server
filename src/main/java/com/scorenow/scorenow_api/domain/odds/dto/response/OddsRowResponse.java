package com.scorenow.scorenow_api.domain.odds.dto.response;

import java.time.Instant;
import java.util.List;

public record OddsRowResponse(
        Instant publishedAt,
        List<OddsCellResponse> cells
) {
    public OddsRowResponse {
        cells = List.copyOf(cells);
    }
}
