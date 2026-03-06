package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsLineupResponse {

	private Integer success;
	private Result results;

	@Getter
	@Setter
	public static class Result {
		private LineupSide home;
		private LineupSide away;
	}

	@Getter
	@Setter
	public static class LineupSide {
		private String formation;
		private List<LineupPlayer> startinglineup;
		private List<LineupPlayer> substitutes;
	}

	@Getter
	@Setter
	public static class LineupPlayer {

		private Player player;
		private String shirtnumber;
		private String pos;
	}

	@Getter
	@Setter
	public static class Player {
		private String id;
		private String name;
		private String cc;
	}
}

