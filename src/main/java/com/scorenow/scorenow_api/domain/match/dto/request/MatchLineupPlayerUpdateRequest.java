package com.scorenow.scorenow_api.domain.match.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchLineupPlayerUpdateRequest {

	private String position;
	private String shirtNumber;
	private Integer goals;

}
