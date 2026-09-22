package com.scorenow.scorenow_api.domain.community.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityCommentReactionRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentReactionResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentReaction;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentReactionRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityCommentReactionService {

	private final CommunityAuthService authService;
	private final UserReferenceService userReferenceService;
	private final CommunityReferenceService referenceService;
	private final CommunityCommentReactionRepository reactionRepository;
	private final CommunityCommentRepository commentRepository;

	@Transactional
	public CommunityCommentReactionResponse react(Long commentId, Long userId, CommunityCommentReactionRequest request) {
		Long loginUserId = authService.requireLogin(userId);
		User user = userReferenceService.requireActiveUser(loginUserId);
		CommunityComment comment = referenceService.requireActiveComment(commentId);
		boolean liked = request.getLiked();

		reactionRepository.findByComment_IdAndUser_Id(commentId, loginUserId)
			.ifPresentOrElse(
				reaction -> updateReaction(commentId, reaction, liked),
				() -> createReaction(comment, user, liked)
			);

		CommunityComment updatedComment = referenceService.requireActiveComment(commentId);
		return CommunityCommentReactionResponse.of(commentId, liked, updatedComment.getLikeCount());
	}

	private void createReaction(CommunityComment comment, User user, boolean liked) {
		if (liked) {
			reactionRepository.save(CommunityCommentReaction.create(comment, user));
			updateLikeCount(comment.getId(), true);
		}
	}

	private void updateReaction(Long commentId, CommunityCommentReaction reaction, boolean liked) {
		if (!liked) {
			reactionRepository.delete(reaction);
			updateLikeCount(commentId, false);
		}
	}

	private void updateLikeCount(Long commentId, boolean increase) {
		int updatedRows = increase
			? commentRepository.increaseLikeCount(commentId, CommunityCommentStatus.ACTIVE)
			: commentRepository.decreaseLikeCount(commentId, CommunityCommentStatus.ACTIVE);
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
		}
	}
}
