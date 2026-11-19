package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.NotNull;

public record MatchLineupPlayerSwapRequest(
	@NotNull Long starterPlayerId,
	@NotNull Long substitutePlayerId
) {
}
