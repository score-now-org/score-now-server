package com.scorenow.scorenow_api.domain.league.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LeagueCreateRequest {

	@NotBlank(message = "종목ID 는 필수입니다.")
	private Long sportId;

	@NotBlank
	private String eName;

	private String kName;

	private String sName;
}
