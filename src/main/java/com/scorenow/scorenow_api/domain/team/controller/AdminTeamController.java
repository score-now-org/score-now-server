package com.scorenow.scorenow_api.domain.team.controller;

import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamCreateRequest;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamSearchCondition;
import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamUpdateRequest;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamResponse;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.team.service.AdminTeamService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/teams")
@RequiredArgsConstructor
public class AdminTeamController implements AdminTeamApiDocs {

    private final AdminTeamService adminTeamService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AdminTeamResponse> createTeam(@Valid @RequestBody AdminTeamCreateRequest request) {
        return ApiResponse.success(adminTeamService.createTeam(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminTeamResponse> createTeamWithEmblem(
            @RequestPart("request") @Valid AdminTeamCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ApiResponse.success(adminTeamService.createTeam(request, image));
    }

    @GetMapping
    public ApiResponse<Page<AdminTeamResponse>> searchTeams(
            @ModelAttribute TeamSearchCondition condition,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ApiResponse.success(adminTeamService.searchTeams(condition, pageable));
    }

    @GetMapping("/search-options")
    public ApiResponse<AdminTeamSearchOptionsResponse> getSearchOptions() {
        return ApiResponse.success(adminTeamService.getSearchOptions());
    }

    @PatchMapping(value = "/{teamId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> updateTeam(@PathVariable Long teamId, @Valid @RequestBody AdminTeamUpdateRequest request) {
        adminTeamService.updateTeam(teamId, request);
        return ApiResponse.success();
    }

    @PatchMapping(value = "/{teamId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateTeamWithEmblem(
            @PathVariable Long teamId,
            @RequestPart("request") @Valid AdminTeamUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        adminTeamService.updateTeam(teamId, request, image);
        return ApiResponse.success();
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId) {
        adminTeamService.deleteTeam(teamId);
        return ApiResponse.success();
    }
}
