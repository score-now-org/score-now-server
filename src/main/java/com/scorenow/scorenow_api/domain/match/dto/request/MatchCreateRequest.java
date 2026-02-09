package com.scorenow.scorenow_api.domain.match.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchCreateRequest {

	@NotBlank(message = "경기 타입은 필수입니다.")
	private String matchType; // A(자동), M(수동)

	@NotBlank(message = "종목 ID는 필수입니다.")
	private String sportId;

	@NotBlank(message = "리그 ID는 필수입니다.")
	private String leagueId;

	@NotBlank(message = "홈팀 ID는 필수입니다.")
	private String homeId;

	@NotBlank(message = "원정팀 ID는 필수입니다.")
	private String awayId;

	@NotNull(message = "경기 시작 시간은 필수입니다.")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime startAt;
}
