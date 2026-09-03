package com.scorenow.scorenow_api.domain.community.entity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
	name = "community_posts",
	indexes = {
		@Index(name = "idx_community_posts_status_id", columnList = "status, id"),
		@Index(name = "idx_community_posts_category_status_id", columnList = "category, status, id"),
		@Index(name = "idx_community_posts_created_at", columnList = "created_at")
	}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseEntity {

	public static final int MAX_TITLE_LENGTH = 100;
	public static final int MAX_CONTENT_LENGTH = 5000;
	public static final int POPULAR_RECOMMENDATION_WEIGHT = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_posts_author"))
	private User author;

	@Column(name = "author_nickname", nullable = false, length = 50)
	private String authorNickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CommunityCategory category;

	@Column(nullable = false, length = MAX_TITLE_LENGTH)
	private String title;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "like_count", nullable = false)
	private long likeCount;

	@Column(name = "dislike_count", nullable = false)
	private long dislikeCount;

	@Column(name = "comment_count", nullable = false)
	private long commentCount;

	@Column(name = "report_count", nullable = false)
	private long reportCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommunityPostStatus status;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static CommunityPost create(User author, CommunityCategory category, String title, String content) {
		validateAuthor(author);
		validateCategory(category);
		validateTitle(title);
		validateContent(content);

		CommunityPost post = new CommunityPost();
		post.author = author;
		post.authorNickname = snapshotNickname(author);
		post.category = category;
		post.title = title.trim();
		post.content = content.trim();
		post.status = CommunityPostStatus.ACTIVE;
		return post;
	}

	public void update(Long userId, CommunityCategory category, String title, String content) {
		validateWritableBy(userId);
		validateCategory(category);
		validateTitle(title);
		validateContent(content);

		this.category = category;
		this.title = title.trim();
		this.content = content.trim();
	}

	public void delete(Long userId) {
		validateWritableBy(userId);
		this.status = CommunityPostStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}

	public void increaseViewCount() {
		this.viewCount++;
	}

	public void increaseLikeCount() {
		this.likeCount++;
	}

	public void decreaseLikeCount() {
		this.likeCount = Math.max(0, this.likeCount - 1);
	}

	public void increaseDislikeCount() {
		this.dislikeCount++;
	}

	public void decreaseDislikeCount() {
		this.dislikeCount = Math.max(0, this.dislikeCount - 1);
	}

	public void increaseCommentCount() {
		this.commentCount++;
	}

	public void decreaseCommentCount() {
		this.commentCount = Math.max(0, this.commentCount - 1);
	}

	public void increaseReportCount() {
		this.reportCount++;
	}

	public long getRecommendationCount() {
		return likeCount + dislikeCount;
	}

	public long getRawPopularScore() {
		return viewCount + getRecommendationCount() * POPULAR_RECOMMENDATION_WEIGHT;
	}

	public double getPopularScore(LocalDateTime now) {
		if (getCreatedAt() == null) {
			return getRawPopularScore();
		}

		long hoursAfterFirstDay = ChronoUnit.HOURS.between(getCreatedAt().plusHours(24), now);
		if (hoursAfterFirstDay < 6) {
			return getRawPopularScore();
		}

		long decaySteps = hoursAfterFirstDay / 6;
		return getRawPopularScore() * Math.pow(0.9, decaySteps);
	}

	public boolean isActive() {
		return status == CommunityPostStatus.ACTIVE;
	}

	private void validateWritableBy(Long userId) {
		if (!isActive()) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}

		if (!Objects.equals(author.getId(), userId)) {
			throw new BusinessException(ErrorCode.COMMUNITY_FORBIDDEN);
		}
	}

	private static void validateAuthor(User author) {
		if (author == null) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
	}

	private static void validateCategory(CommunityCategory category) {
		if (category == null) {
			throw new BusinessException(ErrorCode.COMMUNITY_INVALID_CATEGORY);
		}
	}

	private static void validateTitle(String title) {
		if (title == null || title.isBlank() || title.trim().length() > MAX_TITLE_LENGTH) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_INVALID);
		}
	}

	private static void validateContent(String content) {
		if (content == null || content.isBlank() || content.trim().length() > MAX_CONTENT_LENGTH) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_INVALID);
		}
	}

	private static String snapshotNickname(User author) {
		if (author.getNickname() == null || author.getNickname().isBlank()) {
			return "익명";
		}

		return author.getNickname();
	}
}
