package com.scorenow.scorenow_api.domain.community.entity;

import com.scorenow.scorenow_api.domain.user.entity.User;
import com.scorenow.scorenow_api.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "community_comment_reports",
	uniqueConstraints = @UniqueConstraint(name = "uk_community_comment_reports_comment_reporter", columnNames = {"comment_id", "reporter_id"}),
	indexes = @Index(name = "idx_community_comment_reports_status", columnList = "status")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentReport extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_comment_reports_comment"))
	private CommunityComment comment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_community_comment_reports_reporter"))
	private User reporter;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CommunityReportReason reason;

	@Column(length = 500)
	private String detail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CommunityReportStatus status;

	public static CommunityCommentReport create(CommunityComment comment, User reporter, CommunityReportReason reason,
		String detail) {
		CommunityCommentReport report = new CommunityCommentReport();
		report.comment = comment;
		report.reporter = reporter;
		report.reason = reason;
		report.detail = detail == null ? null : detail.trim();
		report.status = CommunityReportStatus.RECEIVED;
		return report;
	}
}
