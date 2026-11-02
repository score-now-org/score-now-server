package com.scorenow.scorenow_api.domain.player.dto.request;

public record AdminPlayerUpdateRequest(
	String kName,
	String eName,
	Boolean taegeuk
) {
}
