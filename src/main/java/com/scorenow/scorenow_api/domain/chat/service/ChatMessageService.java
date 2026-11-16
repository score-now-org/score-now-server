package com.scorenow.scorenow_api.domain.chat.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import com.scorenow.scorenow_api.domain.community.repository.CommunityUploadedImageRepository;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageRequest;
import com.scorenow.scorenow_api.domain.chat.dto.ChatMessageResponse;
import com.scorenow.scorenow_api.domain.chat.entity.ChatMessage;
import com.scorenow.scorenow_api.domain.chat.repository.ChatMessageRepository;
import com.scorenow.scorenow_api.domain.match.entity.Match;
import com.scorenow.scorenow_api.domain.match.service.MatchReferenceService;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final MatchReferenceService matchReferenceService;
	private final ChatMessageRepository chatMessageRepository;
	private final UserReferenceService userReferenceService;
	private final CommunityUploadedImageRepository uploadedImages;
	private final FileStorage fileStorage;

	@Transactional
	public ChatMessageResponse createMessage(Long matchId, Long userId, ChatMessageRequest request) {

		Match match = matchReferenceService.requireMatch(matchId);

		User sendUser = userReferenceService.requireActiveUser(userId);

		String imageUrl = null;
		if (StringUtils.hasText(request.getImageKey())) {
			String imageKey = request.getImageKey().trim();
			if (uploadedImages.countByImageKeyInAndOwner_Id(List.of(imageKey), userId) != 1)
				throw new BusinessException(ErrorCode.COMMUNITY_IMAGE_INVALID);
			imageUrl = fileStorage.getFileUrl(imageKey);
		}
		ChatMessage chatMessage = ChatMessage.create(match, sendUser, request.getMessage(), imageUrl);

		ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

		return ChatMessageResponse.of(savedMessage, matchId);
	}
}
