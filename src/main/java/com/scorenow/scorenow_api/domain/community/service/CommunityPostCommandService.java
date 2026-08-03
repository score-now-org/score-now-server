package com.scorenow.scorenow_api.domain.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityPostCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.request.CommunityPostUpdateRequest;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostImage;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostImageRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityPostCommandService {

	private static final int MAX_IMAGE_COUNT = 10;

	private final CommunityAuthService authService;
	private final UserReferenceService userReferenceService;
	private final CommunityReferenceService referenceService;
	private final CommunityPostRepository postRepository;
	private final CommunityPostImageRepository imageRepository;

	@Transactional
	public Long createPost(Long userId, CommunityPostCreateRequest request) {
		Long loginUserId = authService.requireLogin(userId);
		User author = userReferenceService.requireActiveUser(loginUserId);
		List<String> imageKeys = normalizeImageKeys(request.getImageKeys());

		CommunityPost post = CommunityPost.create(author, request.getCategory(), request.getTitle(),
			request.getContent());
		CommunityPost savedPost = postRepository.save(post);
		saveImages(savedPost, imageKeys);
		return savedPost.getId();
	}

	@Transactional
	public void updatePost(Long postId, Long userId, CommunityPostUpdateRequest request) {
		Long loginUserId = authService.requireLogin(userId);
		List<String> imageKeys = normalizeImageKeys(request.getImageKeys());
		CommunityPost post = referenceService.requireActivePost(postId);

		post.update(loginUserId, request.getCategory(), request.getTitle(), request.getContent());
		imageRepository.deleteByPost(post);
		saveImages(post, imageKeys);
	}

	@Transactional
	public void deletePost(Long postId, Long userId) {
		Long loginUserId = authService.requireLogin(userId);
		CommunityPost post = referenceService.requireActivePost(postId);
		post.delete(loginUserId);
	}

	private void saveImages(CommunityPost post, List<String> imageKeys) {
		for (int index = 0; index < imageKeys.size(); index++) {
			imageRepository.save(CommunityPostImage.create(post, imageKeys.get(index), index));
		}
	}

	private List<String> normalizeImageKeys(List<String> imageKeys) {
		if (imageKeys == null) {
			return List.of();
		}

		if (imageKeys.size() > MAX_IMAGE_COUNT) {
			throw new BusinessException(ErrorCode.COMMUNITY_IMAGE_COUNT_EXCEEDED);
		}

		return imageKeys.stream()
			.map(imageKey -> {
				if (imageKey == null || imageKey.isBlank()) {
					throw new BusinessException(ErrorCode.COMMUNITY_IMAGE_INVALID);
				}
				return imageKey.trim();
			})
			.distinct()
			.toList();
	}
}
