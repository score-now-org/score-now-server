package com.scorenow.scorenow_api.domain.match.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchAdminService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/matches")
@RequiredArgsConstructor
public class MatchAdminController {

	private final MatchAdminService matchAdminService;

	/**
	 * 경기 리스트 조회
	 */
	@GetMapping
	public ApiResponse<Page<MatchListResponse>> getMatches(
		@ModelAttribute MatchSearchCondition condition,
		@PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.ASC)Pageable pageable) {

		Page<MatchListResponse> result = matchAdminService.getMatches(condition, pageable);
		return ApiResponse.success(result);
	}

	/**
	 * 경기 상세 조회
	 */
	@GetMapping("/{matchId}")
	public ApiResponse<MatchListResponse> getMatch(@PathVariable String matchId){
		MatchListResponse result = matchAdminService.getMatch(matchId);
		return ApiResponse.success(result);
	}

	/**
	 * 경기 수동 등록
	 */
	@PostMapping
	public ApiResponse<MatchListResponse> createMatch(@Valid @RequestBody MatchCreateRequest request){
		MatchListResponse result = matchAdminService.createMatch(request);
		return ApiResponse.success(result);
	}

	/**
	 * 경기 수정
	 */
	@PatchMapping("/{matchId}")
	public ApiResponse<Void> updateMatch(
		@PathVariable String matchId,
		@Valid @RequestBody MatchUpdateRequest request){
		matchAdminService.updateMatch(matchId, request);
		return ApiResponse.success(null);
	}

	/**
	 * 경기 삭제
	 */
	@DeleteMapping("/{matchId}")
	public ApiResponse<Void> deleteMatch(@PathVariable String matchId){
		matchAdminService.deleteMatch(matchId);
		return ApiResponse.success(null);
	}
}
