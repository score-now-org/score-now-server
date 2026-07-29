package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchUpdateRequest {

	private String statusCode; // NOT_STARTED, INPLAY, ENDED

	private Integer homeScore;

	private Integer awayScore;

	private Boolean isManual;	// 자동, 수동 경기 여부

	private Boolean isActive;	// 앱 노출 여부

	@JsonProperty("teamDisplayOrder")
	private TeamDisplayOrder teamDisplayOrder;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime startAt;
}
