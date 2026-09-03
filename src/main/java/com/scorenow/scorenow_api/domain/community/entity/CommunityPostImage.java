package com.scorenow.scorenow_api.domain.community.entity;

import com.scorenow.scorenow_api.global.entity.BaseEntity;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import jakarta.persistence.Column;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "community_post_images",
	indexes = @Index(name = "idx_community_post_images_post_order", columnList = "post_id, sort_order")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_post_images_post"))
	private CommunityPost post;

	@Column(name = "image_key", nullable = false, length = 500)
	private String imageKey;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public static CommunityPostImage create(CommunityPost post, String imageKey, int sortOrder) {
		if (post == null) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}

		if (imageKey == null || imageKey.isBlank()) {
			throw new BusinessException(ErrorCode.COMMUNITY_IMAGE_INVALID);
		}

		CommunityPostImage image = new CommunityPostImage();
		image.post = post;
		image.imageKey = imageKey.trim();
		image.sortOrder = sortOrder;
		return image;
	}
}
