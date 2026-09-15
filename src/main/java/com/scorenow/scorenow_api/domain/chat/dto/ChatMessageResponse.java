package com.scorenow.scorenow_api.domain.chat.dto;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

	@Schema(description = "채팅 메시지 식별자", example = "150")
	private Long chatId;

	@Schema(description = "경기 ID", example = "1")
	private Long matchId;

	@Schema(description = "작성자 닉네임", example = "고도리밀장빼기")
	private String senderNickname;

	@Schema(description = "작성자의 현재 프로필 이미지 URL. 이미지가 없으면 null", example = "https://cdn.score-now.com/profiles/10.png", nullable = true)
	private String profileImageUrl;

	@Schema(description = "채팅 내용", example = "이게 프로 경기가 맞나요?")
	private String message;

	@Schema(description = "메시지 생성 시각", example = "2026-09-14T12:30:00", type = "string", format = "date-time")
	private LocalDateTime createdAt;

	public static ChatMessageResponse of(ChatMessage chatMessage, Long matchId) {
		User user = chatMessage.getUser();

		return ChatMessageResponse.builder()
			.chatId(chatMessage.getId())
			.matchId(matchId)
			.senderNickname(chatMessage.getSenderNickname())
			.profileImageUrl(user != null ? user.getProfileImageUrl() : null)
			.message(chatMessage.getMessage())
			.createdAt(chatMessage.getCreatedAt())
			.build();
	}
}
