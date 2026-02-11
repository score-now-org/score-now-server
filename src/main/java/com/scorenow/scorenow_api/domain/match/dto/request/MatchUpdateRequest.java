package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchUpdateRequest {

	private String statusCode; // NOT_STARTED, INPLAY, ENDED

	private Integer homeScore;

	private Integer awayScore;

	private Boolean isActive;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime startAt;
}
