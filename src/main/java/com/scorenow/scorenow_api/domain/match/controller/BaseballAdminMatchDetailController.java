package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.BaseballBaseStatesUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballBatterUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballCountUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballDetailedBroadcastUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballHalfInningUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballInningScoresUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballPitcherUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.BaseballStatsUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchScheduledStartAtUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchStatusUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.BaseballAdminMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.service.BaseballAdminMatchDetailService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/matches/{matchId}/detail/baseball")
public class BaseballAdminMatchDetailController {
    private final BaseballAdminMatchDetailService service;

    @GetMapping
    public ApiResponse<BaseballAdminMatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        return ApiResponse.success(service.getBaseballMatchDetail(matchId));
    }

    @PutMapping("/scheduled-start-at")
    public ApiResponse<Void> updateScheduledStartAt(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchScheduledStartAtUpdateRequest request) {
        service.updateMatchScheduledStartAt(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchStatusUpdateRequest request) {
        service.updateMatchStatus(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/half-inning")
    public ApiResponse<Void> updateCurrentHalfInning(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballHalfInningUpdateRequest request) {
        service.updateCurrentHalfInning(matchId, request.getHalfInning());
        return ApiResponse.success();
    }

    @PutMapping("/inning-scores")
    public ApiResponse<Void> updateInningScores(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballInningScoresUpdateRequest request) {
        service.updateInningScores(matchId, request.toBaseballInningScores());
        return ApiResponse.success();
    }

    @PutMapping("/statistics")
    public ApiResponse<Void> updateStats(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballStatsUpdateRequest request) {
        service.updateBaseballStats(
                matchId,
                request.getHomeStats().toBaseballStats(),
                request.getAwayStats().toBaseballStats());
        return ApiResponse.success();
    }

    @PutMapping("/count")
    public ApiResponse<Void> updateCount(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballCountUpdateRequest request) {
        service.updateBaseballCount(matchId, request.toBaseballCount());
        return ApiResponse.success();
    }

    @PutMapping("/base-states")
    public ApiResponse<Void> updateBaseStates(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballBaseStatesUpdateRequest request) {
        service.updateBaseStates(matchId, request.toBaseballBaseStates());
        return ApiResponse.success();
    }

    @PutMapping("/pitcher")
    public ApiResponse<Void> updatePitcher(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballPitcherUpdateRequest request) {
        service.updatePitcher(matchId, request.getPitcher());
        return ApiResponse.success();
    }

    @PutMapping("/batter")
    public ApiResponse<Void> updateBatter(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballBatterUpdateRequest request) {
        service.updateBatter(matchId, request.getBatter());
        return ApiResponse.success();
    }

    @PutMapping("/detailed-broadcast")
    public ApiResponse<Void> updateDetailedBroadcast(
            @PathVariable Long matchId,
            @Valid @RequestBody BaseballDetailedBroadcastUpdateRequest request) {
        service.updateDetailedBroadcastEnabled(matchId, request.getEnabled());
        return ApiResponse.success();
    }
}
