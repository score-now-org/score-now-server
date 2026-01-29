package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import lombok.Data;

@Data
public class BetsLeagueResponse {
	private int success;
	private BetsPager pager;
	private List<League> results;

	@Data
	public static class League{
		private String id;
		private String name;
		private String cc;
	}
}
