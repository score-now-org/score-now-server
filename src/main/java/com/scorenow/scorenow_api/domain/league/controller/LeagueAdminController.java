package com.scorenow.scorenow_api.domain.league.controller;

import java.util.List;

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
public class LeagueAdminController {

    private final LeagueAdminService leagueAdminService;

    @GetMapping
    public ApiResponse<List<LeagueAdminResponse>> getLeagues(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(leagueAdminService.getLeagues(keyword));
    }

    @PostMapping
    public ApiResponse<LeagueAdminResponse> createLeague(@RequestBody @Valid LeagueCreateRequest request) {
        return ApiResponse.success(leagueAdminService.createLeague(request));
    }

    @PutMapping("/{leagueId}")
    public ApiResponse<Void> updateLeague(@PathVariable Long leagueId, @RequestBody @Valid LeagueUpdateRequest request) {
        leagueAdminService.updateLeague(leagueId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{leagueId}")
    public ApiResponse<Void> deleteLeague(@PathVariable Long leagueId) {
        leagueAdminService.deleteLeague(leagueId);
        return ApiResponse.success();
    }
}
