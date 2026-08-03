package com.scorenow.scorenow_api.domain.community.dto.response;

import java.time.LocalDateTime;

import com.scorenow.scorenow_api.domain.community.entity.CommunityComment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityCommentResponse {

	private Long id;
	private Long postId;
	private Long authorId;
	private String authorNickname;
	private Long parentCommentId;
	private Long rootCommentId;
	private int depth;
	private String content;
	private LocalDateTime createdAt;

	public static CommunityCommentResponse of(CommunityComment comment) {
		return CommunityCommentResponse.builder()
			.id(comment.getId())
			.postId(comment.getPostId())
			.authorId(comment.getAuthorId())
			.authorNickname(comment.getAuthorNickname())
			.parentCommentId(comment.getParentCommentId())
			.rootCommentId(comment.getRootCommentId())
			.depth(comment.getDepth())
			.content(comment.getContent())
			.createdAt(comment.getCreatedAt())
			.build();
	}
}
