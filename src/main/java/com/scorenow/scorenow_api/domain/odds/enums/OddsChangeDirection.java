package com.scorenow.scorenow_api.domain.odds.enums;

import java.math.BigDecimal;

public enum OddsChangeDirection {
    UP,
    DOWN,
    SAME,
    NONE;

    public static OddsChangeDirection resolveDirection(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) {
            return OddsChangeDirection.NONE;
        }

        int comparison = current.compareTo(previous);

        if (comparison > 0) {
            return OddsChangeDirection.UP;
        }

        if (comparison < 0) {
            return OddsChangeDirection.DOWN;
        }

        return OddsChangeDirection.SAME;
    }
}
