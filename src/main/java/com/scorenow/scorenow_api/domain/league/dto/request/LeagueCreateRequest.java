package com.scorenow.scorenow_api.domain.league.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LeagueCreateRequest {

	@NotBlank
	private String sportId;

	@NotBlank
	private String leagueId; // 외부 ID(BetsAPI 기준)

	@NotBlank
	private String eName;

	private String kName;

	private String sName;
}
