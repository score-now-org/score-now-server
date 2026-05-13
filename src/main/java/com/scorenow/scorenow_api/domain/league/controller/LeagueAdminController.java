package com.scorenow.scorenow_api.domain.league.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.LeagueAdminResponse;
import com.scorenow.scorenow_api.domain.league.service.LeagueAdminService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/leagues")
@Tag(name = "Admin - League ", description = "관리자 리그 관리 API")
public class LeagueAdminController {

    private final LeagueAdminService leagueAdminService;

    @GetMapping
    @Operation(summary = "리그 목록 조회", description = "keyword로 리그 이름 검색 가능 (없으면 전체 조회)")
    public ApiResponse<List<LeagueAdminResponse>> getLeagues(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(leagueAdminService.getLeagues(keyword));
    }

    @PostMapping
    @Operation(summary = "리그 생성", description = "새로운 리그를 생성합니다.")
    public ApiResponse<LeagueAdminResponse> createLeague(@RequestBody @Valid LeagueCreateRequest request) {
        return ApiResponse.success(leagueAdminService.createLeague(request));
    }

    @PutMapping("/{leagueId}")
    @Operation(summary = "리그 수정", description = "리그 ID로 리그 정보를 수정합니다.")
    public ApiResponse<Void> updateLeague(@PathVariable Long leagueId, @RequestBody @Valid LeagueUpdateRequest request) {
        leagueAdminService.updateLeague(leagueId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{leagueId}")
    @Operation(summary = "리그 삭제", description = "리그 ID로 리그를 삭제합니다.")
    public ApiResponse<Void> deleteLeague(@PathVariable Long leagueId) {
        leagueAdminService.deleteLeague(leagueId);
        return ApiResponse.success();
    }
}
