package com.scorenow.scorenow_api.domain.match.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupUpdateRequest;
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

	/** 라인업 수동 업데이트 */
	@PatchMapping("/{matchId}/lineup/players/{playerId}")
	public ApiResponse<String> updateLineupPlayer(
		@PathVariable String matchId,
		@PathVariable String playerId,
		@RequestBody MatchLineupUpdateRequest request
	) {
		String updateId = lineupCommandSvc.updateLineupManual(matchId, playerId, request);
		return ApiResponse.success(updateId);
	}

	/** 라인업 조회 */
	@GetMapping("/{matchId}/lineup")
	public ApiResponse<MatchLineupDocument> getLineup(@PathVariable String matchId) {
		return ApiResponse.success(lineupQuerySvc.getByMatchId(matchId));
	}
}
