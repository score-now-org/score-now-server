package com.scorenow.scorenow_api.domain.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scorenow.scorenow_api.domain.community.entity.CommunityCommentReport;

public interface CommunityCommentReportRepository extends JpaRepository<CommunityCommentReport, Long> {

	boolean existsByComment_IdAndReporter_Id(Long commentId, Long reporterId);
}
