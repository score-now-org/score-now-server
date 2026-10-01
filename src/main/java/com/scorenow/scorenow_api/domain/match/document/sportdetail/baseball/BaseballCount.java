package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class BaseballCount {
    private static final int MAX_BALL_COUNTS = 3;
    private static final int MAX_STRIKE_COUNTS = 2;
    private static final int MAX_OUT_COUNTS = 2;

    private int balls;
    private int strikes;
    private int outs;

    public BaseballCount(int balls, int strikes, int outs) {
        validateCount(balls, strikes, outs);

        this.balls = balls;
        this.strikes = strikes;
        this.outs = outs;
    }

    public static BaseballCount empty() {
        return new BaseballCount(0, 0, 0);
    }

    private void validateCount(int balls, int strikes, int outs) {
        if (balls < 0 || balls > MAX_BALL_COUNTS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "볼은 0~" + MAX_BALL_COUNTS + "범위여야 합니다.");
        }

        if (strikes < 0 || strikes > MAX_STRIKE_COUNTS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "스트라이크는 0~" + MAX_STRIKE_COUNTS + "범위여야 합니다.");
        }

        if (outs < 0 || outs > MAX_OUT_COUNTS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "아웃은 0~" + MAX_OUT_COUNTS + "범위여야 합니다.");
        }
    }

}
