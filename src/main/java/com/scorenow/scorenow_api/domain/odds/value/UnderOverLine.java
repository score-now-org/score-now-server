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
import java.util.Objects;

@Embeddable
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class UnderOverLine {

    private static final int SCALE = 2; // 소수점 두번째 자리까지만 활용

    @Column(precision = 10, scale = SCALE)
    private BigDecimal value;

    private UnderOverLine(BigDecimal value) {
        this.value = normalize(value);
    }

    public static UnderOverLine from(BigDecimal value) {
        return new UnderOverLine(value);
    }

    public BigDecimal value() {
        return value;
    }

    // TODO: 언더오버 관련 에러코드 추가 필요.
    private BigDecimal normalize(BigDecimal value) {
        Objects.requireNonNull(value, "언더/오버 기준값은 필수입니다.");

        BigDecimal normalized = value.setScale(SCALE, RoundingMode.DOWN);

        if (normalized.signum() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "언더/오버 기준값은 0보다 커야 합니다.");
        }

        if (normalized.remainder(BigDecimal.ONE).signum() == 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "언더/오버 기준값은 정수일 수 없습니다.");
        }

        return normalized;
    }
}
