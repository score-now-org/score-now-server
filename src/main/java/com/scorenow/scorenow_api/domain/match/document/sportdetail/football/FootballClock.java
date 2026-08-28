package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
@Builder(toBuilder = true)
public class FootballClock {
    private Instant clockSyncedAt;          // 시계 동기화 시각
    private Integer phaseElapsedSeconds;    // clockSyncedAt 기준 phase 경과 시간
    private FootballPhase phase;            // 경기 진행 단계
    private Boolean running;                // 시간 흐르는 여부
    private Instant providerUpdatedAt;      // 외부 API 에서 데이터를 업데이트 한 시점

    public static FootballClock empty() {
        return FootballClock.builder().build();
    }

    public FootballClock pause() {
        Instant now = Instant.now();

        return this.toBuilder()
                .clockSyncedAt(now)
                .phaseElapsedSeconds(this.phaseElapsedSeconds + (int) Duration.between(clockSyncedAt, now).getSeconds())
                .running(false)
                .build();
    }

    public FootballClock resume() {
        return this.toBuilder()
                .clockSyncedAt(Instant.now())
                .running(true)
                .build();
    }

    public FootballClock correctPhaseElapsedSeconds(Integer elapsedMinutes, Integer elapsedSeconds) {
        return this.toBuilder()
                .clockSyncedAt(Instant.now())
                .phaseElapsedSeconds(elapsedMinutes * 60 + elapsedSeconds)
                .build();
    }

    public Integer resolvePhaseElapsedMinutes() {
        if (phaseElapsedSeconds == null || phase == null) {
            return null;
        }

        return Math.max(phaseElapsedSeconds / 60, 0);
    }

    public static Integer resolvePhaseElapsedSeconds(Integer accumulatedElapsedSeconds, FootballPhase phase) {
        if (accumulatedElapsedSeconds == null || phase == null) {
            return null;
        }

        return switch (phase) {
            case FIRST_HALF -> accumulatedElapsedSeconds;
            case SECOND_HALF -> Math.max(accumulatedElapsedSeconds - 45 * 60, 0);
            case EXTRA_FIRST_HALF -> Math.max(accumulatedElapsedSeconds - 90 * 60, 0);
            case EXTRA_SECOND_HALF -> Math.max(accumulatedElapsedSeconds - 105 * 60, 0);
            default -> null;
        };
    }

    public static FootballClock startPhase(FootballPhase phase) {
        return switch (phase) {
            case FIRST_HALF,
                 SECOND_HALF,
                 EXTRA_FIRST_HALF,
                 EXTRA_SECOND_HALF -> startPhase(phase, true);

            case HALF_TIME,
                 EXTRA_TIME_WAITING,
                 EXTRA_TIME_ENDED,
                 PENALTY_SHOOTOUT,
                 FULL_TIME -> startPhase(phase, false);
        };
    }

    private static FootballClock startPhase(FootballPhase phase, boolean running) {
        return FootballClock.builder()
                .clockSyncedAt(Instant.now())
                .phaseElapsedSeconds(0)
                .phase(phase)
                .running(running)
                .build();
    }


}
