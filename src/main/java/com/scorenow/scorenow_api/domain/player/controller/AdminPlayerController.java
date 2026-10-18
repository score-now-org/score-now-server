package com.scorenow.scorenow_api.domain.player.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerResponse;
import com.scorenow.scorenow_api.domain.player.service.AdminPlayerService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/players")
@RequiredArgsConstructor
public class AdminPlayerController {

	private final AdminPlayerService adminPlayerService;

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
