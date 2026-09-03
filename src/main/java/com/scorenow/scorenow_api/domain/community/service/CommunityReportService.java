package com.scorenow.scorenow_api.domain.community.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scorenow.scorenow_api.domain.community.dto.request.CommunityReportCreateRequest;
import com.scorenow.scorenow_api.domain.community.dto.response.CommunityReportResponse;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPost;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostReport;
import com.scorenow.scorenow_api.domain.community.entity.CommunityPostStatus;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostReportRepository;
import com.scorenow.scorenow_api.domain.community.repository.CommunityPostRepository;
import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityReportService {

	private final CommunityAuthService authService;
	private final UserReferenceService userReferenceService;
	private final CommunityReferenceService referenceService;
	private final CommunityPostReportRepository reportRepository;
	private final CommunityPostRepository postRepository;

	@Transactional
	public CommunityReportResponse reportPost(Long postId, Long userId, CommunityReportCreateRequest request) {
		Long loginUserId = authService.requireLogin(userId);
		User reporter = userReferenceService.requireActiveUser(loginUserId);
		CommunityPost post = referenceService.requireActivePost(postId);

		if (reportRepository.existsByPost_IdAndReporter_Id(postId, loginUserId)) {
			throw new BusinessException(ErrorCode.COMMUNITY_REPORT_DUPLICATED);
		}

		reportRepository.save(CommunityPostReport.create(post, reporter, request.getReason(), request.getDetail()));
		validateUpdatedPost(postRepository.increaseReportCount(postId, CommunityPostStatus.ACTIVE));

		CommunityPost updatedPost = referenceService.requireActivePost(postId);
		return CommunityReportResponse.of(updatedPost.getId(), updatedPost.getReportCount());
	}

	private void validateUpdatedPost(int updatedRows) {
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
		}
	}
}
