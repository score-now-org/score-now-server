package com.scorenow.scorenow_api.domain.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityCommentReactionResponse {

	private Long commentId;
	private boolean liked;
	private long likeCount;

	public static CommunityCommentReactionResponse of(Long commentId, boolean liked, long likeCount) {
		return CommunityCommentReactionResponse.builder()
			.commentId(commentId)
			.liked(liked)
			.likeCount(likeCount)
			.build();
	}
}
