package com.scorenow.scorenow_api.domain.community.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityReportCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityReportResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityReportService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts/{postId}/reports")
public class CommunityReportController {

	private final CommunityReportService communityReportService;

	@PostMapping
	public ApiResponse<CommunityReportResponse> reportPost(
		@PathVariable Long postId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityReportCreateRequest request
	) {
		return ApiResponse.success(communityReportService.reportPost(postId, userId, request));
	}
}
