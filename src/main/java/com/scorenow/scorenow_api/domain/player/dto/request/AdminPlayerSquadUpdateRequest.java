package com.scorenow.scorenow_api.domain.player.dto.request;

import jakarta.validation.constraints.NotNull;

public record AdminPlayerSquadUpdateRequest(
	@NotNull Boolean squadOn
) {
}
