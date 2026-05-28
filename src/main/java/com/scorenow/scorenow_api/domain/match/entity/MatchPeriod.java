package com.scorenow.scorenow_api.domain.match.entity;

import lombok.Getter;

@Getter
public enum MatchPeriod {
    FIRST_HALF(0, "전반전"),
    SECOND_HALF(1, "후반전");

    private final Integer code;
    private final String description;

    MatchPeriod(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MatchPeriod fromCode(Integer code) {
        for (MatchPeriod matchPeriod : MatchPeriod.values()) {
            if (matchPeriod.code.equals(code)) {
                return matchPeriod;
            }
        }

        return null;
    }
}
