package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FootballClockChangedPayload {
    private String displayText;
    private Integer displayElapsedMinutes;
    private Integer phaseElapsedSeconds;
    private String phase;
    private Boolean running;
    private Instant syncedAt;

    public static FootballClockChangedPayload from(FootballClock clock, String displayText) {
        return FootballClockChangedPayload.builder()
                .displayText(displayText)
                .displayElapsedMinutes(clock.resolvePhaseElapsedMinutes())
                .phaseElapsedSeconds(clock.getPhaseElapsedSeconds())
                .phase(clock.getPhase() != null ? clock.getPhase().getDescription() : null)
                .running(clock.getRunning())
                .syncedAt(clock.getClockSyncedAt())
                .build();
    }

}
