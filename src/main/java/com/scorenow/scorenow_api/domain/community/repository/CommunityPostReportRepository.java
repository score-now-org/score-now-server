package com.scorenow.scorenow_api.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityPostReport;

public interface CommunityPostReportRepository extends JpaRepository<CommunityPostReport, Long> {

	boolean existsByPost_IdAndReporter_Id(Long postId, Long reporterId);
}
