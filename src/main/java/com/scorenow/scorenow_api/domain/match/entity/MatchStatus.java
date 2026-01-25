package com.scorenow.scorenow_api.domain.match.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchStatus {
    NOT_STARTED("0", "경기전"),
    IN_PLAY("1", "진행중"),
    TO_BE_FIXED("2", "수정예정"),
    ENDED("3", "종료"),
    POSTPONED("4", "연기"),
    CANCELLED("5", "취소"),
    WALKOVER("6", "부전승"),
    INTERRUPTED("7", "중지"),
    ABANDONED("8", "취소"),
    RETIRED("9", "기권"),
    REMOVED("99", "삭제");

    private final String code;
    private final String description;
}