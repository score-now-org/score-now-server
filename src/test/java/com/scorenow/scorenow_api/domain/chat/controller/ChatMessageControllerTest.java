package com.scorenow.scorenow_api.domain.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.security.Principal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageRequest;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.service.ChatMessageService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.websocket.principal.StompPrincipal;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

	@Mock
	private ChatMessageService chatMessageService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private ChatMessageController chatMessageController;

	@Test
	void sendMessage_creates_message_and_publishes_response() {
		Long matchId = 1L;
		Long userId = 10L;
		ChatMessageRequest request = request("hello score now");
		ChatMessageResponse response = ChatMessageResponse.builder()
			.matchId(matchId)
			.senderNickname("score-user")
			.message("hello score now")
			.createdAt(LocalDateTime.of(2026, 6, 30, 20, 15))
			.build();

		given(chatMessageService.createMessage(matchId, userId, request)).willReturn(response);

		chatMessageController.sendMessage(matchId, request, new StompPrincipal(userId));

		then(messagingTemplate).should()
			.convertAndSend("/sub/matches/" + matchId + "/chat/messages", response);
	}

	@Test
	void sendMessage_fails_when_principal_is_not_stomp_principal() {
		Principal principal = () -> "not-stomp-principal";

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatMessageController.sendMessage(1L, request("hello"), principal))
			.satisfies(exception -> assertThat(exception.getErrorCode())
				.isEqualTo(ErrorCode.CHAT_SEND_AUTH_REQUIRED));
		verifyNoInteractions(chatMessageService, messagingTemplate);
	}

	@Test
	void sendMessage_fails_when_request_is_null() {
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatMessageController.sendMessage(1L, null, new StompPrincipal(10L)))
			.satisfies(exception -> assertThat(exception.getErrorCode())
				.isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID));
		verifyNoInteractions(chatMessageService, messagingTemplate);
	}

	@Test
	void sendMessage_fails_when_request_message_is_null() {
		ChatMessageRequest request = request(null);

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatMessageController.sendMessage(1L, request, new StompPrincipal(10L)))
			.satisfies(exception -> assertThat(exception.getErrorCode())
				.isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID));
		verifyNoInteractions(chatMessageService, messagingTemplate);
	}

	@Test
	void handleBusinessException_sends_error_response_to_user_queue() {
		ArgumentCaptor<Object> responseCaptor = ArgumentCaptor.forClass(Object.class);
		BusinessException exception = new BusinessException(ErrorCode.CHAT_MESSAGE_INVALID);

		chatMessageController.handleBusinessException(exception, new StompPrincipal(10L));

		then(messagingTemplate).should()
			.convertAndSendToUser(eq("10"), eq("/queue/errors"), responseCaptor.capture());
		ApiResponse<?> response = (ApiResponse<?>) responseCaptor.getValue();

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getError().getCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID.getCode());
		assertThat(response.getError().getMessage()).isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID.getMessage());
	}

	private ChatMessageRequest request(String message) {
		ChatMessageRequest request = new ChatMessageRequest();
		ReflectionTestUtils.setField(request, "message", message);
		return request;
	}
}
