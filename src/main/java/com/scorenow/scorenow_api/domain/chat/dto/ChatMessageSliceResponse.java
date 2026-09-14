package com.scorenow.scorenow_api.domain.chat.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "채팅 내역 커서 페이지 응답")
public class ChatMessageSliceResponse {

	@Schema(description = "최신순 채팅 목록")
	private List<ChatMessageResponse> messages;

	@Schema(description = "다음 조회에 사용할 cursor. 이번 페이지 마지막 메시지의 chatId이며, hasNext가 false면 null", example = "121", nullable = true)
	private Long nextCursor;

	@Schema(description = "더 오래된 채팅 존재 여부. true이면 nextCursor를 cursor로 전달해 다음 페이지를 조회합니다.", example = "true")
	private boolean hasNext;

	public static ChatMessageSliceResponse of(List<ChatMessageResponse> messages, Long nextCursor, boolean hasNext) {
		return ChatMessageSliceResponse.builder().messages(messages).nextCursor(nextCursor).hasNext(hasNext).build();
	}
}
