package com.scorenow.scorenow_api.domain.league.controller;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsGroupMappingsUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsTypeUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsGroupMappingsResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsResponse;
import com.scorenow.scorenow_api.domain.league.service.AdminLeagueSeasonStandingsService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/league-season-standings")
public class AdminLeagueSeasonStandingsController implements AdminLeagueSeasonStandingsApiDocs {

    private final AdminLeagueSeasonStandingsService adminLeagueSeasonStandingsService;

    @PostMapping
    public ApiResponse<Void> createLeagueSeasonStandings(@RequestBody @Valid LeagueSeasonStandingsCreateRequest request) {
        adminLeagueSeasonStandingsService.createLeagueSeasonStandings(request);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<Page<AdminLeagueSeasonStandingsResponse>> searchLeagueSeasonStandings(
            @ModelAttribute LeagueSeasonStandingsSearchCondition condition,
            Pageable pageable) {

        return ApiResponse.success(
                adminLeagueSeasonStandingsService.searchLeagueSeasonStandings(condition, pageable)
        );
    }

    @PatchMapping("/{leagueSeasonId}/type")
    public ApiResponse<Void> updateLeagueSeasonStandingsType(
            @PathVariable Long leagueSeasonId,
            @RequestBody @Valid LeagueSeasonStandingsTypeUpdateRequest request) {

        adminLeagueSeasonStandingsService.updateLeagueSeasonStandingsType(leagueSeasonId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{leagueSeasonId}")
    public ApiResponse<Void> deleteLeagueSeasonStandings(@PathVariable Long leagueSeasonId) {
        adminLeagueSeasonStandingsService.deleteLeagueSeasonStandings(leagueSeasonId);
        return ApiResponse.success();
    }

    @PostMapping("/{leagueSeasonId}/image")
    public ApiResponse<Void> uploadLeagueSeasonStandingsImage(
            @PathVariable Long leagueSeasonId,
            @RequestPart("image") MultipartFile image) {

        // TODO: 리그 순위 이미지 업로드는 일단 막아두고, 추후 이미지 서버 구축 및 관련 기능 개발할 때 처리하기.
//        adminLeagueSeasonStandingsService.uploadLeagueSeasonStandingsImage(leagueSeasonId, image);
        return ApiResponse.success();
    }

    @PostMapping("/{leagueSeasonId}/sync")
    public ApiResponse<Void> syncLeagueSeasonStandingsData(@PathVariable Long leagueSeasonId) {
        adminLeagueSeasonStandingsService.syncLeagueSeasonStandingsData(leagueSeasonId);
        return ApiResponse.success();
    }

    @GetMapping("/{leagueSeasonId}/group-mappings")
    public ApiResponse<AdminLeagueSeasonStandingsGroupMappingsResponse> getGroupMappings(
            @PathVariable Long leagueSeasonId) {

        return ApiResponse.success(adminLeagueSeasonStandingsService.getGroupMappings(leagueSeasonId));
    }

    @PutMapping("/{leagueSeasonId}/group-mappings")
    public ApiResponse<Void> updateGroupMappings(
            @PathVariable Long leagueSeasonId,
            @RequestBody @Valid LeagueSeasonStandingsGroupMappingsUpdateRequest request) {

        adminLeagueSeasonStandingsService.updateGroupMappings(leagueSeasonId, request);
        return ApiResponse.success();
    }
}
