package com.scorenow.scorenow_api.domain.league.dto.response;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLeagueResponse {

    private Long id;
    private Long sportId;
    private String kName;
    private String eName;
    private String sName;
    private TeamDisplayOrder teamDisplayOrder;
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
