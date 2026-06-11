package com.scorenow.scorenow_api.domain.team.dto.response;

import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminTeamResponse {

    private Long id;
    private Long sportId;
    private TeamType teamType;
    private String kName;
    private String eName;
    private String sName;
    private String imageUrl;
    private String cc;

    public static AdminTeamResponse from(Team team) {
        return AdminTeamResponse.builder()
                .id(team.getId())
                .sportId(team.getSport() != null ? team.getSport().getId() : null)
                .teamType(team.getType())
                .kName(team.getKName())
                .eName(team.getEName())
                .sName(team.getSName())
                .imageUrl(team.getImageUrl())
                .cc(team.getCc())
                .build();
    }
}
