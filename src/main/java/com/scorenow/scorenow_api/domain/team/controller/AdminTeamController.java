package com.scorenow.scorenow_api.domain.team.controller;

import com.scorenow.scorenow_api.domain.team.dto.request.TeamCreateRequest;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamSearchCondition;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamUpdateRequest;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamResponse;
import com.scorenow.scorenow_api.domain.team.service.AdminTeamService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/teams")
@RequiredArgsConstructor
public class AdminTeamController {

    private final AdminTeamService adminTeamService;

    @PostMapping
    public ApiResponse<AdminTeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        return ApiResponse.success(adminTeamService.createTeam(request));
    }

    @GetMapping
    public ApiResponse<Page<AdminTeamResponse>> searchTeams(
            @ModelAttribute TeamSearchCondition condition,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(adminTeamService.searchTeams(condition, pageable));
    }

    @PatchMapping("/{teamId}")
    public ApiResponse<Void> updateTeam(@PathVariable Long teamId, @Valid @RequestBody TeamUpdateRequest request) {
        adminTeamService.updateTeam(teamId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId) {
        adminTeamService.deleteTeam(teamId);
        return ApiResponse.success();
    }
}
