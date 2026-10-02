package com.scorenow.scorenow_api.domain.community.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityPostSliceResponse {

	private List<CommunityPostListResponse> posts;
	private Long nextCursor;
	private Long nextCursorLikeCount;
	private boolean hasNext;

	public static CommunityPostSliceResponse of(List<CommunityPostListResponse> posts, Long nextCursor,
		Long nextCursorLikeCount, boolean hasNext) {
		return CommunityPostSliceResponse.builder()
			.posts(posts)
			.nextCursor(nextCursor)
			.nextCursorLikeCount(nextCursorLikeCount)
			.hasNext(hasNext)
			.build();
	}
}
