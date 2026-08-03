package com.scorenow.scorenow_api.domain.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityReportResponse {

	private Long postId;
	private long reportCount;

	public static CommunityReportResponse of(Long postId, long reportCount) {
		return CommunityReportResponse.builder()
			.postId(postId)
			.reportCount(reportCount)
			.build();
	}
}
