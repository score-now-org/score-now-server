package com.scorenow.scorenow_api.domain.chat.dto;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageResponse {

	private Long matchId;
	private String senderNickname;
	private String message;
	private LocalDateTime createdAt;

	public static ChatMessageResponse of(ChatMessage chatMessage, Long matchId) {
		return ChatMessageResponse.builder()
			.matchId(matchId)
			.senderNickname(chatMessage.getSenderNickname())
			.message(chatMessage.getMessage())
			.createdAt(chatMessage.getCreatedAt())
			.build();
	}
}
