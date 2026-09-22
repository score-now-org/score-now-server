package com.scorenow.scorenow_api.domain.community.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityCommentCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.CommunityCommentCursor;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentSliceResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentSort;
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
	public CommunityCommentSliceResponse getComments(Long postId, CommunityCommentSort sort, String cursor, int size) {
		referenceService.requireActivePost(postId);
		int normalizedSize = normalizeSize(size);
		CommunityCommentSort normalizedSort = sort == null ? CommunityCommentSort.LATEST : sort;
		CommunityCommentCursor commentCursor = CommunityCommentCursor.parse(cursor, normalizedSort);
		Pageable pageable = PageRequest.of(0, normalizedSize + 1);

		List<CommunityComment> comments = getComments(postId, normalizedSort, commentCursor, pageable);

		boolean hasNext = comments.size() > normalizedSize;
		List<CommunityComment> slicedComments = hasNext ? comments.subList(0, normalizedSize) : comments;
		String nextCursor = slicedComments.isEmpty() ? null
			: CommunityCommentCursor.of(normalizedSort, slicedComments.get(slicedComments.size() - 1));

		return CommunityCommentSliceResponse.of(toResponses(slicedComments), nextCursor, hasNext);
	}

	private List<CommunityComment> getComments(Long postId, CommunityCommentSort sort,
		CommunityCommentCursor cursor, Pageable pageable) {
		if (sort == CommunityCommentSort.LATEST) {
			return cursor == null
				? commentRepository.findByPost_IdAndStatusOrderByIdDesc(postId, CommunityCommentStatus.ACTIVE, pageable)
				: commentRepository.findByPost_IdAndStatusAndIdLessThanOrderByIdDesc(postId,
					CommunityCommentStatus.ACTIVE, cursor.commentId(), pageable);
		}

		return cursor == null
			? commentRepository.findRecommendedComments(postId, CommunityCommentStatus.ACTIVE, pageable)
			: commentRepository.findRecommendedCommentsAfter(postId, CommunityCommentStatus.ACTIVE, cursor.likeCount(),
				cursor.commentId(), pageable);
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
