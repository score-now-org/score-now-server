package com.scorenow.scorenow_api.domain.community.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.scorenow.scorenow_api.domain.community.dto.response.CommunityImageUploadResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityAuthService;
import com.scorenow.scorenow_api.domain.community.service.CommunityImageService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/images")
public class CommunityImageController {

	private final CommunityAuthService authService;
	private final CommunityImageService communityImageService;

	@PostMapping
	public ApiResponse<CommunityImageUploadResponse> uploadImage(
		@AuthenticationPrincipal Long userId,
		@RequestPart("image") MultipartFile image
	) {
		authService.requireLogin(userId);
		return ApiResponse.success(communityImageService.uploadImage(image));
	}
}
