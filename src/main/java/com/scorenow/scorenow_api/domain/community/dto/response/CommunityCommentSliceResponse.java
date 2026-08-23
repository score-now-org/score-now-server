package com.scorenow.scorenow_api.domain.community.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityCommentSliceResponse {

	private List<CommunityCommentResponse> comments;
	private Long nextCursor;
	private boolean hasNext;

	public static CommunityCommentSliceResponse of(List<CommunityCommentResponse> comments, Long nextCursor,
		boolean hasNext) {
		return CommunityCommentSliceResponse.builder()
			.comments(comments)
			.nextCursor(nextCursor)
			.hasNext(hasNext)
			.build();
	}
}
