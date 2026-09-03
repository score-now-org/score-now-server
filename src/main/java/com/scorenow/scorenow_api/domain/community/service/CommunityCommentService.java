package com.scorenow.scorenow_api.domain.community.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityCommentCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentSliceResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityCommentService {

	private static final int DEFAULT_SIZE = 30;
	private static final int MAX_SIZE = 100;

	private final CommunityAuthService authService;
	private final UserReferenceService userReferenceService;
	private final CommunityReferenceService referenceService;
	private final CommunityCommentRepository commentRepository;
	private final CommunityPostRepository postRepository;

	@Transactional(readOnly = true)
	public CommunityCommentSliceResponse getComments(Long postId, Long cursor, int size) {
		referenceService.requireActivePost(postId);
		int normalizedSize = normalizeSize(size);
		Pageable pageable = PageRequest.of(0, normalizedSize + 1);

		List<CommunityComment> comments = cursor == null
			? commentRepository.findByPost_IdAndStatusOrderByIdAsc(postId, CommunityCommentStatus.ACTIVE, pageable)
			: commentRepository.findByPost_IdAndStatusAndIdGreaterThanOrderByIdAsc(postId,
				CommunityCommentStatus.ACTIVE, cursor, pageable);

		boolean hasNext = comments.size() > normalizedSize;
		List<CommunityComment> slicedComments = hasNext ? comments.subList(0, normalizedSize) : comments;
		Long nextCursor = slicedComments.isEmpty() ? null : slicedComments.get(slicedComments.size() - 1).getId();

		return CommunityCommentSliceResponse.of(toResponses(slicedComments), nextCursor, hasNext);
	}

	@Transactional
	public CommunityCommentResponse createComment(Long postId, Long userId, CommunityCommentCreateRequest request) {
		Long loginUserId = authService.requireLogin(userId);
		User author = userReferenceService.requireActiveUser(loginUserId);
		CommunityPost post = referenceService.requireActivePost(postId);
		CommunityComment parentComment = referenceService.requireActiveParentComment(postId, request.getParentCommentId());

		CommunityComment savedComment = commentRepository.save(
			CommunityComment.create(post, author, parentComment, request.getContent())
		);
		CommunityCommentResponse response = CommunityCommentResponse.of(savedComment);
		validateUpdatedPost(postRepository.increaseCommentCount(postId, CommunityPostStatus.ACTIVE));

		return response;
	}

	@Transactional
	public void deleteComment(Long commentId, Long userId) {
		Long loginUserId = authService.requireLogin(userId);
		CommunityComment comment = referenceService.requireActiveComment(commentId);
		Long postId = comment.getPostId();

		comment.delete(loginUserId);
		validateUpdatedPost(postRepository.decreaseCommentCount(postId, CommunityPostStatus.ACTIVE));
	}

	private List<CommunityCommentResponse> toResponses(List<CommunityComment> comments) {
		return comments.stream()
			.map(CommunityCommentResponse::of)
			.toList();
	}

	private int normalizeSize(int size) {
		if (size == 0) {
			return DEFAULT_SIZE;
		}

		if (size < 0 || size > MAX_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_PARAMETER);
		}

		return size;
	}

	private void validateUpdatedPost(int updatedRows) {
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}
	}
}
