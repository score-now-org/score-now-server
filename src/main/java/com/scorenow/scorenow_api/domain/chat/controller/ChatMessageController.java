package com.scorenow.scorenow_api.domain.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageRequest;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.service.ChatMessageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/matches/{matchId}/chat/messages")
	public void sendMessage(@DestinationVariable Long matchId, @Payload ChatMessageRequest request,
		Principal principal) {
		Long userId = Long.parseLong(principal.getName());

		ChatMessageResponse response = chatMessageService.createMessage(matchId, userId, request);

		messagingTemplate.convertAndSend("/sub/matches/" + matchId + "/chat/messages", response);
	}
}