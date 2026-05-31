package com.scorenow.scorenow_api.domain.common.enums;

import java.util.List;

public enum TeamDisplayOrder {
    HOME_AWAY(List.of("HOME", "AWAY")),
    AWAY_HOME(List.of("AWAY", "HOME"));

    private final List<String> displaySides;

    TeamDisplayOrder(List<String> displaySides) {
        this.displaySides = displaySides;
    }

    public List<String> toDisplaySides() {
        return displaySides;
    }

}
