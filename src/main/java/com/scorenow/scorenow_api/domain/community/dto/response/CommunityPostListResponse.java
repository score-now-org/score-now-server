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
	private String thumbnailImageUrl;
	private long viewCount;
	private long likeCount;
	private long dislikeCount;
	private long recommendationCount;
	private long commentCount;
	private double popularScore;
	private LocalDateTime createdAt;

	public static CommunityPostListResponse of(CommunityPost post, String thumbnailImageUrl, LocalDateTime now) {
		return CommunityPostListResponse.builder()
			.id(post.getId())
			.category(post.getCategory())
			.categoryName(post.getCategory().getDisplayName())
			.title(post.getTitle())
			.authorNickname(post.getAuthorNickname())
			.thumbnailImageUrl(thumbnailImageUrl)
			.viewCount(post.getViewCount())
			.likeCount(post.getLikeCount())
			.dislikeCount(post.getDislikeCount())
			.recommendationCount(post.getRecommendationCount())
			.commentCount(post.getCommentCount())
			.popularScore(post.getPopularScore(now))
			.createdAt(post.getCreatedAt())
			.build();
	}
}
