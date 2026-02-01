package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsLineupResponse {

	private Integer success;
	private BetsLineupResult results;

	@Getter
	@Setter
	public static class BetsLineupResult {
		private BetsLineupSide home;
		private BetsLineupSide away;
	}

	@Getter
	@Setter
	public static class BetsLineupSide {
		private String formation;
		private List<BetsLineupPlayer> startinglineup;
		private List<BetsLineupPlayer> substitutes;
	}

	@Getter
	@Setter
	public static class BetsLineupPlayer {

		private ExternalPlayer player;
		private String shirtnumber;
		private String pos;

		@Getter
		@Setter
		public static class ExternalPlayer {
			private String id;
			private String name;
			private String cc;
		}
	}
}

