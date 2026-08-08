package com.scorenow.scorenow_api.domain.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 경기 검색 조건")
public class MatchSearchCondition {
	@Schema(description = "종목 ID", example = "1")
	private Long sportId;

	@Schema(description = "리그 ID", example = "2")
	private Long leagueId;

	@Schema(description = "리그명 검색어", example = "프리미어")
	private String leagueName;

	@Schema(
			description = "경기 상태",
			example = "IN_PLAY",
			allowableValues = {
					"NOT_STARTED", "IN_PLAY", "TO_BE_FIXED", "ENDED", "POSTPONED",
					"CANCELLED", "WALKOVER", "INTERRUPTED", "ABANDONED", "RETIRED", "REMOVED"
			}
	)
	private String status;

	@Schema(description = "앱 노출 여부. null이면 전체, true이면 사용중, false이면 미사용중", example = "true")
	private Boolean isActive;

	@Schema(description = "자동/수동 경기 여부. null이면 전체, true이면 수동, false이면 자동", example = "true")
	private Boolean isManual;

	@Schema(description = "경기 날짜. yyyyMMdd", example = "20260605")
	private String date;

	@Schema(description = "정렬 기준. null이면 기본값 date", example = "date", allowableValues = {"sport", "league", "status", "date"})
	private String sortBy;

	@Schema(description = "정렬 방향. null이면 기본값 ASC", example = "ASC", allowableValues = {"ASC", "DESC"})
	private String sortDirection;
}
