package com.scorenow.scorenow_api.domain.chat.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageSliceResponse;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.match.service.MatchReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

	private static final int MAX_SIZE = 100;

	private final ChatMessageRepository chatMessageRepository;
	private final MatchReferenceService matchReferenceService;

	@Transactional(readOnly = true)
	public ChatMessageSliceResponse getMessages(Long matchId, Long cursor, int size) {
		validateSize(size);
		matchReferenceService.requireMatch(matchId);

		List<ChatMessage> messages = findMessages(matchId, cursor, size);
		boolean hasNext = hasNext(messages, size);

		List<ChatMessage> slicedMessages = sliceMessages(messages, size);
		Long nextCursor = getNextCursor(slicedMessages);

		return ChatMessageSliceResponse.of(toResponses(slicedMessages), nextCursor, hasNext);
	}

	private void validateSize(int size) {
		if (size <= 0 || size > MAX_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}
	}

	private List<ChatMessage> findMessages(Long matchId, Long cursor, int size) {
		Pageable pageable = PageRequest.of(0, size + 1);

		if (cursor == null) {
			return chatMessageRepository.findByMatch_IdOrderByIdDesc(matchId, pageable);
		}

		return chatMessageRepository.findByMatch_IdAndIdLessThanOrderByIdDesc(matchId, cursor, pageable);
	}

	private boolean hasNext(List<ChatMessage> messages, int size) {
		return messages.size() > size;
	}

	private List<ChatMessage> sliceMessages(List<ChatMessage> messages, int size) {
		if (messages.size() <= size) {
			return messages;
		}

		return messages.subList(0, size);
	}

	private Long getNextCursor(List<ChatMessage> messages) {
		if (messages.isEmpty()) {
			return null;
		}

		return messages.get(messages.size() - 1).getId();
	}

	private List<ChatMessageResponse> toResponses(List<ChatMessage> messages) {
		return messages.stream().map(ChatMessageResponse::from).toList();
	}
}
