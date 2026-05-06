package com.scorenow.scorenow_api.domain.match.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InplayScanDto {
	private final Long matchId;
	private final String apiMatchId;

	private final Long sportId;

	private final Long homeId;
	private final String homeName;
	private final String apiHomeTeamId;

	private final Long awayId;
	private final String awayName;
	private final String apiAwayTeamId;
}
