package com.scorenow.scorenow_api.domain.common.enums;

import java.util.List;

public enum TeamDisplayOrder {
    HOME_AWAY(List.of("HOME", "AWAY"), "홈-어웨이"),
    AWAY_HOME(List.of("AWAY", "HOME"), "어웨이-홈");

    private final List<String> displaySides;
    private final String description;

    TeamDisplayOrder(List<String> displaySides, String description) {
        this.displaySides = displaySides;
        this.description = description;
    }

    public List<String> toDisplaySides() {
        return displaySides;
    }

    public String getDescription() {
        return description;
    }
}
