package com.scorenow.scorenow_api.domain.league.dto.response;

import com.scorenow.scorenow_api.domain.league.entity.League;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LeagueAdminResponse {

	private String id;
	private String sportId;
	private String kName;
	private String eName;
	private String sName;

	public static LeagueAdminResponse from(League league){
		return LeagueAdminResponse.builder()
			.id(league.getId())
			.sportId(league.getSportId())
			.kName(league.getKName())
			.eName(league.getEName())
			.sName(league.getSName())
			.build();
	}
}
