package com.scorenow.scorenow_api.domain.community.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityReactionRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityReactionResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostReaction;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;
import com.scorenow.scorenow_api.domain.community.entity.CommunityReactionType;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostReactionRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityReactionService {

	private final CommunityAuthService authService;
	private final UserReferenceService userReferenceService;
	private final CommunityReferenceService referenceService;
	private final CommunityPostReactionRepository reactionRepository;
	private final CommunityPostRepository postRepository;

	@Transactional
	public CommunityReactionResponse react(Long postId, Long userId, CommunityReactionRequest request) {
		Long loginUserId = authService.requireLogin(userId);
		User user = userReferenceService.requireActiveUser(loginUserId);
		CommunityPost post = referenceService.requireActivePost(postId);

		CommunityReactionType currentReaction = reactionRepository.findByPost_IdAndUser_Id(postId, loginUserId)
			.map(reaction -> updateExistingReaction(post.getId(), reaction, request.getType()))
			.orElseGet(() -> createReaction(post, user, request.getType()));

		CommunityPost updatedPost = referenceService.requireActivePost(postId);
		return CommunityReactionResponse.of(updatedPost.getId(), currentReaction, updatedPost.getLikeCount(),
			updatedPost.getDislikeCount());
	}

	private CommunityReactionType createReaction(CommunityPost post, User user, CommunityReactionType requestedType) {
		reactionRepository.save(CommunityPostReaction.create(post, user, requestedType));
		increaseCount(post.getId(), requestedType);
		return requestedType;
	}

	private CommunityReactionType updateExistingReaction(Long postId, CommunityPostReaction reaction,
		CommunityReactionType requestedType) {
		CommunityReactionType currentType = reaction.getType();

		if (currentType == requestedType) {
			reactionRepository.delete(reaction);
			decreaseCount(postId, currentType);
			return null;
		}

		reaction.changeType(requestedType);
		decreaseCount(postId, currentType);
		increaseCount(postId, requestedType);
		return requestedType;
	}

	private void increaseCount(Long postId, CommunityReactionType type) {
		int updatedRows = type == CommunityReactionType.LIKE
			? postRepository.increaseLikeCount(postId, CommunityPostStatus.ACTIVE)
			: postRepository.increaseDislikeCount(postId, CommunityPostStatus.ACTIVE);
		validateUpdatedPost(updatedRows);
	}

	private void decreaseCount(Long postId, CommunityReactionType type) {
		int updatedRows = type == CommunityReactionType.LIKE
			? postRepository.decreaseLikeCount(postId, CommunityPostStatus.ACTIVE)
			: postRepository.decreaseDislikeCount(postId, CommunityPostStatus.ACTIVE);
		validateUpdatedPost(updatedRows);
	}

	private void validateUpdatedPost(int updatedRows) {
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}
	}
}
