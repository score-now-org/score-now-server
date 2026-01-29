package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.team.entity.Team;

import lombok.Data;

@Data
public class BetsEventResponse {
	private int success;
	private BetsPager pager;

	@Data
	public static class Event{
		private String id;

		@JsonProperty("sport_id")
		private String sportId;

		private String time;

		@JsonProperty("time_status")
		private String timeStatus;

		private League league;
		private Team home;
		private Team away;
		private String ss;

		@JsonProperty("bet365_id")
		private String bet365Id;
	}

	@Data
	public static class League{
		private String id;
		private String name;
		private String cc;
	}

	@Data
	public static class Team{
		private String id;
		private String name;

		@JsonProperty("image_id")
		private String imageId;

		private String cc;
	}
}
