package com.scorenow.scorenow_api.domain.push.dto;

import com.scorenow.scorenow_api.domain.push.enums.PushLandingType;
import com.scorenow.scorenow_api.domain.push.enums.PushPlatform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminPushSendRequest(
	@NotNull
	PushPlatform platform,

	@NotBlank
	String title,

	@NotBlank
	String content,

	@NotNull
	PushLandingType landingType,

	Long matchId,

	String imageUrl

) {
}
