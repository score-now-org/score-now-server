package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.FootballAdditionalTimeUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballClockCorrectionRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballPhaseTransitionRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballShootOutScoreUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.FootballStatsUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchScheduledStartAtUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchScoreUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchStatusUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.FootballAdminMatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.service.FootballAdminMatchDetailService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/matches/{matchId}/detail")
public class AdminMatchDetailController implements AdminMatchDetailApiDocs {

    private final FootballAdminMatchDetailService footballAdminMatchDetailService;

    @GetMapping
    public ApiResponse<FootballAdminMatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        return ApiResponse.success(footballAdminMatchDetailService.getFootballMatchDetail(matchId));
    }

    @PutMapping("/scheduled-start-at")
    public ApiResponse<Void> updateScheduledStartAt(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchScheduledStartAtUpdateRequest request) {

        footballAdminMatchDetailService.updateMatchScheduledStartAt(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchStatusUpdateRequest request) {

        footballAdminMatchDetailService.updateMatchStatus(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/score")
    public ApiResponse<Void> updateScore(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchScoreUpdateRequest request) {

        footballAdminMatchDetailService.updateMatchScore(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/football/additional-time")
    public ApiResponse<Void> updateAdditionalTime(
            @PathVariable Long matchId,
            @Valid @RequestBody FootballAdditionalTimeUpdateRequest request) {

        footballAdminMatchDetailService.updateAdditionalTime(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/football/statistics")
    public ApiResponse<Void> updateFootballStats(
            @PathVariable Long matchId,
            @Valid @RequestBody FootballStatsUpdateRequest request) {

        footballAdminMatchDetailService.updateFootballStats(matchId, request);
        return ApiResponse.success();
    }

    @PutMapping("/football/shoot-out-score")
    public ApiResponse<Void> updateShootOutScore(
            @PathVariable Long matchId,
            @Valid @RequestBody FootballShootOutScoreUpdateRequest request) {

        footballAdminMatchDetailService.updateShootOutScore(matchId, request);
        return ApiResponse.success();
    }

    @PostMapping("/football/clock/phase-transitions")
    public ApiResponse<Void> transitionPhase(
            @PathVariable Long matchId,
            @Valid @RequestBody FootballPhaseTransitionRequest request) {

        footballAdminMatchDetailService.updatePhase(matchId, request);
        return ApiResponse.success();
    }

    @PostMapping("/football/clock/corrections")
    public ApiResponse<Void> correctClock(
            @PathVariable Long matchId,
            @Valid @RequestBody FootballClockCorrectionRequest request) {

        footballAdminMatchDetailService.correctClock(matchId, request);
        return ApiResponse.success();
    }

    @PostMapping("/football/clock/pause")
    public ApiResponse<Void> pauseClock(@PathVariable Long matchId) {
        footballAdminMatchDetailService.pauseClock(matchId);
        return ApiResponse.success();
    }

    @PostMapping("/football/clock/resume")
    public ApiResponse<Void> resumeClock(@PathVariable Long matchId) {

        footballAdminMatchDetailService.resumeClock(matchId);
        return ApiResponse.success();
    }
}
