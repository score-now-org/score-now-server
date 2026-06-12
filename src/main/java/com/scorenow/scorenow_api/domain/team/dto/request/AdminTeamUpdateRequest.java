package com.scorenow.scorenow_api.domain.team.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminTeamUpdateRequest {

    @JsonProperty("teamType")
    private TeamType teamType;

    @JsonProperty("kName")
    private String kName;

    @JsonProperty("eName")
    private String eName;

    @JsonProperty("sName")
    private String sName;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("cc")
    private String cc;
}
