package com.scorenow.scorenow_api.domain.match.dto.request;

import jakarta.validation.constraints.NotNull;

public record MatchLineupPlayerRoleUpdateRequest(
	@NotNull Role role
) {
	public enum Role {
		STARTER,
		SUBSTITUTE
	}
}
