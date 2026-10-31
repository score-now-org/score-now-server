package com.scorenow.scorenow_api.domain.odds.dto.cursor;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsRegistrationType;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchOddsHistoryCursorTest {

    @Test
    void 커서를_인코딩하고_동일한_조회_조건으로_복원한다() {
        MatchOddsHistoryCursor cursor = new MatchOddsHistoryCursor(
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                OddsRegistrationType.AUTOMATIC,
                Instant.parse("2026-10-31T01:02:03.123456Z"),
                123L
        );

        MatchOddsHistoryCursor parsed = MatchOddsHistoryCursor.parse(
                cursor.encode(),
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                OddsRegistrationType.AUTOMATIC
        );

        assertThat(parsed).isEqualTo(cursor);
    }

    @Test
    void 첫_페이지처럼_인코딩된_커서가_없으면_null을_반환한다() {
        MatchOddsHistoryCursor parsed = MatchOddsHistoryCursor.parse(
                null,
                OddsCategory.DOMESTIC,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsRegistrationType.MANUAL
        );

        assertThat(parsed).isNull();
    }

    @Test
    void 다른_마켓의_커서는_사용할_수_없다() {
        MatchOddsHistoryCursor cursor = new MatchOddsHistoryCursor(
                OddsCategory.OVERSEAS,
                OddsMarketType.HANDICAP,
                OddsRegistrationType.AUTOMATIC,
                Instant.parse("2026-10-31T01:02:03Z"),
                123L
        );

        assertThatThrownBy(() -> MatchOddsHistoryCursor.parse(
                cursor.encode(),
                OddsCategory.OVERSEAS,
                OddsMarketType.UNDER_OVER,
                OddsRegistrationType.AUTOMATIC
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void 표기_방식이_변경되면_기존_커서를_사용할_수_없다() {
        MatchOddsHistoryCursor cursor = new MatchOddsHistoryCursor(
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsRegistrationType.AUTOMATIC,
                Instant.parse("2026-10-31T01:02:03Z"),
                123L
        );

        assertThatThrownBy(() -> MatchOddsHistoryCursor.parse(
                cursor.encode(),
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsRegistrationType.MANUAL
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void 형식이_올바르지_않은_커서는_사용할_수_없다() {
        assertThatThrownBy(() -> MatchOddsHistoryCursor.parse(
                "invalid-cursor",
                OddsCategory.OVERSEAS,
                OddsMarketType.WIN_DRAW_LOSS,
                OddsRegistrationType.AUTOMATIC
        )).isInstanceOf(BusinessException.class);
    }
}
