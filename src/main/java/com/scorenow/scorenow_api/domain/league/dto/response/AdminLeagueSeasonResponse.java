package com.scorenow.scorenow_api.domain.league.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "관리자 리그 시즌 응답")
@Getter
@NoArgsConstructor
public class AdminLeagueSeasonResponse {

    /* 리그 정보 */
    @Schema(description = "리그 ID", example = "1")
    private Long leagueId;

    @Schema(description = "리그명", example = "프리미어리그")
    private String leagueName;

    /* 시즌 정보 */
    @Schema(description = "리그 시즌 ID", example = "10")
    private Long leagueSeasonId;

    @Schema(description = "리그 시즌명", example = "2025/26")
    private String leagueSeasonName;

    @Schema(description = "리그 시즌 시작일", example = "2025-08-01")
    private LocalDate leagueSeasonStartAt;

    @Schema(description = "리그 시즌 종료일", example = "2026-05-31")
    private LocalDate leagueSeasonEndAt;

    @Schema(description = "현재 시즌 여부", example = "true")
    private boolean isCurrentSeason;

    public AdminLeagueSeasonResponse(
            Long leagueId,
            String leagueName,
            Long leagueSeasonId,
            String leagueSeasonName,
            LocalDate leagueSeasonStartAt,
            LocalDate leagueSeasonEndAt,
            boolean isCurrentSeason) {

        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.leagueSeasonId = leagueSeasonId;
        this.leagueSeasonName = leagueSeasonName;
        this.leagueSeasonStartAt = leagueSeasonStartAt;
        this.leagueSeasonEndAt = leagueSeasonEndAt;
        this.isCurrentSeason = isCurrentSeason;
    }
}
