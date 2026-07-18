package com.scorenow.scorenow_api.domain.league.dto.response;

import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeason;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandings;
import com.scorenow.scorenow_api.domain.league.entity.LeagueSeasonStandingsType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "관리자 리그 시즌 순위 관리 응답")
@Getter
@Builder
public class AdminLeagueSeasonStandingsResponse {

    /* 리그 정보 */
    @Schema(description = "리그 ID", example = "1")
    private Long leagueId;

    @Schema(description = "리그명", example = "프리미어리그")
    private String leagueName;

    /* 시즌 정보 */
    @Schema(description = "리그 시즌 ID", example = "10")
    private Long leagueSeasonId;

    @Schema(description = "시즌명", example = "2025/26")
    private String seasonName;

    @Schema(description = "현재 시즌 여부", example = "true")
    private boolean isCurrentSeason;

    /* 리그 시즌 순위 정보 (관리 타입, 이미지 등록 여부) */
    @Schema(description = "리그 시즌 순위 관리 ID", example = "100")
    private Long leagueSeasonStandingsId;

    @Schema(description = "리그 시즌 순위 관리 타입", example = "EXTERNAL_DATA", allowableValues = {"IMAGE", "EXTERNAL_DATA"})
    private LeagueSeasonStandingsType standingType;

    @Schema(description = "순위 이미지 등록 여부. 이미지 타입 관리 시 사용합니다.", example = "false")
    private Boolean standingImageRegistered;

    public static AdminLeagueSeasonStandingsResponse from(LeagueSeasonStandings seasonStanding) {

        LeagueSeason leagueSeason = seasonStanding.getLeagueSeason();
        League league = leagueSeason.getLeague();

        return AdminLeagueSeasonStandingsResponse.builder()
                .leagueId(league.getId())
                .leagueSeasonId(leagueSeason.getId())
                .leagueSeasonStandingsId(seasonStanding.getId())
                .leagueName(league.getKName() != null ? league.getKName() : league.getEName())
                .seasonName(leagueSeason.getSeasonName())
                .isCurrentSeason(leagueSeason.isCurrent())
                .standingType(seasonStanding.getStandingsType())
                .standingImageRegistered(seasonStanding.getImageUrl() != null && !seasonStanding.getImageUrl().isBlank())
                .build();
    }
}
