package com.scorenow.scorenow_api.external.betsapi.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsStandingsResponse {

	private Integer success;
	private List<Result> results;

	@Getter
	@Setter
	public static class Result {
		private Season season;
		private Overall overall;
	}

	@Getter
	@Setter
	public static class Season {
		private String name;
	}

	@Getter
	@Setter
	public static class Overall {
		private List<Table> tables;
	}

	@Getter
	@Setter
	public static class Table {
		private List<Row> rows;
	}

	@Getter
	@Setter
	public static class Row {
		private Team team;
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
