package com.scorenow.scorenow_api.domain.player.controller;

import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerAvailabilityUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.PlayerAvailabilitySearchCondition;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerAvailabilityResponse;
import com.scorenow.scorenow_api.domain.player.service.AdminPlayerAvailabilityService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/player-availabilities")
@RequiredArgsConstructor
public class AdminPlayerAvailabilityController implements AdminPlayerAvailabilityApiDocs {

    private final AdminPlayerAvailabilityService adminPlayerAvailabilityService;

    @GetMapping
    public ApiResponse<Page<AdminPlayerAvailabilityResponse>> searchPlayers(
            @ModelAttribute PlayerAvailabilitySearchCondition condition,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(adminPlayerAvailabilityService.searchPlayers(condition, pageable));
    }

    @PatchMapping("/{playerTeamDetailId}")
    public ApiResponse<Void> updatePlayerAvailabilityStatus(
            @PathVariable Long playerTeamDetailId,
            @Valid @RequestBody AdminPlayerAvailabilityUpdateRequest request) {
        adminPlayerAvailabilityService.updatePlayerAvailabilityStatus(playerTeamDetailId, request.getAvailabilityStatus());
        return ApiResponse.success();
    }
}
