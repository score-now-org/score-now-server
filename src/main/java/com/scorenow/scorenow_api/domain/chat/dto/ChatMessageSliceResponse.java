package com.scorenow.scorenow_api.domain.chat.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageSliceResponse {

	private List<ChatMessageResponse> messages;
	private Long nextCursor;
	private boolean hasNext;

	public static ChatMessageSliceResponse of(List<ChatMessageResponse> messages, Long nextCursor, boolean hasNext) {
		return ChatMessageSliceResponse.builder().messages(messages).nextCursor(nextCursor).hasNext(hasNext).build();
	}
}