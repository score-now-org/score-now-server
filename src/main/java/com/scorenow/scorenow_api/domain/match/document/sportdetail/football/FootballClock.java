package com.scorenow.scorenow_api.domain.match.document.sportdetail.football;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FootballClock {
    private Integer elapsedMinutes;     // 경과 시간 (분)
    private Integer elapsedSeconds;     // 경과 시간 (초)
    private FootballPhase phase;        // 경기 진행 단계
    private Boolean running;            // 시간 흐르는 여부
    private Instant providerUpdatedAt;  // 외부 API 에서 데이터를 업데이트 한 시점

    public static FootballClock empty() {
        return FootballClock.builder().build();
    }

    public Integer resolvePhaseElapsedMinutes() {
        if (elapsedMinutes == null || phase == null) {
            return null;
        }

        return switch (phase) {
            case FIRST_HALF -> elapsedMinutes;
            case SECOND_HALF -> Math.max(elapsedMinutes - 45, 0);
            case EXTRA_FIRST_HALF -> Math.max(elapsedMinutes - 90, 0);
            case EXTRA_SECOND_HALF -> Math.max(elapsedMinutes - 105, 0);
            default -> null;
        };
    }
}
