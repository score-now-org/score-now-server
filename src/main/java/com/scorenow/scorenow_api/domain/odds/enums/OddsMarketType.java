package com.scorenow.scorenow_api.domain.odds.enums;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public enum OddsMarketType {
    WIN_DRAW_LOSS("승무패"),
    HANDICAP("핸디캡"),
    UNDER_OVER("언더/오버");

    private final String description;

    public static List<OddsMarketType> getAllTypes() {
        return List.of(WIN_DRAW_LOSS, HANDICAP, UNDER_OVER);
    }
}
