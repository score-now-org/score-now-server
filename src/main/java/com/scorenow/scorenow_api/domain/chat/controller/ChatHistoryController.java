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

		- cursor가 없으면 최신 메시지부터 조회합니다.
		- cursor가 있으면 해당 메시지 ID보다 오래된 메시지를 조회합니다.
		- 응답의 nextCursor를 다음 요청의 cursor로 사용하면 됩니다.
		""")
	@GetMapping("/{matchId}/history")
	public ApiResponse<ChatMessageSliceResponse> getHistory(
		@Parameter(description = "경기 ID", example = "1") @PathVariable Long matchId,

		@Parameter(description = "커서 메시지 ID. 해당 ID보다 오래된 메시지를 조회합니다.", example = "150") @RequestParam(required = false) Long cursor,

		@Parameter(description = "조회할 메시지 개수", example = "30") @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
		return ApiResponse.success(chatHistoryService.getMessages(matchId, cursor, size));
	}
}
