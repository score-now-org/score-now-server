package com.scorenow.scorenow_api.domain.match.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LineupPlayer {

	private String rowId;
	private String playerId;
	private String eName;
	private String kName;

	private Integer shirtNumber;
	private String position;
	private Integer goals;

	private boolean substitute;  // startinglineup=false, substitute=true
	private boolean temp;
}
