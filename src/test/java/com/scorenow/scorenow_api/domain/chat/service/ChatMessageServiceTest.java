package com.scorenow.scorenow_api.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageRequest;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.service.MatchReferenceService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

	@Mock
	private MatchReferenceService matchReferenceService;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private UserReferenceService userReferenceService;

	@InjectMocks
	private ChatMessageService chatMessageService;

	@Test
	void createMessage_saves_chat_message_and_returns_response() {
		Long matchId = 1L;
		Long userId = 10L;
		Match match = Match.builder().id(matchId).build();
		User user = User.builder().id(userId).nickname("score-user").build();
		LocalDateTime createdAt = LocalDateTime.of(2026, 6, 2, 20, 15);

		given(matchReferenceService.requireMatch(matchId)).willReturn(match);
		given(userReferenceService.requireActiveUser(userId)).willReturn(user);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
			ChatMessage savedMessage = invocation.getArgument(0);
			ReflectionTestUtils.setField(savedMessage, "id", 100L);
			ReflectionTestUtils.setField(savedMessage, "createdAt", createdAt);
			return savedMessage;
		});

		ChatMessageResponse response = chatMessageService.createMessage(matchId, userId, request("  hello score now  "));

		ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
		then(chatMessageRepository).should().save(captor.capture());
		ChatMessage savedMessage = captor.getValue();

		assertThat(savedMessage.getMatchId()).isEqualTo(matchId);
		assertThat(savedMessage.getUserId()).isEqualTo(userId);
		assertThat(savedMessage.getSenderNickname()).isEqualTo("score-user");
		assertThat(savedMessage.getMessage()).isEqualTo("hello score now");
		assertThat(response.getMatchId()).isEqualTo(matchId);
		assertThat(response.getSenderNickname()).isEqualTo("score-user");
		assertThat(response.getMessage()).isEqualTo("hello score now");
		assertThat(response.getCreatedAt()).isEqualTo(createdAt);
	}

	@Test
	void createMessage_does_not_save_blank_message() {
		Long matchId = 1L;
		Long userId = 10L;
		Match match = Match.builder().id(matchId).build();
		User user = User.builder().id(userId).nickname("score-user").build();

		given(matchReferenceService.requireMatch(matchId)).willReturn(match);
		given(userReferenceService.requireActiveUser(userId)).willReturn(user);

		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatMessageService.createMessage(matchId, userId, request("  ")))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_INVALID));
		then(chatMessageRepository).should(never()).save(any(ChatMessage.class));
	}

	private ChatMessageRequest request(String message) {
		ChatMessageRequest request = new ChatMessageRequest();
		ReflectionTestUtils.setField(request, "message", message);
		return request;
	}
}
