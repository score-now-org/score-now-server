package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsEventResponse {
	private int success;
	private BetsPager pager;
	private List<Event> results;

	@Getter
	@Setter
	public static class Event {
		private String id;

		@JsonProperty("sport_id")
		private String sportId;

		private String time;

		@JsonProperty("time_status")
		private String timeStatus;

		private BetsEventResponse.League league;
		private BetsEventResponse.Team home;
		private BetsEventResponse.Team away;
		private String ss;

		@JsonProperty("bet365_id")
		private String bet365Id;
	}

	@Getter
	@Setter
	public static class League {
		private String id;
		private String name;
		private String cc;
	}

	@Getter
	@Setter
	public static class Team {
		private String id;
		private String name;

		@JsonProperty("image_id")
		private String imageId;

		private String cc;
	}
}
