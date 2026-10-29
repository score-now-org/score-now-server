package com.scorenow.scorenow_api.domain.odds.value;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnderOverLineTest {

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "4", "4.0", "4.00", "4.009"})
    void 정수로_정규화되는_기준값은_허용하지_않는다(String value) {
        assertThatThrownBy(() -> UnderOverLine.from(new BigDecimal(value)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ODDS_INVALID_VALUE);
                    assertThat(exception.getMessage()).isEqualTo("언더/오버 기준값은 정수일 수 없습니다.");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.01", "3.50", "3.999"})
    void 소수부가_있는_기준값은_소수점_둘째_자리까지_정규화한다(String value) {
        UnderOverLine line = UnderOverLine.from(new BigDecimal(value));

        BigDecimal expected = new BigDecimal(value).setScale(2, RoundingMode.DOWN);
        assertThat(line.value()).isEqualByComparingTo(expected);
    }
}
