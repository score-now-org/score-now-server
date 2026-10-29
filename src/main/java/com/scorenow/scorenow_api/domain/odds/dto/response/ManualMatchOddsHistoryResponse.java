package com.scorenow.scorenow_api.domain.odds.dto.response;

import com.scorenow.scorenow_api.domain.odds.entity.MatchOddsHistory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.value.HandicapLine;
import com.scorenow.scorenow_api.domain.odds.value.OddsValue;
import com.scorenow.scorenow_api.domain.odds.value.UnderOverLine;

import java.math.BigDecimal;
import java.time.Instant;

public record ManualMatchOddsHistoryResponse(
        Long id,
        OddsMarketType marketType,
        OddsStatus status,
        BigDecimal homeOdds,
        BigDecimal drawOdds,
        BigDecimal awayOdds,
        BigDecimal handicapLine,
        BigDecimal underOverLine,
        BigDecimal underOdds,
        BigDecimal overOdds,
        Instant publishedAt,
        Boolean registeredBeforeMatchStart
) {

    public static ManualMatchOddsHistoryResponse from(MatchOddsHistory history, Instant matchStartAt) {
        Boolean registeredBeforeMatchStart = matchStartAt == null
                ? null
                : history.getPublishedAt().isBefore(matchStartAt);

        return new ManualMatchOddsHistoryResponse(
                history.getId(),
                history.getMarketType(),
                history.getStatus(),
                valueOf(history.getHomeOdds()),
                valueOf(history.getDrawOdds()),
                valueOf(history.getAwayOdds()),
                valueOf(history.getHandicapLine()),
                valueOf(history.getUnderOverLine()),
                valueOf(history.getUnderOdds()),
                valueOf(history.getOverOdds()),
                history.getPublishedAt(),
                registeredBeforeMatchStart
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
