package com.scorenow.scorenow_api.domain.match.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchLineupUpdateRequest {

	private String position;
	private Integer shirtNumber;
	private Integer goals;

}
