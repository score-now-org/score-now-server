package com.scorenow.scorenow_api.domain.community.dto.response;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCategory;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityPostListResponse {

	private Long id;
	private CommunityCategory category;
	private String categoryName;
	private String title;
	private String authorNickname;
	private boolean hasImages;
	private long viewCount;
	private long commentCount;
	private LocalDateTime createdAt;

	public static CommunityPostListResponse of(CommunityPost post, boolean hasImages) {
		return CommunityPostListResponse.builder()
			.id(post.getId())
			.category(post.getCategory())
			.categoryName(post.getCategory().getDisplayName())
			.title(post.getTitle())
			.authorNickname(post.getAuthorNickname())
			.hasImages(hasImages)
			.viewCount(post.getViewCount())
			.commentCount(post.getCommentCount())
			.createdAt(post.getCreatedAt())
			.build();
	}
}
