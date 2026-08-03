package com.scorenow.scorenow_api.domain.community.dto.request;

import com.scorenow.scorenow_api.domain.community.entity.CommunityReportReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommunityReportCreateRequest {

	@NotNull(message = "신고 사유는 필수입니다.")
	private CommunityReportReason reason;

	@Size(max = 500, message = "신고 상세 내용은 500자를 초과할 수 없습니다.")
	private String detail;
}
