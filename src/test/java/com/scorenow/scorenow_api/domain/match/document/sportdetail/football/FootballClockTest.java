package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FootballClockTest {

    private static final List<FootballPhase> RUNNING_PHASES = List.of(
            FootballPhase.FIRST_HALF,
            FootballPhase.SECOND_HALF,
            FootballPhase.EXTRA_FIRST_HALF,
            FootballPhase.EXTRA_SECOND_HALF
    );

    @Test
    void 전후반과_연장전후반은_1분부터_표시한다() {
        for (FootballPhase phase : RUNNING_PHASES) {
            assertThat(FootballClock.resolveDisplayElapsedMinutes(0, phase)).isEqualTo(1);
            assertThat(FootballClock.resolveDisplayElapsedMinutes(59, phase)).isEqualTo(1);
            assertThat(FootballClock.resolveDisplayElapsedMinutes(60, phase)).isEqualTo(2);
        }
    }

    @Test
    void 타이머가_흐르지_않는_Phase는_기존_분_계산을_유지한다() {
        assertThat(FootballClock.resolveDisplayElapsedMinutes(0, FootballPhase.HALF_TIME)).isZero();
        assertThat(FootballClock.resolveDisplayElapsedMinutes(60, FootballPhase.FULL_TIME)).isEqualTo(1);
    }
}
