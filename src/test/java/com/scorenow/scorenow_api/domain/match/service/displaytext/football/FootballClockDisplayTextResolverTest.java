package com.scorenow.scorenow_api.domain.match.service.displaytext.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FootballClockDisplayTextResolverTest {

    private final FootballClockDisplayTextResolver resolver = new FootballClockDisplayTextResolver();

    @Test
    void 전후반과_연장전후반_시작_표시문구는_1분이다() {
        List<FootballPhase> phases = List.of(
                FootballPhase.FIRST_HALF,
                FootballPhase.SECOND_HALF,
                FootballPhase.EXTRA_FIRST_HALF,
                FootballPhase.EXTRA_SECOND_HALF
        );

        for (FootballPhase phase : phases) {
            FootballClock clock = FootballClock.builder()
                    .phase(phase)
                    .phaseElapsedSeconds(0)
                    .clockSyncedAt(Instant.now())
                    .running(false)
                    .build();

            assertThat(resolver.resolve(clock)).isEqualTo(phase.getDescription() + " 1");
        }
    }

    @Test
    void 타이머가_흐르지_않는_Phase는_시간없이_구간명만_표시한다() {
        FootballClock clock = FootballClock.builder()
                .phase(FootballPhase.HALF_TIME)
                .phaseElapsedSeconds(0)
                .clockSyncedAt(Instant.now())
                .running(false)
                .build();

        assertThat(resolver.resolve(clock)).isEqualTo("하프타임");
    }
}
