package com.scorenow.scorenow_api.domain.league.dto.response;

import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeagueAdminResponse {

    private Long id;
    private Long sportId;
    private String kName;
    private String eName;
    private String sName;
    private TeamDisplayOrder teamDisplayOrder;

    public static LeagueAdminResponse from(League league) {
        return LeagueAdminResponse.builder()
                .id(league.getId())
                .sportId(league.getSportId())
                .kName(league.getKName())
                .eName(league.getEName())
                .sName(league.getSName())
                .teamDisplayOrder(league.getTeamDisplayOrder())
                .build();
    }
}
