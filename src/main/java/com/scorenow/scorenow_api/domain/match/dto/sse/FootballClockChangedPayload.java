package com.scorenow.scorenow_api.domain.match.dto.sse;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FootballClockChangedPayload {
    private String displayText;
    private Integer elapsedMinutes;
    private Integer displayElapsedMinutes;
    private Integer elapsedSeconds;
    private String phase;
    private Boolean running;
    private Instant providerUpdatedAt;

    public static FootballClockChangedPayload from(FootballClock footballClock, String displayText) {
        return FootballClockChangedPayload.builder()
                .displayText(displayText)
                .elapsedMinutes(footballClock.getElapsedMinutes())
                .displayElapsedMinutes(footballClock.resolvePhaseElapsedMinutes())
                .elapsedSeconds(footballClock.getElapsedSeconds())
                .phase(footballClock.getPhase().getDescription())
                .running(footballClock.getRunning())
                .providerUpdatedAt(footballClock.getProviderUpdatedAt())
                .build();
    }

}
