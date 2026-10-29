package com.scorenow.scorenow_api.domain.odds.dto.response;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.domain.odds.value.HandicapLine;
import com.scorenow.scorenow_api.domain.odds.value.OddsValue;
import com.scorenow.scorenow_api.domain.odds.value.UnderOverLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OverseasOddsManagementOverviewResponse(
        OddsRegistrationType displayMode,
        List<LatestManualMarketOdds> latestManualMarketOdds
) {

    public record LatestManualMarketOdds(
            OddsMarketType marketType,
            BigDecimal homeOdds,
            BigDecimal drawOdds,
            BigDecimal awayOdds,
            BigDecimal handicapLine,
            BigDecimal underOverLine,
            BigDecimal underOdds,
            BigDecimal overOdds,
            Instant publishedAt
    ) {
    }

    public static OverseasOddsManagementOverviewResponse from(
            OddsRegistrationType displayMode,
            List<MatchOddsHistory> latestManualHistories
    ) {
        return new OverseasOddsManagementOverviewResponse(
                displayMode,
                latestManualHistories.stream()
                        .map(history -> new LatestManualMarketOdds(
                                        history.getMarketType(),
                                        valueOf(history.getHomeOdds()),
                                        valueOf(history.getDrawOdds()),
                                        valueOf(history.getAwayOdds()),
                                        valueOf(history.getHandicapLine()),
                                        valueOf(history.getUnderOverLine()),
                                        valueOf(history.getUnderOdds()),
                                        valueOf(history.getOverOdds()),
                                        history.getPublishedAt()
                                )
                        )
                        .toList()
        );
    }

    private static BigDecimal valueOf(OddsValue oddsValue) {
        return oddsValue == null ? null : oddsValue.value();
    }

    private static BigDecimal valueOf(HandicapLine handicapLine) {
        return handicapLine == null ? null : handicapLine.value();
    }

    private static BigDecimal valueOf(UnderOverLine underOverLine) {
        return underOverLine == null ? null : underOverLine.value();
    }
}
