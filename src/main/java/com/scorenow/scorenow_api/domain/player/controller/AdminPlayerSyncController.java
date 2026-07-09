package com.scorenow.scorenow_api.domain.player.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.player.service.AdminPlayerSyncService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/player-sync-jobs")
@RequiredArgsConstructor
public class AdminPlayerSyncController {

	private final AdminPlayerSyncService adminPlayerSyncService;

	@PostMapping
	public ApiResponse<String> runPlayerSyncJob() {
		adminPlayerSyncService.runPlayerSyncJob();

		return ApiResponse.success("선수 동기화 배치 실행 요청 완료");
	}

}
