package com.scorenow.scorenow_api.domain.player.dto.request;

import java.time.LocalDate;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;

public record AdminPlayerCreateRequest(
	Long sportId,
	Long leagueId,
	Long teamId,
	String kName,
	String eName,
	String shirtNumber,
	String cc,
	LocalDate birthdate,
	Integer height,
	DataOrigin provider,
	String apiPlayerId
) {
}
