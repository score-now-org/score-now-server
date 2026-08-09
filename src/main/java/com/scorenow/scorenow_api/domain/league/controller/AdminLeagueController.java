package com.scorenow.scorenow_api.domain.league.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueApiLeagueIdUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSyncEnabledUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.service.AdminLeagueService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/leagues")
public class AdminLeagueController implements AdminLeagueApiDocs {

	private final AdminLeagueService adminLeagueService;

	@GetMapping
	public ApiResponse<List<AdminLeagueResponse>> getLeagues(@ModelAttribute LeagueSearchCondition condition) {
		return ApiResponse.success(adminLeagueService.searchLeagues(condition));
	}

	@GetMapping("/{leagueId}")
	public ApiResponse<AdminLeagueResponse> getLeagues(@PathVariable Long leagueId) {
		return ApiResponse.success(adminLeagueService.searchLeagues(leagueId));
	}

	@PostMapping
	public ApiResponse<AdminLeagueResponse> createLeague(@RequestBody @Valid AdminLeagueCreateRequest request) {
		return ApiResponse.success(adminLeagueService.createLeague(request));
	}

	@PutMapping("/{leagueId}")
	public ApiResponse<Void> updateLeague(@PathVariable Long leagueId,
		@RequestBody @Valid AdminLeagueUpdateRequest request) {
		adminLeagueService.updateLeague(leagueId, request);
		return ApiResponse.success();
	}

	@PatchMapping("/{leagueId}/external-mapping/api-league-id")
	public ApiResponse<Void> updateApiLeagueId(
		@PathVariable Long leagueId,
		@RequestBody @Valid LeagueApiLeagueIdUpdateRequest request) {
		adminLeagueService.updateApiLeagueId(leagueId, request);
		return ApiResponse.success();
	}

	@PatchMapping("/{leagueId}/external-mapping/sync-enabled")
	public ApiResponse<Void> updateSyncEnabled(
		@PathVariable Long leagueId,
		@RequestBody @Valid LeagueSyncEnabledUpdateRequest request) {
		adminLeagueService.updateSyncEnabled(leagueId, request);
		return ApiResponse.success();
	}

	@DeleteMapping("/{leagueId}")
	public ApiResponse<Void> deleteLeague(@PathVariable Long leagueId) {
		adminLeagueService.deleteLeague(leagueId);
		return ApiResponse.success();
	}
}
