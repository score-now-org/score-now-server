package com.scorenow.scorenow_api.domain.chat.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageSliceResponse;
import com.scorenow.scorenow_api.domain.chat.service.ChatHistoryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat - history", description = "채팅 내역 API")
public class ChatHistoryController {

	private static final String DEFAULT_SIZE = "30";

	private final ChatHistoryService chatHistoryService;

	@Operation(summary = "채팅 내역 조회", description = """
		특정 경기의 채팅 내역을 커서 기반 무한스크롤 방식으로 조회합니다.

		1. 최초 요청은 cursor 없이 호출하면 최신 메시지부터 size개를 조회합니다.
		2. 응답의 hasNext가 true이면 nextCursor(이번 페이지 마지막 메시지의 chatId)를 다음 요청의 cursor로 전달합니다.
		3. cursor가 있으면 해당 chatId보다 오래된 메시지를 최신순으로 조회합니다.

		예: 첫 요청의 마지막 chatId가 121이면, 다음 요청은 ?cursor=121&size=30으로 호출합니다.
		각 메시지는 chatId, profileImageUrl, senderNickname, message, createdAt을 포함합니다.
		""")
	@GetMapping("/{matchId}/history")
	public ApiResponse<ChatMessageSliceResponse> getHistory(
		@Parameter(description = "경기 ID", example = "1") @PathVariable Long matchId,

		@Parameter(description = "이전 응답의 nextCursor. 해당 chatId보다 오래된 메시지를 조회합니다. 최초 요청에는 생략합니다.", example = "121") @RequestParam(required = false) Long cursor,

		@Parameter(description = "조회할 메시지 개수. 1~100, 기본값 30", example = "30") @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
		return ApiResponse.success(chatHistoryService.getMessages(matchId, cursor, size));
	}
}
