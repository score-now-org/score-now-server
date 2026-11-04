package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchLineupPlayerResponse {

	private Long playerId;
	private DataOrigin provider;
	private String apiPlayerId;
	private String kName;
	private String eName;
	private String position;
	private Long teamId;
	private String teamName;
	private String shirtNumber;
	private boolean selected;

}
