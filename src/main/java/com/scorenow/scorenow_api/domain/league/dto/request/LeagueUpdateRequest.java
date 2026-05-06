package com.scorenow.scorenow_api.domain.league.dto.request;

import lombok.Getter;

@Getter
public class LeagueUpdateRequest {

	private Long sportId;
	private String eName;
	private String kName;
	private String sName;
}
