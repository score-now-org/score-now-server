package com.scorenow.scorenow_api.domain.community.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityUploadedImage;

public interface CommunityUploadedImageRepository extends JpaRepository<CommunityUploadedImage, Long> {

	long countByImageKeyInAndOwner_Id(Collection<String> imageKeys, Long ownerId);
}
