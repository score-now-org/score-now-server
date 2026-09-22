package com.scorenow.scorenow_api.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityCommentReactionRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentReactionResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;
import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentStatus;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentReactionRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityCommentRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;

@ExtendWith(MockitoExtension.class)
class CommunityCommentReactionServiceTest {

	@Mock
	private CommunityAuthService authService;

	@Mock
	private UserReferenceService userReferenceService;

	@Mock
	private CommunityReferenceService referenceService;

	@Mock
	private CommunityCommentReactionRepository reactionRepository;

	@Mock
	private CommunityCommentRepository commentRepository;

	@InjectMocks
	private CommunityCommentReactionService service;

	@Test
	void bulkUpdateAfterReturnsTheScalarLikeCount() {
		Long commentId = 10L;
		Long userId = 20L;
		CommunityComment comment = mock(CommunityComment.class);
		CommunityCommentReactionRequest request = mock(CommunityCommentReactionRequest.class);

		given(authService.requireLogin(userId)).willReturn(userId);
		given(userReferenceService.requireActiveUser(userId)).willReturn(mock(User.class));
		given(referenceService.requireActiveComment(commentId)).willReturn(comment);
		given(comment.getId()).willReturn(commentId);
		given(request.getLiked()).willReturn(true);
		given(reactionRepository.findByComment_IdAndUser_Id(commentId, userId)).willReturn(Optional.empty());
		given(commentRepository.increaseLikeCount(commentId, CommunityCommentStatus.ACTIVE)).willReturn(1);
		given(commentRepository.findLikeCountByIdAndStatus(commentId, CommunityCommentStatus.ACTIVE))
			.willReturn(Optional.of(7L));

		CommunityCommentReactionResponse response = service.react(commentId, userId, request);

		assertThat(response.getLikeCount()).isEqualTo(7L);
		then(referenceService).should().requireActiveComment(commentId);
		then(commentRepository).should().findLikeCountByIdAndStatus(commentId, CommunityCommentStatus.ACTIVE);
	}
}
