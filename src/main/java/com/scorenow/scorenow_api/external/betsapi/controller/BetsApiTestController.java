package com.scorenow.scorenow_api.external.betsapi.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

	@GetMapping("/upcoming")
	public ApiResponse<BetsEventResponse> testUpcoming(
		@RequestParam(defaultValue = "1") String sportId,
		@RequestParam(required = false) String day,
		@RequestParam(defaultValue = "1") int page) {

		return ApiResponse.success(betsApiClient.getUpcomingEvents(sportId, day, page));
	}

	@GetMapping("/inplay")
	public ApiResponse<BetsEventResponse> testInplay(
		@RequestParam(defaultValue = "1") String sportId) {

		return ApiResponse.success(betsApiClient.getInplayEvents(sportId));
	}
}
