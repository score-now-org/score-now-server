package com.scorenow.scorenow_api.domain.match.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InplayMatchDetectionDto {
	private final String matchId;

	private final String homeId;    // 홈 팀 ID
	private final String homeName;    // 홈 팀명(영문)

	private final String awayId;
	private final String awayName;
}
