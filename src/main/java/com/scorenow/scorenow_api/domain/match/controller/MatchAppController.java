package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.statistics.MatchStatisticsResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchAppQueryService;
import com.scorenow.scorenow_api.domain.match.service.MatchStatisticsAppQueryService;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/app/matches")
@RequiredArgsConstructor
public class MatchAppController implements MatchAppApiDocs {

    private final MatchAppQueryService matchAppQueryService;
    private final MatchStatisticsAppQueryService matchStatisticsAppQueryService;

    /**
     * 앱 경기 목록 조회
     */
    @GetMapping
    public ApiResponse<MatchAppResponse> getMatches(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(required = false) SportCode sportCode,
            @RequestParam(required = false) Long leagueId) {

        return ApiResponse.success(matchAppQueryService.getMatchesWithFeatured(date, sportCode, leagueId));
    }

    /**
     * 앱 경기 상세 조회
     */
    @Override
    @GetMapping("/{matchId}")
    public ApiResponse<MatchAppDetailResponse> getMatch(@PathVariable Long matchId) {
        return ApiResponse.success(matchAppQueryService.getMatch(matchId));
    }

    /**
     * 앱 경기 통계 조회
     */
    @GetMapping("/{matchId}/statistics")
    public ApiResponse<MatchStatisticsResponse> getMatchStatistics(@PathVariable Long matchId) {
        return ApiResponse.success(matchStatisticsAppQueryService.getStatistics(matchId));
    }

}
