package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.Half.BOTTOM;
import static com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball.BaseballHalfInning.Half.TOP;

// 매 이닝은 초(top), 말(bottom) 으로 구성되어 있다.
// 야구의 규칙 상, 초에는 원정팀이 공격, 말에는 홈팀이 공격을 진행한다.
// 각 이닝의 top 점수의 총합은 원정팀이 얻은 점수다.
// 각 이닝의 bottom 점수의 총합은 홈팀이 얻은 점수다.

@Getter
public enum BaseballHalfInning {
    FIRST_INNING_TOP(1, TOP, "1회초"),
    FIRST_INNING_BOTTOM(1, BOTTOM, "1회말"),
    SECOND_INNING_TOP(2, TOP, "2회초"),
    SECOND_INNING_BOTTOM(2, BOTTOM, "2회말"),
    THIRD_INNING_TOP(3, TOP, "3회초"),
    THIRD_INNING_BOTTOM(3, BOTTOM, "3회말"),
    FOURTH_INNING_TOP(4, TOP, "4회초"),
    FOURTH_INNING_BOTTOM(4, BOTTOM, "4회말"),
    FIFTH_INNING_TOP(5, TOP, "5회초"),
    FIFTH_INNING_BOTTOM(5, BOTTOM, "5회말"),
    SIXTH_INNING_TOP(6, TOP, "6회초"),
    SIXTH_INNING_BOTTOM(6, BOTTOM, "6회말"),
    SEVENTH_INNING_TOP(7, TOP, "7회초"),
    SEVENTH_INNING_BOTTOM(7, BOTTOM, "7회말"),
    EIGHTH_INNING_TOP(8, TOP, "8회초"),
    EIGHTH_INNING_BOTTOM(8, BOTTOM, "8회말"),
    NINTH_INNING_TOP(9, TOP, "9회초"),
    NINTH_INNING_BOTTOM(9, BOTTOM, "9회말"),
    TENTH_INNING_TOP(10, TOP, "10회초"),
    TENTH_INNING_BOTTOM(10, BOTTOM, "10회말"),
    ELEVENTH_INNING_TOP(11, TOP, "11회초"),
    ELEVENTH_INNING_BOTTOM(11, BOTTOM, "11회말"),
    TWELFTH_INNING_TOP(12, TOP, "12회초"),
    TWELFTH_INNING_BOTTOM(12, BOTTOM, "12회말"),
    THIRTEENTH_INNING_TOP(13, TOP, "13회초"),
    THIRTEENTH_INNING_BOTTOM(13, BOTTOM, "13회말"),
    FOURTEENTH_INNING_TOP(14, TOP, "14회초"),
    FOURTEENTH_INNING_BOTTOM(14, BOTTOM, "14회말"),
    FIFTEENTH_INNING_TOP(15, TOP, "15회초"),
    FIFTEENTH_INNING_BOTTOM(15, BOTTOM, "15회말"),
    SIXTEENTH_INNING_TOP(16, TOP, "16회초"),
    SIXTEENTH_INNING_BOTTOM(16, BOTTOM, "16회말"),
    SEVENTEENTH_INNING_TOP(17, TOP, "17회초"),
    SEVENTEENTH_INNING_BOTTOM(17, BOTTOM, "17회말"),
    EIGHTEENTH_INNING_TOP(18, TOP, "18회초"),
    EIGHTEENTH_INNING_BOTTOM(18, BOTTOM, "18회말"),
    NINETEENTH_INNING_TOP(19, TOP, "19회초"),
    NINETEENTH_INNING_BOTTOM(19, BOTTOM, "19회말"),
    TWENTIETH_INNING_TOP(20, TOP, "20회초"),
    TWENTIETH_INNING_BOTTOM(20, BOTTOM, "20회말");

    private final int inningNumber;
    private final Half half;
    private final String description;

    BaseballHalfInning(int inningNumber, Half half, String description) {
        this.inningNumber = inningNumber;
        this.half = half;
        this.description = description;
    }

    public static BaseballHalfInning getHomeHalfInning(int inningNumber) {
        for (BaseballHalfInning value : BaseballHalfInning.values()) {
            if (value.getInningNumber() == inningNumber && value.half == BOTTOM) {
                return value;
            }
        }

        throw new BusinessException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 범위의 이닝 입니다. 입력한 inning:" + inningNumber);
    }

    public static BaseballHalfInning getAwayHalfInning(int inningNumber) {
        for (BaseballHalfInning value : BaseballHalfInning.values()) {
            if (value.getInningNumber() == inningNumber && value.half == TOP) {
                return value;
            }
        }

        throw new BusinessException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 범위의 이닝 입니다. 입력한 inning:" + inningNumber);
    }

    public boolean isTop() {
        return this.getHalf() == TOP;
    }

    public boolean isBottom() {
        return this.getHalf() == BOTTOM;
    }

    public static List<BaseballHalfInning> getInningCodes(int inningNumber) {
        return Arrays.stream(values())
                .filter(halfInning -> halfInning.getInningNumber() == inningNumber)
                .toList();
    }

    public static int getMinInning() {
        return 1;
    }

    public static int getMaxInning() {
        return 20;
    }

    public enum Half {
        TOP, BOTTOM
    }

}
