package com.scorenow.scorenow_api.domain.odds.value;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class OddsValue {

    private static final int SCALE = 2; // 소수점 두번째 자리까지만 활용

    @Column(precision = 10, scale = SCALE)
    private BigDecimal value;

    private OddsValue(BigDecimal value) {
        this.value = normalize(value);
    }

    public static OddsValue from(BigDecimal value) {
        return new OddsValue(value);
    }

    public BigDecimal value() {
        return value;
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_VALUE, "배당률은 필수입니다.");
        }

        BigDecimal normalized = value.setScale(SCALE, RoundingMode.DOWN);

        if (normalized.signum() <= 0) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_VALUE, "배당률은 0보다 커야 합니다.");
        }

        return normalized;
    }
}
