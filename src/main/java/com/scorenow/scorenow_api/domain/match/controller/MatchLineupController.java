package com.scorenow.scorenow_api.domain.match.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.document.MatchLineupDocument;
import com.scorenow.scorenow_api.domain.match.service.MatchLineupService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lineup")
public class MatchLineupController {

	private final MatchLineupService matchLineupSvc;

	/**
	 * eventId를 직접 넣어서 라인업 호출 + Mongo 저장
	 * POST /api/lineup/save?eventId=10119802
	 */
	@PostMapping("/save")
	public ApiResponse<MatchLineupDocument> saveLineup(@RequestParam String eventId) {
		return ApiResponse.success(matchLineupSvc.fetchAndSaveByMatchId(eventId));
	}

	/**
	 * 저장된 라인업 조회 (Mongo)
	 * GET /api/lineup/{matchId}
	 */
	@GetMapping("/{matchId}")
	public ApiResponse<MatchLineupDocument> getLineup(@PathVariable String matchId) {
		return ApiResponse.success(matchLineupSvc.getByMatchId(matchId));
	}
}
