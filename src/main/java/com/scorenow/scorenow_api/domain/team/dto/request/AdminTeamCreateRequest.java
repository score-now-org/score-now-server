package com.scorenow.scorenow_api.domain.team.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminTeamCreateRequest {

    @NotNull(message = "종목 ID는 필수입니다.")
    private Long sportId;

    @NotNull(message = "팀 타입은 필수입니다.")
    @JsonProperty("teamType")
    private TeamType teamType;

    @NotBlank(message = "한글 팀명은 필수입니다.")
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
