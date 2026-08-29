package com.scorenow.scorenow_api.domain.match.service.displaytext.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class FootballClockDisplayTextResolver {

    public String resolve(FootballClock clock) {

        if (clock == null || clock.getPhase() == null) {
            return null;
        }

        FootballPhase phase = clock.getPhase();

        // 타이머가 흐르는 Phase 가 아닌 경우에는 description 만 보여준다. (예: 하프타임, 연장대기)
        if (!phase.isClockRunningPhase()) {
            return phase.getDescription();
        }

        Instant now = Instant.now();
        Instant clockSyncedAt = clock.getClockSyncedAt();
        Integer phaseElapsedSeconds = clock.getPhaseElapsedSeconds();

        if (clockSyncedAt == null || phaseElapsedSeconds == null) {
            return phase.getDescription();
        }

        int calculatedPhaseElapsedSeconds = (int) Duration.between(clockSyncedAt, now).getSeconds() + phaseElapsedSeconds;

        return phase.getDescription() + " " + (calculatedPhaseElapsedSeconds / 60);
    }

}
