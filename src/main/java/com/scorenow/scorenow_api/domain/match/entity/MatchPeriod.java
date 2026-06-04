package com.scorenow.scorenow_api.domain.match.entity;

import lombok.Getter;

@Getter
public enum MatchPeriod {
    FIRST_HALF(0, "전반전"),
    SECOND_HALF(1, "후반전"),
    EXTRA_FIRST_HALF(2, "연장전 전반"),
    EXTRA_SECOND_HALF(3, "연장전 후반"),
    PENALTY_SHOOTOUT(4, "승부차기");

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
