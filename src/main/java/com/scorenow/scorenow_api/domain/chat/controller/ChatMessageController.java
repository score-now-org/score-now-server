package com.scorenow.scorenow_api.domain.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageRequest;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.service.ChatMessageService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.websocket.principal.StompPrincipal;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/matches/{matchId}/chat/messages")
	public void sendMessage(@DestinationVariable Long matchId, @Payload ChatMessageRequest request,
		Principal principal) {
		StompPrincipal stompPrincipal = getStompPrincipal(principal);
		validateRequest(request);

		ChatMessageResponse response = chatMessageService.createMessage(matchId, stompPrincipal.getUserId(), request);

		messagingTemplate.convertAndSend("/sub/matches/" + matchId + "/chat/messages", response);
	}

	@MessageExceptionHandler(BusinessException.class)
	public void handleBusinessException(BusinessException exception, Principal principal) {
		if (principal == null) {
			return;
		}

		ErrorCode errorCode = exception.getErrorCode();
		ApiResponse<Void> response = ApiResponse.error(
			errorCode.getCode(),
			exception.getMessage(),
			exception.getDetails()
		);

		messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", response);
	}

	private StompPrincipal getStompPrincipal(Principal principal) {
		if (!(principal instanceof StompPrincipal stompPrincipal)) {
			throw new BusinessException(ErrorCode.CHAT_SEND_AUTH_REQUIRED);
		}

		return stompPrincipal;
	}

	private void validateRequest(ChatMessageRequest request) {
		if (request == null || request.getMessage() == null) {
			throw new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID);
		}
	}
}
