package com.scorenow.scorenow_api.domain.community.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityCommentCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentResponse;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentSliceResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityCommentService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community")
public class CommunityCommentController {

	private static final String DEFAULT_SIZE = "30";

	private final CommunityCommentService communityCommentService;

	@GetMapping("/posts/{postId}/comments")
	public ApiResponse<CommunityCommentSliceResponse> getComments(
		@PathVariable Long postId,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = DEFAULT_SIZE) int size
	) {
		return ApiResponse.success(communityCommentService.getComments(postId, cursor, size));
	}

	@PostMapping("/posts/{postId}/comments")
	public ApiResponse<CommunityCommentResponse> createComment(
		@PathVariable Long postId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityCommentCreateRequest request
	) {
		return ApiResponse.success(communityCommentService.createComment(postId, userId, request));
	}

	@DeleteMapping("/comments/{commentId}")
	public ApiResponse<Void> deleteComment(
		@PathVariable Long commentId,
		@AuthenticationPrincipal Long userId
	) {
		communityCommentService.deleteComment(commentId, userId);
		return ApiResponse.success();
	}
}
