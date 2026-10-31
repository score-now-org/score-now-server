package com.scorenow.scorenow_api.domain.odds.dto.response;

import com.scorenow.scorenow_api.domain.odds.enums.OddsCellPosition;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCellType;
import com.scorenow.scorenow_api.domain.odds.enums.OddsChangeDirection;

import java.math.BigDecimal;

public record OddsCellResponse(
        OddsCellPosition position,          // LEFT, CENTER, RIGHT
        OddsCellType type,                  // HOME, AWAY, UNDER, OVER, HANDICAP_LINE, UNDER_OVER_LINE
        BigDecimal currentValue,
        BigDecimal previousValue,
        OddsChangeDirection changeDirection
) {

    public static OddsCellResponse odds(
            OddsCellPosition position,
            OddsCellType type,
            BigDecimal currentValue,
            BigDecimal previousValue
    ) {
        return new OddsCellResponse(
                position,
                type,
                currentValue,
                previousValue,
                OddsChangeDirection.resolveDirection(currentValue, previousValue)
        );
    }

    public static OddsCellResponse line(
            OddsCellPosition position,
            OddsCellType type,
            BigDecimal currentValue
    ) {
        return new OddsCellResponse(
                position,
                type,
                currentValue,
                null,
                OddsChangeDirection.NONE
        );
    }

}
