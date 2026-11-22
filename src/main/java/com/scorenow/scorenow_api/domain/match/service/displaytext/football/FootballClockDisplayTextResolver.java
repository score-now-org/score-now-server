package com.scorenow.scorenow_api.domain.match.service.displaytext.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class FootballClockDisplayTextResolver {

    public String resolve(FootballClock clock) {
        return resolve(clock, Instant.now());
    }

    public String resolve(FootballClock clock, Instant now) {
        if (clock == null || clock.getPhase() == null) {
            return null;
        }

        FootballPhase phase = clock.getPhase();

        // 타이머가 흐르는 Phase 가 아닌 경우에는 description 만 보여준다. (예: 하프타임, 연장대기)
        if (!phase.isClockRunningPhase()) {
            return phase.getDescription();
        }

        Instant clockSyncedAt = clock.getClockSyncedAt();
        Integer phaseElapsedSeconds = clock.getPhaseElapsedSeconds();

        if (clockSyncedAt == null || phaseElapsedSeconds == null) {
            return phase.getDescription();
        }

        int elapsedSinceSyncedAt = Boolean.TRUE.equals(clock.getRunning())
                ? (int) Math.max(Duration.between(clockSyncedAt, now).getSeconds(), 0)
                : 0;
        int calculatedPhaseElapsedSeconds = Boolean.TRUE.equals(clock.getRunning())
                ? elapsedSinceSyncedAt + phaseElapsedSeconds
                : phaseElapsedSeconds;

        Integer displayElapsedMinutes = FootballClock.resolveDisplayElapsedMinutes(
                calculatedPhaseElapsedSeconds,
                phase
        );

        return phase.getDescription() + " " + displayElapsedMinutes;
    }

}
