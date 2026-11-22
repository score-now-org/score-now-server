package com.scorenow.scorenow_api.domain.match.controller;

import java.util.List;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerAddRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerRoleUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupSideUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Match Lineup API", description = "관리자 경기 라인업 관리 API")
public interface AdminMatchLineupApiDocs {

	@Operation(summary = "경기 라인업 전체 조회")
	ApiResponse<MatchLineupDocument> getLineup(
		@Parameter(description = "경기 ID", required = true) Long matchId
	);

	@Operation(summary = "라인업 팀 설정 수정", description = "해당 팀의 유니폼 색상을 수정")
	ApiResponse<String> updateLineupSide(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId,
		MatchLineupSideUpdateRequest request
	);

	@Operation(summary = "라인업 선수 정보 수정", description = "선수의 포지션과 등번호를 수정")
	ApiResponse<String> updateLineupPlayer(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId,
		@Parameter(description = "선수 ID", required = true) Long playerId,
		MatchLineupPlayerUpdateRequest request
	);

	@Operation(
		summary = "라인업 추가 가능 선수 목록 조회",
		description = "해당 팀에 소속된 선수 목록과 현재 라인업 등록 여부를 조회")
	ApiResponse<List<MatchLineupPlayerResponse>> getSelectablePlayers(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId
	);

	@Operation(
		summary = "라인업 추가 가능 선수 검색",
		description = "전체 선수 목록에서 이름으로 검색하고, 현재 라인업 등록 여부를 조회")
	ApiResponse<List<MatchLineupPlayerResponse>> searchSelectablePlayers(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId,
		@Parameter(description = "선수 검색어", required = true) String keyword
	);

	@Operation(summary = "라인업 선수 추가")
	ApiResponse<String> addLineupPlayer(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId,
		MatchLineupPlayerAddRequest request
	);

	@Operation(summary = "라인업 선수 삭제")
	ApiResponse<String> deleteLineupPlayer(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId,
		@Parameter(description = "선수 ID", required = true) Long playerId
	);

	@Operation(summary = "라인업 선수 역할 변경", description = "등록된 선수를 선발 또는 후보 명단으로 이동합니다.")
	ApiResponse<String> updateLineupPlayerRole(
		@Parameter(description = "경기 ID", required = true) Long matchId,
		@Parameter(description = "팀 ID", required = true) Long teamId,
		@Parameter(description = "선수 ID", required = true) Long playerId,
		MatchLineupPlayerRoleUpdateRequest request
	);

}
