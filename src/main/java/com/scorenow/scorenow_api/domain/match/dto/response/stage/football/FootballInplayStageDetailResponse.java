package com.scorenow.scorenow_api.domain.match.dto.response.stage.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FootballInplayStageDetailResponse {

    private String phaseCode;
    private String phaseName;
    private Integer elapsedMinutes;
    private Integer elapsedSeconds;
    private Integer phaseElapsedMinutes;
    private Boolean running;
    private Instant providerUpdatedAt;

    public static FootballInplayStageDetailResponse from(FootballClock clock) {
        if (clock == null) {
            return FootballInplayStageDetailResponse.builder().build();
        }

        FootballPhase phase = clock.getPhase();

        return FootballInplayStageDetailResponse.builder()
                .phaseCode(phase != null ? phase.name() : null)
                .phaseName(phase != null ? phase.getDescription() : null)
                .elapsedMinutes(clock.getElapsedMinutes())
                .elapsedSeconds(clock.getElapsedSeconds())
                .phaseElapsedMinutes(clock.resolvePhaseElapsedMinutes())
                .running(clock.getRunning())
                .providerUpdatedAt(clock.getProviderUpdatedAt())
                .build();
    }
}
