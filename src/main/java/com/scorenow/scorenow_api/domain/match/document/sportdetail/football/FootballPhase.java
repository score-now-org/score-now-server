package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FootballPhase {
    FIRST_HALF("전반"),
    HALF_TIME("하프타임"),
    SECOND_HALF("후반"),
    EXTRA_TIME_WAITING("연장 대기"),
    EXTRA_FIRST_HALF("연장 전반"),
    EXTRA_SECOND_HALF("연장 후반"),
    EXTRA_TIME_ENDED("연장 종료"),  // 현재 Bets 기반 자동 동기화에서는 사용하지 않음 (승부차기와 구분을 할 수 없음)
    PENALTY_SHOOTOUT("승부차기"),
    FULL_TIME("경기 종료");

    private final String description;
}
