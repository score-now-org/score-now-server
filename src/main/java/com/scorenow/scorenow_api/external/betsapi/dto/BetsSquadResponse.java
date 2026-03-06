package com.scorenow.scorenow_api.external.betsapi.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BetsSquadResponse {

	private Integer success;
	private List<SquadPlayer> results;

	@Getter
	@Setter
	public static class SquadPlayer {
		private String id;
		private String name;
		private String cc;

		@JsonFormat(pattern = "yyyy-MM-dd")
		private LocalDate birthdate;

		private String position;
		private String height;
		private String shirtnumber;
	}
}
