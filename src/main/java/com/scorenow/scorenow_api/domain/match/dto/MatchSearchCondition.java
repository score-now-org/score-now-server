package com.scorenow.scorenow_api.domain.match.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchSearchCondition {
	private Long sportId;
	private Long leagueId;
	private String leagueName; // 리그명 검색
	private String status; // NOT_STARTED, INPLAY, ENDED 등
	private Boolean isActive; // null = 전체, true = 사용중, false = 미사용
	private String date;  // yyyyMMdd
	private String sortBy; // sport, league, status, date (null이면 기본값: date)
	private String sortDirection; // ASC, DESC (null이면 기본값: ASC)
}
