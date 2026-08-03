package com.scorenow.scorenow_api.domain.community.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityReactionRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityReactionResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityReactionService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts/{postId}/reaction")
public class CommunityReactionController {

	private final CommunityReactionService communityReactionService;

	@PutMapping
	public ApiResponse<CommunityReactionResponse> react(
		@PathVariable Long postId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityReactionRequest request
	) {
		return ApiResponse.success(communityReactionService.react(postId, userId, request));
	}
}
