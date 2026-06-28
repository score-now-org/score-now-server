package com.scorenow.scorenow_api.domain.match.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchLineupPlayerResponse {

	private String playerId;
	private String kName;
	private String eName;
	private String position;
	private String teamId;
	private String teamName;
	private String shirtNumber;
	private boolean selected;

}
