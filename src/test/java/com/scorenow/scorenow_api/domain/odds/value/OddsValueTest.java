package com.scorenow.scorenow_api.domain.odds.value;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OddsValueTest {

    @Test
    void 소수점_둘째_자리_아래는_절삭한다() {
        OddsValue oddsValue = OddsValue.from(new BigDecimal("1.239"));

        assertThat(oddsValue.value()).isEqualByComparingTo("1.23");
    }

    @Test
    void 값이_null이면_배당률_에러가_발생한다() {
        assertThatThrownBy(() -> OddsValue.from(null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_VALUE)
                );
    }

    @Test
    void 절삭한_배당률이_0이면_배당률_에러가_발생한다() {
        assertThatThrownBy(() -> OddsValue.from(new BigDecimal("0.009")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_VALUE)
                );
    }
}
