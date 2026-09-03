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
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Match Detail API", description = "관리자 경기 상세 관리 API")
public interface AdminMatchDetailApiDocs {

    @Operation(summary = "축구 경기 상세 조회")
    ApiResponse<FootballAdminMatchDetailResponse> getMatchDetail(
            @Parameter(description = "경기 ID", required = true) Long matchId);

    @Operation(summary = "경기 예정 시각 수정")
    ApiResponse<Void> updateScheduledStartAt(Long matchId, MatchScheduledStartAtUpdateRequest request);

    @Operation(summary = "경기 상태 수정")
    ApiResponse<Void> updateStatus(Long matchId, MatchStatusUpdateRequest request);

    @Operation(summary = "일반 점수 수정")
    ApiResponse<Void> updateScore(Long matchId, MatchScoreUpdateRequest request);

    @Operation(summary = "축구 추가시간 전체 수정")
    ApiResponse<Void> updateAdditionalTime(Long matchId, FootballAdditionalTimeUpdateRequest request);

    @Operation(summary = "축구 경기 스탯 전체 수정")
    ApiResponse<Void> updateFootballStats(Long matchId, FootballStatsUpdateRequest request);

    @Operation(summary = "축구 승부차기 점수 수정")
    ApiResponse<Void> updateShootOutScore(Long matchId, FootballShootOutScoreUpdateRequest request);

    @Operation(summary = "축구 Phase 전환")
    ApiResponse<Void> transitionPhase(Long matchId, FootballPhaseTransitionRequest request);

    @Operation(summary = "축구 경과시간 보정")
    ApiResponse<Void> correctClock(Long matchId, FootballClockCorrectionRequest request);

    @Operation(summary = "축구 경기 타이머 일시정지")
    ApiResponse<Void> pauseClock(Long matchId);

    @Operation(summary = "축구 경기 타이머 재개")
    ApiResponse<Void> resumeClock(Long matchId);
}
