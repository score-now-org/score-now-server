package com.scorenow.scorenow_api.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageSliceResponse;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.service.MatchReferenceService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private MatchReferenceService matchReferenceService;

	@InjectMocks
	private ChatHistoryService chatHistoryService;

	@Test
	void getMessages_without_cursor_returns_latest_messages_with_next_cursor() {
		Long matchId = 1L;
		int size = 2;
		given(matchReferenceService.requireMatch(matchId)).willReturn(Match.builder().id(matchId).build());
		given(chatMessageRepository.findByMatch_IdOrderByIdDesc(matchId, PageRequest.of(0, size + 1)))
			.willReturn(List.of(
				message(5L, matchId, "message-5"),
				message(4L, matchId, "message-4"),
				message(3L, matchId, "message-3")
			));

		ChatMessageSliceResponse response = chatHistoryService.getMessages(matchId, null, size);

		assertThat(response.getMessages()).hasSize(2);
		assertThat(response.getMessages()).extracting(ChatMessageResponse::getMessage)
			.containsExactly("message-5", "message-4");
		assertThat(response.getNextCursor()).isEqualTo(4L);
		assertThat(response.isHasNext()).isTrue();
		then(chatMessageRepository).should(never())
			.findByMatch_IdAndIdLessThanOrderByIdDesc(anyLong(), anyLong(), any(Pageable.class));
	}

	@Test
	void getMessages_with_cursor_returns_older_messages() {
		Long matchId = 1L;
		Long cursor = 5L;
		int size = 2;
		given(matchReferenceService.requireMatch(matchId)).willReturn(Match.builder().id(matchId).build());
		given(chatMessageRepository.findByMatch_IdAndIdLessThanOrderByIdDesc(matchId, cursor, PageRequest.of(0, size + 1)))
			.willReturn(List.of(
				message(4L, matchId, "message-4"),
				message(3L, matchId, "message-3")
			));

		ChatMessageSliceResponse response = chatHistoryService.getMessages(matchId, cursor, size);

		assertThat(response.getMessages()).hasSize(2);
		assertThat(response.getMessages()).extracting(ChatMessageResponse::getMessage)
			.containsExactly("message-4", "message-3");
		assertThat(response.getNextCursor()).isEqualTo(3L);
		assertThat(response.isHasNext()).isFalse();
		then(chatMessageRepository).should(never()).findByMatch_IdOrderByIdDesc(anyLong(), any(Pageable.class));
	}

	@Test
	void getMessages_returns_null_cursor_when_history_is_empty() {
		Long matchId = 1L;
		int size = 30;
		given(matchReferenceService.requireMatch(matchId)).willReturn(Match.builder().id(matchId).build());
		given(chatMessageRepository.findByMatch_IdOrderByIdDesc(matchId, PageRequest.of(0, size + 1)))
			.willReturn(List.of());

		ChatMessageSliceResponse response = chatHistoryService.getMessages(matchId, null, size);

		assertThat(response.getMessages()).isEmpty();
		assertThat(response.getNextCursor()).isNull();
		assertThat(response.isHasNext()).isFalse();
	}

	@Test
	void getMessages_fails_when_size_is_less_than_one() {
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatHistoryService.getMessages(1L, null, 0))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
		verifyNoInteractions(matchReferenceService, chatMessageRepository);
	}

	@Test
	void getMessages_fails_when_size_exceeds_maximum() {
		assertThatExceptionOfType(BusinessException.class)
			.isThrownBy(() -> chatHistoryService.getMessages(1L, null, 101))
			.satisfies(exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PARAMETER));
		verifyNoInteractions(matchReferenceService, chatMessageRepository);
	}

	private ChatMessage message(Long id, Long matchId, String content) {
		Match match = Match.builder().id(matchId).build();
		User user = User.builder().id(10L).nickname("score-user").build();
		ChatMessage chatMessage = ChatMessage.create(match, user, content);
		ReflectionTestUtils.setField(chatMessage, "id", id);
		ReflectionTestUtils.setField(chatMessage, "createdAt", LocalDateTime.of(2026, 6, 2, 20, id.intValue()));
		return chatMessage;
	}
}
