package com.scorenow.scorenow_api.domain.league.dto.response;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "관리자 리그 응답")
@Getter
@Builder
public class AdminLeagueResponse {

    @Schema(description = "리그 ID", example = "1")
    private Long id;

    @Schema(description = "종목 ID", example = "1")
    private Long sportId;

    @Schema(description = "한글 리그명", example = "프리미어리그")
    private String kName;

    @Schema(description = "영문 리그명", example = "Premier League")
    private String eName;

    @Schema(description = "숏 리그명", example = "EPL")
    private String sName;

    @Schema(description = "팀 표시 순서", example = "HOME_AWAY", allowableValues = {"HOME_AWAY", "AWAY_HOME"})
    private TeamDisplayOrder teamDisplayOrder;

    @Schema(description = "데이터 원천", example = "BETS", allowableValues = {"BETS", "MANUAL"})
    private DataOrigin dataOrigin;

    public static AdminLeagueResponse from(League league) {
        return AdminLeagueResponse.builder()
                .id(league.getId())
                .sportId(league.getSportId())
                .kName(league.getKName())
                .eName(league.getEName())
                .sName(league.getSName())
                .teamDisplayOrder(league.getTeamDisplayOrder())
                .dataOrigin(league.getDataOrigin())
                .build();
    }
}
