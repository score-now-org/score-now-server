package com.scorenow.scorenow_api.domain.community.entity;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "community_comment_reactions",
	uniqueConstraints = @UniqueConstraint(name = "uk_community_comment_reactions_comment_user", columnNames = {"comment_id", "user_id"}),
	indexes = @Index(name = "idx_community_comment_reactions_user", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentReaction extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_comment_reactions_comment"))
	private CommunityComment comment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_comment_reactions_user"))
	private User user;

	public static CommunityCommentReaction create(CommunityComment comment, User user) {
		CommunityCommentReaction reaction = new CommunityCommentReaction();
		reaction.comment = comment;
		reaction.user = user;
		return reaction;
	}
}
