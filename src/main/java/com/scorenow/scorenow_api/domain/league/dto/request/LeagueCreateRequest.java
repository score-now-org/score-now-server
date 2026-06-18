package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueCreateRequest {

    @NotNull(message = "종목ID 는 필수입니다.")
    private Long sportId;

    @NotBlank
    @JsonProperty("eName")
    private String eName;

    @JsonProperty("kName")
    private String kName;

    @NotBlank
    @JsonProperty("sName")
    private String sName;

    @JsonProperty("teamDisplayOrder")
    private TeamDisplayOrder teamDisplayOrder;
}
