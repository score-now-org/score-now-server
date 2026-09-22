package com.scorenow.scorenow_api.domain.community.entity;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "community_uploaded_images",
	uniqueConstraints = @UniqueConstraint(name = "uk_community_uploaded_images_key", columnNames = "image_key"),
	indexes = @Index(name = "idx_community_uploaded_images_owner", columnList = "owner_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityUploadedImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_uploaded_images_owner"))
	private User owner;

	@Column(name = "image_key", nullable = false, length = 500)
	private String imageKey;

	public static CommunityUploadedImage create(User owner, String imageKey) {
		CommunityUploadedImage image = new CommunityUploadedImage();
		image.owner = owner;
		image.imageKey = imageKey;
		return image;
	}
}
