package com.scorenow.scorenow_api.domain.match.dto.response.statusdisplay.football;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballClock;
import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballPhase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(description = "축구 진행중 경기 상태 표시 상세 응답")
public class FootballInplayStatusDisplayDetailResponse {

    @Schema(description = "축구 경기 구간 코드", example = "SECOND_HALF")
    private String phaseCode;

    @Schema(description = "축구 경기 구간명", example = "후반")
    private String phaseName;

    @Schema(description = "현재 구간 기준 경과 시간 분", example = "15")
    private Integer phaseElapsedMinutes;

    @Schema(description = "현재 구간 기준 경과 시간 초", example = "1800 (30분 = 30*60 = 1800)")
    private Integer phaseElapsedSeconds;

    @Schema(description = "타이머 진행 여부", example = "true")
    private Boolean running;

    @Schema(description = "경과 시간 동기화 기준 시각", example = "2026-07-19T05:30:00Z")
    private Instant syncedAt;

    public static FootballInplayStatusDisplayDetailResponse from(FootballClock clock) {
        if (clock == null) {
            return FootballInplayStatusDisplayDetailResponse.builder().build();
        }

        FootballPhase phase = clock.getPhase();

        return FootballInplayStatusDisplayDetailResponse.builder()
                .phaseCode(phase != null ? phase.name() : null)
                .phaseName(phase != null ? phase.getDescription() : null)
                .phaseElapsedMinutes(clock.resolvePhaseElapsedMinutes())
                .phaseElapsedSeconds(clock.getPhaseElapsedSeconds())
                .running(clock.getRunning())
                .syncedAt(clock.getClockSyncedAt())
                .build();
    }
}
