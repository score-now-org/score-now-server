package com.scorenow.scorenow_api.domain.match.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchListResponse {
	private String id;
	private String sportId;
	private String sportName;
	private String leagueId;
	private String leagueName;
	private String matchType;
	private String statusCode;
	private String statusName;
	private String homeId;
	private String homeName;
	private String homeImageUrl;
	private Integer homeScore;
	private String awayId;
	private String awayName;
	private String awayImageUrl;
	private Integer awayScore;
	private boolean isManual;
	private boolean isActive;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime startAt;
}
