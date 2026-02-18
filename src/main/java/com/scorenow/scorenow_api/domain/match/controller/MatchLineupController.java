package com.scorenow.scorenow_api.domain.match.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

	/** 라인업 조회 */
	@GetMapping("/{matchId}")
	public ApiResponse<MatchLineupDocument> getLineup(@PathVariable String matchId) {
		return ApiResponse.success(matchLineupSvc.getByMatchId(matchId));
	}
}
