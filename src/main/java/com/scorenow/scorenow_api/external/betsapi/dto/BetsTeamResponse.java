package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsTeamResponse {
	private int success;
	private BetsPager pager;
	private List<Team> results;

	@Getter
	@Setter
	public static class Team{
		private String id;
		private String name;

		@JsonProperty("image_id")
		private String imageId;

		private String cc;
	}
}
