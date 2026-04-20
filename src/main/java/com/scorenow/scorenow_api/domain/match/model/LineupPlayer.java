package com.scorenow.scorenow_api.domain.match.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LineupPlayer {

	private String rowId;
	private String playerId;
	private String eName;
	private String kName;

	private String shirtNumber;
	private String position;
	private Integer goals;

	private boolean substitute;  // startinglineup=false, substitute=true

	/**
	 * 기타 선수
	 * - DB에 없는 선수 라인업 등록
	 * - Player Entity와 분리된 Mongo document 모델
	 * - 경기 종료 시 cleanup 대상
	 */
	private boolean temp;
}
