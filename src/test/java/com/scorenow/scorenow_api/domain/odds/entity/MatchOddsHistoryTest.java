package com.scorenow.scorenow_api.domain.odds.entity;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsStatus;
import com.scorenow.scorenow_api.domain.odds.value.OddsOrigin;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MatchOddsHistoryTest {

    @Test
    void 야구_승패는_무승부_배당률_없이_생성할_수_있다() {
        Instant publishedAt = Instant.parse("2026-10-27T00:00:00Z");

        MatchOddsHistory history = MatchOddsHistory.createWinDrawLoss(
                1L,
                OddsCategory.OVERSEAS,
                OddsOrigin.manual(),
                new BigDecimal("1.80"),
                null,
                new BigDecimal("2.10"),
                publishedAt
        );

        assertThat(history.getMarketType()).isEqualTo(OddsMarketType.WIN_DRAW_LOSS);
        assertThat(history.getStatus()).isEqualTo(OddsStatus.ACTIVE);
        assertThat(history.getOrigin().isManual()).isTrue();
        assertThat(history.getOrigin().getSource()).isNull();
        assertThat(history.getOrigin().getBookmaker()).isNull();
        assertThat(history.getHomeOdds().value()).isEqualByComparingTo("1.80");
        assertThat(history.getDrawOdds()).isNull();
        assertThat(history.getAwayOdds().value()).isEqualByComparingTo("2.10");
        assertThat(history.getPublishedAt()).isEqualTo(publishedAt);
    }
}
