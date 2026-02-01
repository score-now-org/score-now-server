package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsLeagueResponse {
	private int success;
	private BetsPager pager;
	private List<League> results;

	@Getter
	@Setter
	public static class League {
		private String id;
		private String name;
		private String cc;
	}
}
