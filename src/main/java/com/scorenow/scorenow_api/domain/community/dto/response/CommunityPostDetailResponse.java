package com.scorenow.scorenow_api.domain.community.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCategory;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityPostDetailResponse {

	private Long id;
	private CommunityCategory category;
	private String categoryName;
	private String title;
	private String content;
	private Long authorId;
	private String authorNickname;
	private long viewCount;
	private long likeCount;
	private long dislikeCount;
	private long recommendationCount;
	private long commentCount;
	private long reportCount;
	private double popularScore;
	private List<CommunityPostImageResponse> images;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public static CommunityPostDetailResponse of(CommunityPost post, List<CommunityPostImageResponse> images,
		LocalDateTime now) {
		return CommunityPostDetailResponse.builder()
			.id(post.getId())
			.category(post.getCategory())
			.categoryName(post.getCategory().getDisplayName())
			.title(post.getTitle())
			.content(post.getContent())
			.authorId(post.getAuthor().getId())
			.authorNickname(post.getAuthorNickname())
			.viewCount(post.getViewCount())
			.likeCount(post.getLikeCount())
			.dislikeCount(post.getDislikeCount())
			.recommendationCount(post.getRecommendationCount())
			.commentCount(post.getCommentCount())
			.reportCount(post.getReportCount())
			.popularScore(post.getPopularScore(now))
			.images(images)
			.createdAt(post.getCreatedAt())
			.updatedAt(post.getUpdatedAt())
			.build();
	}
}
