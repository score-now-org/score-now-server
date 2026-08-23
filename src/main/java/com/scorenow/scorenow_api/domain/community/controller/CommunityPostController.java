package com.scorenow.scorenow_api.domain.community.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityPostCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.request.CommunityPostUpdateRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostDetailResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostListResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityPostSliceResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityBoardType;
import com.scorenow.scorenow_api.domain.community.service.CommunityPostCommandService;
import com.scorenow.scorenow_api.domain.community.service.CommunityPostQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts")
public class CommunityPostController {

	private static final String DEFAULT_SIZE = "20";
	private static final String DEFAULT_POPULAR_SIZE = "5";
	private static final String DEFAULT_POPULAR_TOP_SIZE = "3";

	private final CommunityPostCommandService commandService;
	private final CommunityPostQueryService queryService;

	@GetMapping
	public ApiResponse<CommunityPostSliceResponse> getPosts(
		@RequestParam(defaultValue = "ALL") CommunityBoardType boardType,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = DEFAULT_SIZE) int size
	) {
		return ApiResponse.success(queryService.getPosts(boardType, cursor, size));
	}

	@GetMapping("/me")
	public ApiResponse<CommunityPostSliceResponse> getMyPosts(
		@AuthenticationPrincipal Long userId,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = DEFAULT_SIZE) int size
	) {
		return ApiResponse.success(queryService.getMyPosts(userId, cursor, size));
	}

	@GetMapping("/popular-rolling")
	public ApiResponse<List<CommunityPostListResponse>> getPopularRollingPosts(
		@RequestParam(defaultValue = DEFAULT_POPULAR_SIZE) int size
	) {
		return ApiResponse.success(queryService.getPopularRollingPosts(size));
	}

	@GetMapping("/popular-top")
	public ApiResponse<List<CommunityPostListResponse>> getPopularTopPosts(
		@RequestParam(defaultValue = DEFAULT_POPULAR_TOP_SIZE) int size
	) {
		return ApiResponse.success(queryService.getPopularTopPosts(size));
	}

	@GetMapping("/{postId}")
	public ApiResponse<CommunityPostDetailResponse> getPost(@PathVariable Long postId) {
		return ApiResponse.success(queryService.getPostDetail(postId));
	}

	@PostMapping
	public ApiResponse<Long> createPost(
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityPostCreateRequest request
	) {
		return ApiResponse.success(commandService.createPost(userId, request));
	}

	@PatchMapping("/{postId}")
	public ApiResponse<Void> updatePost(
		@PathVariable Long postId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityPostUpdateRequest request
	) {
		commandService.updatePost(postId, userId, request);
		return ApiResponse.success();
	}

	@DeleteMapping("/{postId}")
	public ApiResponse<Void> deletePost(
		@PathVariable Long postId,
		@AuthenticationPrincipal Long userId
	) {
		commandService.deletePost(postId, userId);
		return ApiResponse.success();
	}
}
