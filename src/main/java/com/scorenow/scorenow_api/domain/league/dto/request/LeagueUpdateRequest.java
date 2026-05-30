package com.scorenow.scorenow_api.domain.league.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueUpdateRequest {

	private Long sportId;

	@JsonProperty("eName")
	private String eName;

	@JsonProperty("kName")
	private String kName;

	@JsonProperty("sName")
	private String sName;

	@JsonProperty("teamDisplayOrder")
	private TeamDisplayOrder teamDisplayOrder;
}
