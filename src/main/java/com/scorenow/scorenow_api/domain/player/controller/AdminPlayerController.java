package com.scorenow.scorenow_api.domain.player.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerCreateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerSquadUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerResponse;
import com.scorenow.scorenow_api.domain.player.service.AdminPlayerService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/players")
@RequiredArgsConstructor
public class AdminPlayerController {

	private final AdminPlayerService adminPlayerService;

	// 선수 팀 소속 정보 삭제
	@DeleteMapping("/team-details/{playerTeamDetailId}")
	public ApiResponse<Void> deletePlayerTeamDetail(
		@PathVariable Long playerTeamDetailId
	) {
		adminPlayerService.deletePlayerTeamDetail(playerTeamDetailId);
		return ApiResponse.success();
	}

	@GetMapping("/providers")
	public ApiResponse<List<String>> getProviders() {
		List<String> providers = Arrays.stream(DataOrigin.values())
			.map(Enum::name)
			.toList();

		return ApiResponse.success(providers);
	}

	// 선수 수동 등록
	@PostMapping
	public ApiResponse<Long> createPlayer(
		@RequestBody @Valid AdminPlayerCreateRequest request
	) {
		Long playerId = adminPlayerService.createPlayer(request);
		return ApiResponse.success(playerId);
	}

	// 선수 스쿼드 on/off
	@PatchMapping("/team-details/{playerTeamDetailId}/squad")
	public ApiResponse<Void> updateSquadOn(
		@PathVariable Long playerTeamDetailId,
		@RequestBody @Valid AdminPlayerSquadUpdateRequest request
	) {
		adminPlayerService.updateSquadOn(playerTeamDetailId, request);
		return ApiResponse.success();
	}

	@PatchMapping("/{playerId}")
	public ApiResponse<Void> updatePlayer(
		@PathVariable Long playerId,
		@RequestBody AdminPlayerUpdateRequest request
	) {
		adminPlayerService.updatePlayer(playerId, request);
		return ApiResponse.success();
	}

	@GetMapping
	public ApiResponse<Page<AdminPlayerResponse>> getPlayers(
		@RequestParam(required = false) Long sportId,
		@RequestParam(required = false) Long leagueId,
		@RequestParam(required = false) String playerKeyword,
		@RequestParam(required = false) String teamKeyword,
		@PageableDefault(size = 20) Pageable pageable
	) {
		return ApiResponse.success(
			adminPlayerService.searchPlayers(
				sportId,
				leagueId,
				playerKeyword,
				teamKeyword,
				pageable
			)
		);
	}

}
