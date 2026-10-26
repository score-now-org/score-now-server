package com.scorenow.scorenow_api.domain.odds.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OddsMarketType {
    WIN_DRAW_LOSS("승무패"),
    HANDICAP("핸디캡"),
    UNDER_OVER("언더/오버");

    private final String description;
}
