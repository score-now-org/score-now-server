package com.scorenow.scorenow_api.domain.match.document.sportdetail.baseball;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BaseballBase {
    FIRST_BASE(1),
    SECOND_BASE(2),
    THIRD_BASE(3);

    private final int baseNumber;
}
