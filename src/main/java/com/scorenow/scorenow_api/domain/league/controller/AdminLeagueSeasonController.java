package com.scorenow.scorenow_api.domain.league.controller;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonResponse;
import com.scorenow.scorenow_api.domain.league.service.AdminLeagueSeasonService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/league-seasons")
public class AdminLeagueSeasonController implements AdminLeagueSeasonApiDocs {

    private final AdminLeagueSeasonService adminLeagueSeasonService;

    @PostMapping
    public ApiResponse<Void> createLeagueSeason(@RequestBody @Valid LeagueSeasonCreateRequest request) {
        adminLeagueSeasonService.createLeagueSeason(request);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<Page<AdminLeagueSeasonResponse>> searchLeagueSeasons(
            @ModelAttribute LeagueSeasonSearchCondition condition,
            Pageable pageable) {

        return ApiResponse.success(
                adminLeagueSeasonService.searchLeagueSeasons(condition, pageable)
        );
    }

    @PatchMapping("/{leagueSeasonId}")
    public ApiResponse<Void> updateLeagueSeason(
            @PathVariable Long leagueSeasonId,
            @RequestBody @Valid LeagueSeasonUpdateRequest request) {

        adminLeagueSeasonService.updateLeagueSeason(leagueSeasonId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{leagueSeasonId}")
    public ApiResponse<Void> deleteLeagueSeason(@PathVariable Long leagueSeasonId) {
        adminLeagueSeasonService.deleteLeagueSeason(leagueSeasonId);
        return ApiResponse.success();
    }
}
