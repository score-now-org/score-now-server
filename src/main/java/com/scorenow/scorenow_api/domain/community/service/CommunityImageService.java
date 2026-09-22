package com.scorenow.scorenow_api.domain.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.scorenow.scorenow_api.domain.community.dto.response.CommunityImageUploadResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostImageResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostImage;
import com.scorenow.scorenow_api.domain.community.entity.CommunityUploadedImage;
import com.scorenow.scorenow_api.domain.community.repository.CommunityUploadedImageRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import com.scorenow.scorenow_api.global.infra.storage.FileStorage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityImageService {

	private static final String COMMUNITY_IMAGE_DIRECTORY = "community/posts";

	private final FileStorage fileStorage;
	private final UserReferenceService userReferenceService;
	private final CommunityUploadedImageRepository uploadedImageRepository;

	public CommunityImageUploadResponse uploadImage(Long userId, MultipartFile image) {
		validateImage(image);
		User owner = userReferenceService.requireActiveUser(userId);

		String imageKey = fileStorage.uploadFileAndReturnKey(image, COMMUNITY_IMAGE_DIRECTORY);
		uploadedImageRepository.save(CommunityUploadedImage.create(owner, imageKey));
		return CommunityImageUploadResponse.of(imageKey, fileStorage.getFileUrl(imageKey));
	}

	public List<CommunityPostImageResponse> toResponses(List<CommunityPostImage> images) {
		return images.stream()
			.map(image -> CommunityPostImageResponse.of(image, fileStorage.getFileUrl(image.getImageKey())))
			.toList();
	}

	private void validateImage(MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new BusinessException(ErrorCode.COMMUNITY_IMAGE_INVALID);
		}

		String contentType = image.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BusinessException(ErrorCode.COMMUNITY_IMAGE_INVALID);
		}
	}
}
