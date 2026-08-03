package com.scorenow.scorenow_api.domain.match.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupSideUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.match.model.LineupPlayer;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupCommandService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchLineupController {

	private final MatchLineupCommandService lineupCommandSvc;
	private final MatchLineupQueryService lineupQuerySvc;

	/**
	 * 라인업 팀 설정 수정
	 * - 포메이션, 유니폼 컬러 변경
	 */
	@PatchMapping("/{matchId}/lineup/teams/{teamId}")
	public ApiResponse<String> updateLineupSide(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@RequestBody MatchLineupSideUpdateRequest request
	) {
		return ApiResponse.success(
			lineupCommandSvc.updateLineupSide(matchId, teamId, request)
		);
	}

	/**
	 * 라인업 수정
	 * - 선수의 포지션, 등번호, 득점 수 변경 가능
	 */
	@PatchMapping("/{matchId}/lineup/players/{apiPlayerId}")
	public ApiResponse<String> updateLineupPlayer(
		@PathVariable Long matchId,
		@PathVariable String apiPlayerId,
		@RequestBody MatchLineupUpdateRequest request
	) {
		String updateId = lineupCommandSvc.updateLineupManual(matchId, apiPlayerId, request);
		return ApiResponse.success(updateId);
	}

	/**
	 * 라인업 조회
	 */
	@GetMapping("/{matchId}/lineup")
	public ApiResponse<MatchLineupDocument> getLineup(@PathVariable Long matchId) {
		return ApiResponse.success(lineupQuerySvc.getByMatchId(matchId));
	}

	/**
	 * 라인업 > 선수추가
	 * - 디플트로 팀 선수 리스트 조회
	 */
	@GetMapping("/{matchId}/lineup/teams/{teamId}/players")
	public ApiResponse<List<MatchLineupPlayerResponse>> getSelectablePlayers(
		@PathVariable Long matchId,
		@PathVariable Long teamId
	) {
		return ApiResponse.success(
			lineupQuerySvc.getSelectablePlayers(matchId, teamId)
		);
	}

	/**
	 * 라인업 > 선수추가
	 * - 선수명/팀명으로 검색 시 전체 선수 풀에서 조회
	 */
	@GetMapping("/{matchId}/lineup/players/search")
	public ApiResponse<List<MatchLineupPlayerResponse>> searchSelectablePlayers(
		@PathVariable Long matchId,
		@RequestParam String keyword
	) {
		return ApiResponse.success(
			lineupQuerySvc.searchSelectablePlayers(matchId, keyword)
		);
	}

	/**
	 * 라인업 > 선수추가
	 * - 선수 반영
	 */
	@PostMapping("/{matchId}/lineup/teams/{teamId}/players")
	public ApiResponse<String> addLineupPlayer(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@RequestBody LineupPlayer request
	) {
		return ApiResponse.success(
			lineupCommandSvc.addPlayerToLineup(matchId, teamId, request)
		);
	}

	/**
	 * 라인업 > 선수추가
	 * - 선수 해제
	 */
	@DeleteMapping("/{matchId}/lineup/players/{playerId}")
	public ApiResponse<String> deleteLineupPlayer(
		@PathVariable Long matchId,
		@PathVariable Long playerId
	) {
		return ApiResponse.success(
			lineupCommandSvc.removePlayerFromLineup(matchId, playerId)
		);
	}
}
