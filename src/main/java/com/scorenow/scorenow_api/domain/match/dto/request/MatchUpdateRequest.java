package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.match.entity.MatchStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchUpdateRequest {

	private MatchStatus statusCode; // NOT_STARTED, INPLAY, ENDED

	private Integer homeScore;	// TODO: 점수 변경 아마 없을텐데 이거 추후에 삭제 고려

	private Integer awayScore;	// TODO: 점수 변경 아마 없을텐데 이거 추후에 삭제 고려

	private Boolean isManual;	// 자동, 수동 경기 여부

	private Boolean isActive;	// 앱 노출 여부

	@JsonProperty("teamDisplayOrder")
	private TeamDisplayOrder teamDisplayOrder;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime startAt;
}
