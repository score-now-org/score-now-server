package com.scorenow.scorenow_api.domain.community.dto.response;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPostImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityPostImageResponse {

	private Long id;
	private String imageKey;
	private String imageUrl;
	private int sortOrder;

	public static CommunityPostImageResponse of(CommunityPostImage image, String imageUrl) {
		return CommunityPostImageResponse.builder()
			.id(image.getId())
			.imageKey(image.getImageKey())
			.imageUrl(imageUrl)
			.sortOrder(image.getSortOrder())
			.build();
	}
}
