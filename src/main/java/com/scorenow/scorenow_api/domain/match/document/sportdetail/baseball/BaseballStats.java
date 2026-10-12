package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class BaseballStats {
    private int hits;
    private int errors;
    private int baseOnBalls;

    public BaseballStats(int hits, int errors, int baseOnBalls) {
        validateNonNegative(hits, errors, baseOnBalls);
        this.hits = hits;
        this.errors = errors;
        this.baseOnBalls = baseOnBalls;
    }

    private void validateNonNegative(int hits, int error, int baseOnBalls) {
        if (hits < 0 || error < 0 || baseOnBalls < 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "음수는 입력이 불가합니다.");
        }
    }

}
