package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.team.entity.Team;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchTeamCandidateResponse {

    private Long id;
    private Long sportId;
    private String name;
    private String shortName;
    private String imageUrl;

    public static MatchTeamCandidateResponse from(Team team) {
        return MatchTeamCandidateResponse.builder()
                .id(team.getId())
                .sportId(team.getSport() != null ? team.getSport().getId() : null)
                .name(team.resolveTeamName())
                .shortName(team.getSName())
                .imageUrl(team.getImageUrl())
                .build();
    }

}
