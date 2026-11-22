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
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerAddRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerRoleUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchLineupSideUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchLineupPlayerResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupCommandService;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/matches/{matchId}/lineup")
public class AdminMatchLineupController implements AdminMatchLineupApiDocs {

	private final MatchLineupCommandService lineupCommandSvc;
	private final MatchLineupQueryService lineupQuerySvc;

	@Override
	@GetMapping
	public ApiResponse<MatchLineupDocument> getLineup(@PathVariable Long matchId) {
		return ApiResponse.success(lineupQuerySvc.getByMatchId(matchId));
	}

	@Override
	@PatchMapping("/teams/{teamId}")
	public ApiResponse<String> updateLineupSide(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@RequestBody MatchLineupSideUpdateRequest request
	) {
		return ApiResponse.success(
			lineupCommandSvc.updateLineupSide(
				matchId,
				teamId,
				request)
		);
	}

	@Override
	@PatchMapping("/teams/{teamId}/players/{playerId}")
	public ApiResponse<String> updateLineupPlayer(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@PathVariable Long playerId,
		@RequestBody MatchLineupPlayerUpdateRequest request
	) {
		return ApiResponse.success(
			lineupCommandSvc.updateLineupManual(
				matchId,
				teamId,
				playerId,
				request
			)
		);
	}

	@Override
	@GetMapping("/teams/{teamId}/players")
	public ApiResponse<List<MatchLineupPlayerResponse>> getSelectablePlayers(
		@PathVariable Long matchId,
		@PathVariable Long teamId
	) {
		return ApiResponse.success(
			lineupQuerySvc.getSelectablePlayers(matchId, teamId)
		);
	}

	@Override
	@GetMapping("/teams/{teamId}/players/search")
	public ApiResponse<List<MatchLineupPlayerResponse>> searchSelectablePlayers(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@RequestParam String keyword
	) {
		return ApiResponse.success(
			lineupQuerySvc.searchSelectablePlayers(matchId, teamId, keyword)
		);
	}

	@Override
	@PostMapping("/teams/{teamId}/players")
	public ApiResponse<String> addLineupPlayer(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@Valid @RequestBody MatchLineupPlayerAddRequest request
	) {
		return ApiResponse.success(
			lineupCommandSvc.addPlayerToLineup(matchId, teamId, request)
		);
	}

	@Override
	@DeleteMapping("/teams/{teamId}/players/{playerId}")
	public ApiResponse<String> deleteLineupPlayer(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@PathVariable Long playerId
	) {
		return ApiResponse.success(
			lineupCommandSvc.removePlayerFromLineup(
				matchId,
				teamId,
				playerId
			)
		);
	}

	@Override
	@PatchMapping("/teams/{teamId}/players/{playerId}/role")
	public ApiResponse<String> updateLineupPlayerRole(
		@PathVariable Long matchId,
		@PathVariable Long teamId,
		@PathVariable Long playerId,
		@Valid @RequestBody MatchLineupPlayerRoleUpdateRequest request
	) {
		return ApiResponse.success(
			lineupCommandSvc.updateLineupPlayerRole(
				matchId,
				teamId,
				playerId,
				request
			)
		);
	}
}
