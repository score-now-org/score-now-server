package com.scorenow.scorenow_api.domain.community.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityReferenceService {

	private final CommunityPostRepository postRepository;
	private final CommunityCommentRepository commentRepository;

	public CommunityPost requireActivePost(Long postId) {
		return postRepository.findByIdAndStatus(postId, CommunityPostStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
	}

	public CommunityComment requireActiveComment(Long commentId) {
		return commentRepository.findByIdAndStatus(commentId, CommunityCommentStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
	}

	public CommunityComment requireActiveParentComment(Long postId, Long parentCommentId) {
		if (parentCommentId == null) {
			return null;
		}

		CommunityComment parentComment = requireActiveComment(parentCommentId);
		if (!parentComment.getPostId().equals(postId)) {
			throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
		}

		return parentComment;
	}
}
