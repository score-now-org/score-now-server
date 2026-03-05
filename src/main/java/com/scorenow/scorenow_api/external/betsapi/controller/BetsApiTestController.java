package com.scorenow.scorenow_api.external.betsapi.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.service.MatchSyncService;
import com.scorenow.scorenow_api.domain.player.service.PlayerSyncService;
import com.scorenow.scorenow_api.external.betsapi.BetsApiClient;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsEventResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@Profile({"dev", "test"})
@RestController
@RequestMapping("/api/test/betsapi")
@RequiredArgsConstructor
public class BetsApiTestController {

	private final BetsApiClient betsApiClient;
	private final MatchSyncService matchSyncService;

	private final PlayerSyncService playerSyncService;

	@GetMapping("/upcoming")
	public ApiResponse<BetsEventResponse> testUpcoming(
		@RequestParam(defaultValue = "1") String sportId,
		@RequestParam(required = false) String leagueId,
		@RequestParam(required = false) String day) {

		return ApiResponse.success(betsApiClient.getUpcomingEvents(sportId, leagueId, day));
	}

	@GetMapping("/inplay")
	public ApiResponse<BetsEventResponse> testInplay(
		@RequestParam(defaultValue = "1") String sportId,
		@RequestParam(required = false) String leagueId) {

		return ApiResponse.success(betsApiClient.getInplayEvents(sportId, leagueId));
	}

	@PostMapping("/sync/upcoming")
	public ApiResponse<String> syncUpcoming(
		@RequestParam(defaultValue = "1") String sportId,
		@RequestParam(required = false) String day
	) {
		int count = matchSyncService.syncUpcomingMatches(sportId, day);
		return ApiResponse.success(count + "건 동기화 완료");
	}

	@PostMapping("/sync/inplay")
	public ApiResponse<String> syncInplay(
		@RequestParam(defaultValue = "1") String sportId
	) {
		int count = matchSyncService.syncInplayMatches(sportId);
		return ApiResponse.success(count + "건 동기화 완료");
	}

	@PostMapping("/sync/ended")
	public ApiResponse<String> syncEnded(
		@RequestParam(defaultValue = "1") String sportId,
		@RequestParam(required = false) String day
	) {
		int count = matchSyncService.syncEndedMatches(sportId, day);
		return ApiResponse.success(count + "건 동기화 완료");
	}

	/**
	 * 선수 동기화
	 * curl -X POST "http://localhost:8080/api/test/betsapi/sync/players?leagueId=BETS194"
	 */
	@PostMapping("/sync/players")
	public ApiResponse<String> syncPlayer(@RequestParam String leagueId) {
		int count = playerSyncService.syncPlayersByLeague(leagueId);
		return ApiResponse.success(count + "건 동기화 완료");
	}
}
