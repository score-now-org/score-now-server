package com.scorenow.scorenow_api.domain.league.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueApiLeagueIdUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSyncEnabledUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.service.AdminLeagueService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/leagues")
@Tag(name = "Admin - League ", description = "관리자 리그 관리 API")
public class AdminLeagueController {

    private final AdminLeagueService adminLeagueService;

    @GetMapping
    @Operation(summary = "리그 목록 조회", description = "keyword 로 리그 이름 검색 (없으면 전체 조회)")
    public ApiResponse<List<AdminLeagueResponse>> getLeagues(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminLeagueService.searchLeagues(keyword));
    }

    @GetMapping("/{leagueId}")
    @Operation(summary = "리그 조회", description = "leagueId 로 리그 검색 (없으면 전체 조회)")
    public ApiResponse<AdminLeagueResponse> getLeagues(@PathVariable Long leagueId) {
        return ApiResponse.success(adminLeagueService.searchLeagues(leagueId));
    }

    @PostMapping
    @Operation(summary = "리그 생성", description = "새로운 리그를 생성합니다.")
    public ApiResponse<AdminLeagueResponse> createLeague(@RequestBody @Valid AdminLeagueCreateRequest request) {
        return ApiResponse.success(adminLeagueService.createLeague(request));
    }

    @PutMapping("/{leagueId}")
    @Operation(summary = "리그 수정", description = "리그 ID로 리그 정보를 수정합니다.")
    public ApiResponse<Void> updateLeague(@PathVariable Long leagueId, @RequestBody @Valid AdminLeagueUpdateRequest request) {
        adminLeagueService.updateLeague(leagueId, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{leagueId}/external-mapping/api-league-id")
    @Operation(summary = "외부 API 리그 ID 수정", description = "리그 ID로 외부 API 리그 ID를 수정합니다.")
    public ApiResponse<Void> updateApiLeagueId(
            @PathVariable Long leagueId,
            @RequestBody @Valid LeagueApiLeagueIdUpdateRequest request) {
        adminLeagueService.updateApiLeagueId(leagueId, request);
        return ApiResponse.success();
    }

    @PatchMapping("/{leagueId}/external-mapping/sync-enabled")
    @Operation(summary = "외부 API 동기화 여부 수정", description = "리그 ID로 외부 API 경기 동기화 활성화 여부를 수정합니다.")
    public ApiResponse<Void> updateSyncEnabled(
            @PathVariable Long leagueId,
            @RequestBody @Valid LeagueSyncEnabledUpdateRequest request) {
        adminLeagueService.updateSyncEnabled(leagueId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{leagueId}")
    @Operation(summary = "리그 삭제", description = "리그 ID로 리그를 삭제합니다.")
    public ApiResponse<Void> deleteLeague(@PathVariable Long leagueId) {
        adminLeagueService.deleteLeague(leagueId);
        return ApiResponse.success();
    }
}
