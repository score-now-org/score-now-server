package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.FootballMatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/matches/{matchId}/detail")
public class AdminMatchDetailController {

    private final MatchDetailService matchDetailService;

    @GetMapping
    public ApiResponse<MatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        return ApiResponse.success(matchDetailService.getMatchDetail(matchId));
    }

    @PatchMapping
    public ApiResponse<Void> updateMatchDetail(
            @PathVariable Long matchId,
            @RequestBody FootballMatchDetailUpdateRequest request) {

        matchDetailService.updateMatchDetailManual(matchId, request);
        return ApiResponse.success();
    }
}