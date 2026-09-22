package com.scorenow.scorenow_api.domain.community.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityReportCreateRequest;
import com.scorenow.scorenow_api.domain.community.service.CommunityReportService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/comments/{commentId}/reports")
public class CommunityCommentReportController {

	private final CommunityReportService communityReportService;

	@PostMapping
	public ApiResponse<Void> reportComment(
		@PathVariable Long commentId,
		@AuthenticationPrincipal Long userId,
		@Valid @RequestBody CommunityReportCreateRequest request
	) {
		communityReportService.reportComment(commentId, userId, request);
		return ApiResponse.success();
	}
}
