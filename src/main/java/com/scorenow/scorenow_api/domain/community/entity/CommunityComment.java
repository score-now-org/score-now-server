package com.scorenow.scorenow_api.domain.community.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "community_comments",
	indexes = {
		@Index(name = "idx_community_comments_post_id_id", columnList = "post_id, id"),
		@Index(name = "idx_community_comments_root_id", columnList = "root_comment_id")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment extends BaseEntity {

	public static final int MAX_CONTENT_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_comments_post"))
	private CommunityPost post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_comments_author"))
	private User author;

	@Column(name = "author_nickname", nullable = false, length = 50)
	private String authorNickname;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id", foreignKey = @ForeignKey(name = "fk_community_comments_parent"))
	private CommunityComment parentComment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "root_comment_id", foreignKey = @ForeignKey(name = "fk_community_comments_root"))
	private CommunityComment rootComment;

	@Column(nullable = false)
	private int depth;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommunityCommentStatus status;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static CommunityComment create(CommunityPost post, User author, CommunityComment parentComment,
		String content) {
		validatePost(post);
		validateAuthor(author);
		validateContent(content);

		CommunityComment comment = new CommunityComment();
		comment.post = post;
		comment.author = author;
		comment.authorNickname = snapshotNickname(author);
		comment.parentComment = parentComment;
		comment.rootComment = resolveRootComment(parentComment);
		comment.depth = parentComment == null ? 0 : 1;
		comment.content = content.trim();
		comment.status = CommunityCommentStatus.ACTIVE;
		return comment;
	}

	public void delete(Long userId) {
		if (status != CommunityCommentStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND);
		}

		if (!Objects.equals(author.getId(), userId)) {
			throw new BusinessException(ErrorCode.COMMUNITY_FORBIDDEN);
		}

		this.status = CommunityCommentStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}

	public Long getPostId() {
		return post.getId();
	}

	public Long getAuthorId() {
		return author.getId();
	}

	public Long getParentCommentId() {
		return parentComment == null ? null : parentComment.getId();
	}

	public Long getRootCommentId() {
		return rootComment == null ? null : rootComment.getId();
	}

	private static CommunityComment resolveRootComment(CommunityComment parentComment) {
		if (parentComment == null) {
			return null;
		}

		return parentComment.rootComment == null ? parentComment : parentComment.rootComment;
	}

	private static void validatePost(CommunityPost post) {
		if (post == null || !post.isActive()) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}
	}

	private static void validateAuthor(User author) {
		if (author == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
	}

	private static void validateContent(String content) {
		if (content == null || content.isBlank() || content.trim().length() > MAX_CONTENT_LENGTH) {
			throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_INVALID);
		}
	}

	private static String snapshotNickname(User author) {
		if (author.getNickname() == null || author.getNickname().isBlank()) {
			return "익명";
		}

		return author.getNickname();
	}
}
