package com.scorenow.scorenow_api.external.betsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BetsPager {
	private int page;

	@JsonProperty("per_page")
	private int perPage;

	private int total;

	@JsonProperty("min_id")
	private String minId;
}
