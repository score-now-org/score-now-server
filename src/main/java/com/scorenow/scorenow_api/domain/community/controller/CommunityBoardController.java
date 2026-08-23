package com.scorenow.scorenow_api.domain.community.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.community.dto.response.CommunityBoardResponse;
import com.scorenow.scorenow_api.domain.community.service.CommunityBoardService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/boards")
public class CommunityBoardController {

	private final CommunityBoardService communityBoardService;

	@GetMapping
	public ApiResponse<List<CommunityBoardResponse>> getBoards() {
		return ApiResponse.success(communityBoardService.getBoards());
	}
}
