package com.scorenow.scorenow_api.domain.push.dto;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.push.entity.PushSendHistory;
import com.scorenow.scorenow_api.domain.push.enums.PushLandingType;
import com.scorenow.scorenow_api.domain.push.enums.PushSendStatus;

public record AdminPushHistoryResponse(
	Long id,
	String title,
	String content,
	PushLandingType landingType,
	Long matchId,
	String imageUrl,
	PushSendStatus status,
	LocalDateTime sentAt
) {
	public static AdminPushHistoryResponse from(PushSendHistory history) {
		return new AdminPushHistoryResponse(
			history.getId(),
			history.getTitle(),
			history.getContent(),
			history.getLandingType(),
			history.getMatchId(),
			history.getImageUrl(),
			history.getStatus(),
			history.getSentAt()
		);
	}

}
