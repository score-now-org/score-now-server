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
public class HandicapLine {

    private static final int SCALE = 2; // 소수점 두번째 자리까지만 활용

    @Column(precision = 10, scale = SCALE)
    private BigDecimal value;


    private HandicapLine(BigDecimal value) {
        this.value = normalize(value);
    }

    public static HandicapLine from(BigDecimal value) {
        return new HandicapLine(value);
    }

    public BigDecimal value() {
        return value;
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.ODDS_INVALID_VALUE, "핸디캡 기준값은 필수입니다.");
        }
        return value.setScale(SCALE, RoundingMode.DOWN);
    }
}
