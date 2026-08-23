package com.scorenow.scorenow_api.domain.community.dto.response;

import com.scorenow.scorenow_api.domain.community.entity.CommunityReactionType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityReactionResponse {

	private Long postId;
	private CommunityReactionType currentReaction;
	private long likeCount;
	private long dislikeCount;
	private long recommendationCount;

	public static CommunityReactionResponse of(Long postId, CommunityReactionType currentReaction, long likeCount,
		long dislikeCount) {
		return CommunityReactionResponse.builder()
			.postId(postId)
			.currentReaction(currentReaction)
			.likeCount(likeCount)
			.dislikeCount(dislikeCount)
			.recommendationCount(likeCount + dislikeCount)
			.build();
	}
}
