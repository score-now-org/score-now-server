package com.scorenow.scorenow_api.domain.community.entity;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "community_post_reactions",
	uniqueConstraints = @UniqueConstraint(name = "uk_community_post_reactions_post_user", columnNames = {"post_id", "user_id"}),
	indexes = @Index(name = "idx_community_post_reactions_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostReaction extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_post_reactions_post"))
	private CommunityPost post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_post_reactions_user"))
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommunityReactionType type;

	public static CommunityPostReaction create(CommunityPost post, User user, CommunityReactionType type) {
		CommunityPostReaction reaction = new CommunityPostReaction();
		reaction.post = post;
		reaction.user = user;
		reaction.type = type;
		return reaction;
	}

	public void changeType(CommunityReactionType type) {
		this.type = type;
	}
}
