package com.scorenow.scorenow_api.domain.match.service.displaytext.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import org.springframework.stereotype.Component;

@Component
public class FootballClockDisplayTextResolver {

    public String resolve(FootballClock clock) {

        if (clock == null || clock.getPhase() == null) {
            return null;
        }

        FootballPhase phase = clock.getPhase();

        if (!hasMinuteDisplay(phase)) {
            return phase.getDescription();
        }

        // TODO: 수동 경기의 경우 elapsedMinutes 가 없음. 그래서 후반 15 분여도, 후반으로만 내려감 (SSE 기준)
        Integer phaseElapsedMinutes = clock.resolvePhaseElapsedMinutes();
        if (phaseElapsedMinutes == null) {
            return phase.getDescription();
        }

        return phase.getDescription() + " " + phaseElapsedMinutes;
    }

    private boolean hasMinuteDisplay(FootballPhase phase) {
        return switch (phase) {
            case FIRST_HALF,
                 SECOND_HALF,
                 EXTRA_FIRST_HALF,
                 EXTRA_SECOND_HALF -> true;
            case HALF_TIME,
                 EXTRA_TIME_WAITING,
                 EXTRA_TIME_ENDED,
                 PENALTY_SHOOTOUT,
                 FULL_TIME -> false;
        };
    }
}
