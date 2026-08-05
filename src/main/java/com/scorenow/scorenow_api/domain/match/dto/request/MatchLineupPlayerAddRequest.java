package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.model.LineupPlayerRole;

import jakarta.validation.constraints.NotNull;

public record MatchLineupPlayerAddRequest(
	@NotNull Long playerId,
	@NotNull LineupPlayerRole role) {
}
