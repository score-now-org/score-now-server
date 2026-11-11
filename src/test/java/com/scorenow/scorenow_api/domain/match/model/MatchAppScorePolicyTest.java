package com.scorenow.scorenow_api.domain.match.model;

import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class MatchAppScorePolicyTest {

    @ParameterizedTest
    @EnumSource(value = MatchStatus.class, names = {"POSTPONED", "CANCELLED", "ABANDONED"})
    void 연기와_취소_상태는_앱_점수를_숨긴다(MatchStatus status) {
        assertThat(MatchAppScorePolicy.resolveScore(status, 3)).isNull();
    }

    @ParameterizedTest
    @EnumSource(
            value = MatchStatus.class,
            names = {"POSTPONED", "CANCELLED", "ABANDONED"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void 그외_상태는_저장된_점수를_유지한다(MatchStatus status) {
        assertThat(MatchAppScorePolicy.resolveScore(status, 3)).isEqualTo(3);
    }
}
