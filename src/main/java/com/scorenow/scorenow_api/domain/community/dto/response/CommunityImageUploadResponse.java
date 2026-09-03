package com.scorenow.scorenow_api.domain.community.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityImageUploadResponse {

	private String imageKey;
	private String imageUrl;

	public static CommunityImageUploadResponse of(String imageKey, String imageUrl) {
		return CommunityImageUploadResponse.builder()
			.imageKey(imageKey)
			.imageUrl(imageUrl)
			.build();
	}
}
