package com.scorenow.scorenow_api.domain.community.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityCommentReactionRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityCommentReactionResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityCommentReactionService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/comments/{commentId}/reaction")
public class CommunityCommentReactionController {

	private final CommunityCommentReactionService communityCommentReactionService;

	@PutMapping
	public ApiResponse<CommunityCommentReactionResponse> react(
		@PathVariable Long commentId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityCommentReactionRequest request
	) {
		return ApiResponse.success(communityCommentReactionService.react(commentId, userId, request));
	}
}
